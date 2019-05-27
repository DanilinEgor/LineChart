package com.kuxurum.smoothlinechart;

import android.animation.Animator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.util.LongSparseArray;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class PercentageBarLineView extends BaseLineView {
    private static int DATE_MARGIN = 20;
    private static int ANIMATION_DURATION = 200;
    private static int STATE_ANIMATION_DURATION = 500;
    private static int DATE_ANIMATION_DURATION = 150;
    private static int MAX_ALPHA = 128;

    private Data data;

    private int fromIndex;
    private int toIndex;

    private long minX, maxX;
    private long maxY, prevMaxY;
    private long fromX, toX;
    private long step0, step0Time;
    private float step0k, step0b;
    private boolean step0Down;
    private float sw = 0f;

    private LongSparseArray<Long> yToTime = new LongSparseArray<>();
    private LongSparseArray<Long> dateToTime = new LongSparseArray<>();
    private LongSparseArray<Boolean> dateToUp = new LongSparseArray<>();
    private List<Integer> dateIndices = new ArrayList<>();
    private SparseArray<Long> lineToTime = new SparseArray<>();
    private SparseArray<Boolean> lineToUp = new SparseArray<>();
    private boolean[] lineDisabled;
    private int selectedIndex = -1;
    private int oldSelectedIndex = -1;
    private int oldFromIndex, oldToIndex;

    int _24dp;

    private int maxIndex;
    private int d;

    Listener listener;
    float[][] coords;
    Path[] paths;

    private static final int BAR = 0;
    private static final int ANIMATING = 1;
    private static final int CIRCLE = 2;
    int state = BAR;

    RectF oval = new RectF();
    long[] sums;
    float[] angles;

    public PercentageBarLineView(Context context) {
        super(context);
        init();
    }

    public PercentageBarLineView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public PercentageBarLineView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    //float drawX = 0f;

    protected void init() {
        super.init();
        titleMargin = 0;
        setPadding(0, Utils.dpToPx(16), 0, 0);
        _24dp = Utils.dpToPx(24);

        sw = xTextP.measureText(new SimpleDateFormat("MMM dd", Locale.US).format(0));

        setOnTouchListener(new OnTouchListener() {
            private float startY = 0f;
            boolean startOnZoomOut = false;

            @SuppressLint("ClickableViewAccessibility")
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (state == ANIMATING) return true;

                int paddingStart = getPaddingLeft();
                int paddingEnd = getPaddingRight();
                int w = getWidth() - paddingStart - paddingEnd;
                boolean needToInvalidate = false;

                if (state == BAR) {
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        startY = event.getY();
                        touchStart = System.currentTimeMillis();
                        labelWasShown = labelShown;
                        labelPressed = labelRectF.contains(event.getX(), event.getY());
                        needToInvalidate = true;
                        getParent().requestDisallowInterceptTouchEvent(true);
                    }

                    if (event.getAction() == MotionEvent.ACTION_DOWN
                            || event.getAction() == MotionEvent.ACTION_MOVE) {
                        //drawX = event.getX();
                        //invalidate();
                        if (event.getX() < paddingStart || event.getX() > getWidth() - paddingEnd) {
                            return true;
                        }

                        if (labelWasShown
                                && System.currentTimeMillis() - touchStart < TAP_TIMEOUT) {
                            if (needToInvalidate) invalidate();
                            return true;
                        }

                        if (labelRectF.contains(event.getX(), event.getY())) {
                            if (needToInvalidate) invalidate();
                            return true;
                        }

                        if (Math.abs(event.getY() - startY) > Utils.dpToPx(30)) {
                            getParent().requestDisallowInterceptTouchEvent(false);
                        }
                        float v1 = (event.getX() - paddingStart) / w;
                        long x = (long) (fromX + v1 * (toX - fromX));
                        long[] columnX = data.columns[0].value;
                        int search = Arrays.binarySearch(columnX, x);
                        int round;
                        if (search < 0) {
                            round = Math.min(columnX.length - 1, -search - 1) - 1;
                        } else {
                            round = Math.min(columnX.length - 1, search) - 1;
                        }
                        int newIndex = Math.min(toIndex - 1, Math.max(round, fromIndex));
                        if (selectedIndex != newIndex) needToInvalidate = true;
                        selectedIndex = newIndex;
                        labelShown = true;
                        //Log.v("LineView", "v1=" + v1 + " x=" + x + " index=" + selectedIndex);
                    } else {
                        getParent().requestDisallowInterceptTouchEvent(false);
                        needToInvalidate = true;
                        if (labelPressed) {
                            Log.v("bars", "clicked label");
                            if (listener != null) {
                                listener.onPressed(data.columns[0].value[selectedIndex]);
                            }
                            labelPressed = false;
                            labelShown = false;
                            labelWasShown = false;
                            labelRectF.set(0, 0, 0, 0);
                        } else {
                            if (labelWasShown
                                    && System.currentTimeMillis() - touchStart < TAP_TIMEOUT) {
                                labelRectF.set(0, 0, 0, 0);
                                labelShown = false;
                                labelWasShown = false;
                                selectedIndex = -1;
                            } else {
                                labelWasShown = true;
                            }
                        }
                    }
                    if (needToInvalidate) invalidate();
                    return true;
                } else if (state == CIRCLE) {
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        startY = event.getY();
                        touchStart = System.currentTimeMillis();
                        startOnZoomOut = false;
                        if (zoomOutRectF.contains(event.getX(), event.getY())) {
                            startOnZoomOut = true;
                            return true;
                        } else if (Math.sqrt(Math.pow(event.getX() - oval.centerX(), 2) + Math.pow(
                                event.getY() - oval.centerY(), 2)) > oval.width() / 2f) {
                            if (selectedIndex != -1) {
                                selectedIndex = -1;
                                invalidate();
                            }
                            return false;
                        }
                        needToInvalidate = true;
                        getParent().requestDisallowInterceptTouchEvent(true);
                    }

                    if (event.getAction() == MotionEvent.ACTION_DOWN
                            || event.getAction() == MotionEvent.ACTION_MOVE) {
                        //drawX = event.getX();
                        //invalidate();
                        float ex = event.getX();
                        float ey = event.getY();

                        if (Math.sqrt(Math.pow(event.getX() - oval.centerX(), 2) + Math.pow(
                                event.getY() - oval.centerY(), 2)) > oval.width() / 2f) {
                            return false;
                        }

                        //if (Math.abs(ey - startY) > Utils.dpToPx(30)) {
                        //    getParent().requestDisallowInterceptTouchEvent(false);
                        //}

                        double angle = 0;
                        if (ex != oval.centerX()) {
                            angle = Math.toDegrees(
                                    Math.atan2(ey - oval.centerY(), ex - oval.centerX()));
                        }
                        //Log.v("circle", "angle=" + angle);
                        if (angle < 0) angle += 360;
                        int newIndex = 0;
                        float sumAngle = 0;
                        for (int i = 0; i < angles.length; i++) {
                            if (angle < sumAngle + angles[i] && angle > sumAngle) {
                                newIndex = i;
                                break;
                            }
                            sumAngle += angles[i];
                        }

                        if (selectedIndex != newIndex) needToInvalidate = true;
                        selectedIndex = newIndex;
                    } else {
                        if (event.getAction() == MotionEvent.ACTION_UP && zoomOutRectF.contains(
                                event.getX(), event.getY()) && startOnZoomOut) {
                            listener.onZoomOut();
                            return true;
                        }
                        selectedIndex = -1;
                        getParent().requestDisallowInterceptTouchEvent(false);
                        needToInvalidate = true;
                    }
                    if (needToInvalidate) invalidate();
                    return true;
                }
                return false;
            }
        });
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        long time = System.currentTimeMillis();
        //Log.v("LineView", "====");

        int paddingStart = getPaddingLeft();
        int paddingEnd = getPaddingRight();
        int paddingTop = getPaddingTop();
        int paddingBottom = getPaddingBottom();

        Paint.FontMetrics titlePFM = titleP.getFontMetrics();
        float titleH = titlePFM.descent - titlePFM.ascent;

        int w = getWidth() - paddingStart - paddingEnd;
        int h = (int) (getHeight()
                - paddingBottom
                - paddingTop
                - titleH
                - titleMargin
                - xTextP.getTextSize()
                - Utils.dpToPx(6));

        if (data.columns.length == 0) return;

        if (step0Time != 0L) {
            step0 = (long) (step0k * (time - step0Time) + step0b);
            if (step0Down) {
                step0 = Math.min(step0, (long) (maxY * 0.7f / 5f));
            } else {
                step0 = Math.max(step0, (long) (maxY * 0.7f / 5f));
            }
            //Log.v("LineView", "maxY=" + maxY + " step0=" + step0);
        }

        if (state == BAR) {
            titleP.setColor(titleColor);
            canvas.drawText("Fruits", paddingStart + _24dp, paddingTop - titlePFM.ascent, titleP);

            String dateText = "";
            if (isSameDay(data.columns[0].value[fromIndex], data.columns[0].value[toIndex - 1])) {
                date.setTime(data.columns[0].value[fromIndex]);
                dateText = titleDateFormat.format(date);
            } else {
                date.setTime(data.columns[0].value[fromIndex]);
                String fromDateText = shortDateFormat.format(date);
                date.setTime(data.columns[0].value[toIndex - 1]);
                String toDateText = shortDateFormat.format(date);
                dateText = fromDateText + " - " + toDateText;
            }

            canvas.drawText(dateText, getWidth() - _24dp - titleDateP.measureText(dateText),
                    paddingTop - titlePFM.ascent, titleDateP);
        } else if (state == CIRCLE) {
            titleP.setColor(zoomOutColor);
            zoomOutRectF.set(paddingStart + _24dp, paddingTop,
                    paddingStart + _24dp + titleP.measureText("Zoom out"), paddingTop + titleH);
            canvas.drawText("Zoom out", paddingStart + _24dp, paddingTop - titlePFM.ascent, titleP);

            String dateText = "";
            if (isSameDay(data.columns[0].value[fromIndex], data.columns[0].value[toIndex - 1])) {
                date.setTime(data.columns[0].value[fromIndex]);
                dateText = titleDateFormat.format(date);
            } else {
                date.setTime(data.columns[0].value[fromIndex]);
                String fromDateText = shortDateFormat.format(date);
                date.setTime(data.columns[0].value[toIndex - 1]);
                String toDateText = shortDateFormat.format(date);
                dateText = fromDateText + " - " + toDateText;
            }

            canvas.drawText(dateText, getWidth() - _24dp - titleDateP.measureText(dateText),
                    paddingTop - titlePFM.ascent, titleDateP);
        }

        canvas.save();
        canvas.translate(0, titleH + titleMargin);

        if (state == ANIMATING) {
            drawPaths(canvas);
        } else if (state == CIRCLE) {
            drawCircle(canvas, time, fromIndex, toIndex);
            if (selectedIndex != -1) {
                drawCircleLabel(canvas, oval.left, oval.top);
            }
        } else if (state == BAR) {
            drawLines(canvas, time, fromIndex, toIndex);
            // draw axis
            int size = yToTime.size();
            boolean maxWasDrawn = false;
            for (int j = 0; j < size; j++) {
                long yKey = yToTime.keyAt(j);
                if (yKey == 0) continue;
                int alpha;
                if (yKey == maxY) {
                    maxWasDrawn = true;
                    alpha = Math.min(
                            (int) (1f * MAX_ALPHA / ANIMATION_DURATION * (time - yToTime.get(
                                    yKey))), MAX_ALPHA);
                } else {
                    alpha = Math.max(
                            (int) (1f * MAX_ALPHA / ANIMATION_DURATION * (yToTime.get(yKey) - time
                                    + ANIMATION_DURATION)), 0);
                }
                //Log.v("LineView", "maxY=" + maxY + " y=" + yKey + ", alpha=" + alpha);
                axisP.setColor(axisColor);
                axisP.setAlpha(25);
                getAxisTexts(yKey, 0);
                for (int i = 0; i < 6; i++) {
                    float y = convertToY(h, (long) (i * yKey / 5f));
                    canvas.drawLine(paddingStart + _24dp, paddingTop + y,
                            getWidth() - paddingEnd - _24dp, paddingTop + y, axisP);
                    canvas.drawText(axisTexts[i], paddingStart + _24dp,
                            paddingTop + y - axisTextP.descent() - Utils.dpToPx(3), axisTextP);
                }
            }

            for (int j = 0; j < yToTime.size(); j++) {
                long yKey = yToTime.keyAt(j);
                long l = time - yToTime.get(yKey);
                //Log.v("LineView", "maxY=" + maxY + " y=" + yKey + " l=" + l);
                if (l > ANIMATION_DURATION) {
                    //Log.v("LineView", "maxY=" + maxY + " remove y=" + yKey);
                    yToTime.remove(yKey);
                }
            }

            //Log.v("LineView", "yToTime.get(maxY)=" + yToTime.get(maxY));
            if (!maxWasDrawn && maxY != 0f) {
                //Log.v("LineView", "maxY=" + maxY + " normal alpha=" + axisP.getAlpha());
                getAxisTexts(maxY, 0);
                for (int i = 0; i < 6; i++) {
                    axisP.setColor(axisColor);
                    axisP.setAlpha(25);
                    float y = convertToY(h, (long) (i * maxY / 5f));
                    canvas.drawLine(paddingStart + _24dp, paddingTop + y,
                            getWidth() - paddingEnd - _24dp, paddingTop + y, axisP);
                    canvas.drawText(axisTexts[i], paddingStart + _24dp,
                            paddingTop + y - axisTextP.descent() - Utils.dpToPx(3), axisTextP);
                }
            }

            {
                //axisP.setColor(axisColorDark);
                //float y = h - 1;
                //canvas.drawLine(paddingStart + _24dp, paddingTop + y,
                //        getWidth() - paddingEnd - _24dp, paddingTop + y, axisP);
                //canvas.drawText("0", paddingStart + _24dp,
                //        paddingTop + y - axisTextP.descent() - Utils.dpToPx(3), axisTextP);
            }

            Data.Column columnX = data.columns[0];
            if (selectedIndex != -1) {
                float x = w * (columnX.value[selectedIndex] - fromX) * 1f / (toX - fromX);

                //Log.v("LineView", "x=" + x);

                canvas.drawLine(paddingStart + x, paddingTop + convertToY(h, maxY),
                        paddingStart + x, paddingTop + h, vertAxisP);
            }

            if (selectedIndex != -1) {
                float x = w * (columnX.value[selectedIndex] - fromX) * 1f / (toX - fromX);
                float endX = w * (columnX.value[selectedIndex + 1] - fromX) * 1f / (toX - fromX);
                int minX = paddingStart + Utils.dpToPx(5);
                int maxX = getWidth() - paddingEnd - Utils.dpToPx(5);
                drawLabel(canvas, paddingStart + x, convertToY(h, maxY) + Utils.dpToPx(5), minX,
                        maxX, x, endX, selectedIndex);
            }

            canvas.restore();
            for (int i : dateIndices) {
                String s = formatDate(columnX.value[i]);//dateFormat.format(date);
                float x = w * (columnX.value[i] - fromX) * 1f / (toX - fromX);

                int alpha;
                Boolean up = dateToUp.get(i);
                if (up == null) {
                    alpha = MAX_ALPHA;
                } else if (up) {
                    alpha = Math.min((int) (1f * MAX_ALPHA / DATE_ANIMATION_DURATION * (time
                            - dateToTime.get(i))), MAX_ALPHA);
                } else {
                    alpha = Math.max(
                            (int) (1f * MAX_ALPHA / DATE_ANIMATION_DURATION * (dateToTime.get(i)
                                    - time + DATE_ANIMATION_DURATION)), 0);
                }
                //Log.v("LineView",
                //        "date=" + i + ", dateToTime=" + dateToTime.get(i) + ", alpha=" + alpha);
                xTextP.setAlpha(alpha);

                canvas.drawText(s, paddingStart + x, getHeight() - paddingBottom - Utils.dpToPx(3),
                        xTextP);
            }

            boolean removed = false;
            //Log.v("LineView", "size1=" + dateIndices.size());
            for (int j = 0; j < dateIndices.size() - 1; ) {
                //Log.v("LineView", "=====");
                //Log.v("LineView", "j=" + j);

                int index = dateIndices.get(j);
                float x = w * (columnX.value[index] - fromX) * 1f / (toX - fromX);

                int k;
                boolean found = false;
                for (k = j + 1; k < dateIndices.size(); k++) {
                    int mbIndex = dateIndices.get(k);
                    Boolean up = dateToUp.get(mbIndex);
                    if (up != null && !up) continue;
                    float x2 = w * (columnX.value[mbIndex] - fromX) * 1f / (toX - fromX);

                    //Log.v("LineView", "x2 " + x2 + " x=" + x);
                    //Log.v("LineView", "x2 - x - sw=" + (x2 - x - sw));

                    if (x2 - x - sw > DATE_MARGIN) {
                        found = true;
                        break;
                    }
                }
                //Log.v("LineView", "k=" + k);

                if (found) {
                    for (int i = j + 1; i < k; i++) {
                        //Log.v("LineView", "deleting " + i2 + " dateToTime.get(i2)=" + dateToTime.get(i2));
                        int dateIndex = dateIndices.get(i);
                        if (dateToTime.get(dateIndex) == null) {
                            //Log.v("LineView", "deleting " + i2);
                            dateToTime.put(dateIndex, time);
                            dateToUp.put(dateIndex, false);
                            removed = true;
                        } else {
                            if (dateToUp.get(dateIndex)) {
                                Long l = dateToTime.get(dateIndex);
                                long l1 = time - l;
                                long value = l + 2 * l1 - DATE_ANIMATION_DURATION;
                                dateToTime.put(dateIndex, value);
                                dateToUp.put(dateIndex, false);
                                removed = true;
                            }
                        }
                    }
                }

                j = k;
            }

            if (removed) {
                d = 2 * d + 1;
                //Log.v("LineView", "d=" + d);
            }

            boolean added = false;
            int size1 = dateIndices.size();
            for (int j = 0; j < size1 - 1; j++) {
                int i = dateIndices.get(j);
                int i2 = dateIndices.get(j + 1);
                float x = w * (columnX.value[i] - fromX) * 1f / (toX - fromX);
                float x2 = w * (columnX.value[i2] - fromX) * 1f / (toX - fromX);
                int midJ = (i + i2) / 2;
                if (x2 - x - sw > sw + 2 * DATE_MARGIN) {
                    //Log.v("LineView",
                    //        "maybe adding " + midJ + ", dateToTime.get(midJ)=" + dateToTime.get(midJ));
                    if (dateToTime.get(midJ) == null) {
                        float xMid = w * (columnX.value[midJ] - fromX) * 1f / (toX - fromX);
                        if (x2 - xMid - sw < DATE_MARGIN || xMid - x - sw < DATE_MARGIN) {
                            continue;
                        }

                        //Log.v("LineView", "adding " + midJ);
                        dateIndices.add(midJ);
                        dateToTime.put(midJ, time);
                        dateToUp.put(midJ, true);
                        added = true;
                    } else {
                        //Log.v("LineView", "already here, up?=" + dateToUp.get(midJ));
                        if (!dateToUp.get(midJ)) {
                            Long l = dateToTime.get(midJ);
                            long l1 = time - l;
                            long value = l + 2 * l1 - DATE_ANIMATION_DURATION;
                            dateIndices.add(midJ);
                            dateToTime.put(midJ, value);
                            dateToUp.put(midJ, true);
                            added = true;
                        }
                    }
                }
            }
            if (added) {
                d = (d - 1) / 2;
                //Log.v("LineView", "d=" + d);
            }

            if (!dateIndices.isEmpty()) {
                int lastDate = dateIndices.get(size1 - 1);
                float x = w * (columnX.value[lastDate] - fromX) * 1f / (toX - fromX);
                if (w - x - sw / 2f > DATE_MARGIN + sw) {
                    //Log.v("LineView", "w - x - sw / 2f=" + (w - x - sw / 2f));
                    int i = Math.min(maxIndex, lastDate + d + 1);
                    float checkX = w * (columnX.value[i] - fromX) * 1f / (toX - fromX);
                    if (w - checkX - sw / 2f > 0) {
                        dateIndices.add(i);
                        dateToTime.put(i, time);
                        dateToUp.put(i, true);
                    }
                }
            }

            if (!dateIndices.isEmpty()) {
                int firstDate = dateIndices.get(0);
                float x = w * (columnX.value[firstDate] - fromX) * 1f / (toX - fromX);
                if (x - sw / 2f > DATE_MARGIN + sw) {
                    //Log.v("LineView", "x - sw / 2f=" + (x - sw / 2f));
                    int i = Math.max(0, firstDate - d - 1);
                    float checkX = w * (columnX.value[i] - fromX) * 1f / (toX - fromX);
                    if (checkX - sw / 2f > 0 && x - checkX > sw + DATE_MARGIN) {
                        dateIndices.add(i);
                        dateToTime.put(i, time);
                        dateToUp.put(i, true);
                    }
                }
            }

            Collections.sort(dateIndices);
        }

        for (int i = 0; i < dateIndices.size(); i++) {
            int index = dateIndices.get(i);
            Long start = dateToTime.get(index);
            if (start != null && time - start > DATE_ANIMATION_DURATION) {
                //Log.v("LineView", "start=" + start);
                if (!dateToUp.get(index)) {
                    dateIndices.remove(i);
                    --i;
                }
                dateToTime.remove(index);
                dateToUp.remove(index);
            }
        }

        for (int j = 1; j < data.columns.length; j++) {
            Long lineTime = lineToTime.get(j);
            if (lineTime == null) continue;
            if (time - lineTime > ANIMATION_DURATION) {
                lineDisabled[j] = !lineToUp.get(j);
                lineToUp.remove(j);
                lineToTime.remove(j);
                invalidate();
            }
        }

        if (time - step0Time > ANIMATION_DURATION) {
            step0Time = 0L;
            step0 = (long) (maxY * 0.7f / 5f);
        }

        //canvas.drawLine(drawX, 0, drawX, h, axisP);

        //Log.v("LineView", "time=" + (System.currentTimeMillis() - time) + "ms");

        if (lineToTime.size() != 0
                || dateToTime.size() != 0
                || yToTime.size() != 0
                || step0Time != 0L) {
            postInvalidateOnAnimation();
        }
    }

    void drawLines(Canvas canvas, long time, int fromIndex, int toIndex) {
        int paddingStart = getPaddingLeft();
        int paddingEnd = getPaddingRight();
        int paddingTop = getPaddingTop();
        int paddingBottom = getPaddingBottom();

        Paint.FontMetrics titlePFM = titleP.getFontMetrics();
        float titleH = titlePFM.descent - titlePFM.ascent;

        int w = getWidth() - paddingStart - paddingEnd;
        int h = (int) (getHeight()
                - paddingBottom
                - paddingTop
                - titleH
                - titleMargin
                - xTextP.getTextSize()
                - Utils.dpToPx(6));

        Data.Column columnX = data.columns[0];

        for (int i = fromIndex; i < toIndex; i++) {
            long sum = 0;
            for (int j = 1; j < data.columns.length; j++) {
                if (lineDisabled[j]) continue;

                Data.Column column = data.columns[j];
                float y = column.value[i];
                if (lineToTime.get(j) != null) {
                    float k;
                    if (lineToUp.get(j)) {
                        k = (time - lineToTime.get(j)) * 1f / ANIMATION_DURATION;
                    } else {
                        k = 1 + (lineToTime.get(j) - time) * 1f / ANIMATION_DURATION;
                    }
                    y *= Math.min(1, Math.max(0, k));
                }

                sum += y;
            }
            //Log.v("perc", "=====");
            //Log.v("perc", "sum=" + sum);

            float prevMinY = 0;
            for (int j = 1; j < data.columns.length; j++) {
                if (lineDisabled[j]) {
                    coords[j][i] = prevMinY;
                    continue;
                }

                Data.Column column = data.columns[j];
                float y = column.value[i];
                if (lineToTime.get(j) != null) {
                    float k;
                    if (lineToUp.get(j)) {
                        k = (time - lineToTime.get(j)) * 1f / ANIMATION_DURATION;
                    } else {
                        k = 1 + (lineToTime.get(j) - time) * 1f / ANIMATION_DURATION;
                    }
                    y *= Math.min(1, Math.max(0, k));
                }

                float percentage = y * 100f / sum;
                //Log.v("perc", "percentage=" + percentage);
                prevMinY += percentage;
                coords[j][i] = prevMinY;
            }
            //Log.v("perc", "prevMinY=" + prevMinY);

            //if (selectedIndex != -1 && i != selectedIndex) {
            //    canvas.drawRect((float) Math.floor(startX), convertToY(h, prevMinY),
            //            (float) Math.ceil(stopX), convertToY(h, 0), maskP);
            //}
        }

        for (int i = 1; i < data.columns.length; i++) {
            paths[i - 1].reset();
            //if (lineDisabled[i - 1]) continue;
            float[] coord = coords[i];
            //Log.v("perc", "i=" + i + " coord=" + Arrays.toString(coord));
            for (int j = fromIndex; j < toIndex; j++) {
                if (j == fromIndex) {
                    paths[i - 1].moveTo(w * (columnX.value[j] - fromX) * 1f / (toX - fromX),
                            paddingTop + convertToY(h, coord[j]));
                } else {
                    paths[i - 1].lineTo(w * (columnX.value[j] - fromX) * 1f / (toX - fromX),
                            paddingTop + convertToY(h, coord[j]));
                }
            }
            coord = coords[i - 1];
            for (int j = toIndex - 1; j >= fromIndex; j--) {
                paths[i - 1].lineTo(w * (columnX.value[j] - fromX) * 1f / (toX - fromX),
                        paddingTop + convertToY(h, coord[j]));
            }
        }

        for (int k = 1; k < data.columns.length; k++) {
            p.setColor(data.columns[k].color);
            canvas.drawPath(paths[k - 1], p);
        }
    }

    void drawCircle(Canvas canvas, long time, int fromIndex, int toIndex) {
        int paddingStart = getPaddingLeft();
        int paddingEnd = getPaddingRight();
        int paddingTop = getPaddingTop();
        int paddingBottom = getPaddingBottom();

        int w = getWidth() - paddingStart - paddingEnd - 2 * _24dp;

        long sum = 0;
        for (int j = 1; j < data.columns.length; j++) {
            long colSum = 0;
            for (int i = fromIndex; i < toIndex; i++) {
                if (lineDisabled[j]) continue;

                Data.Column column = data.columns[j];
                float y = column.value[i];
                if (lineToTime.get(j) != null) {
                    float k;
                    if (lineToUp.get(j)) {
                        k = (time - lineToTime.get(j)) * 1f / ANIMATION_DURATION;
                    } else {
                        k = 1 + (lineToTime.get(j) - time) * 1f / ANIMATION_DURATION;
                    }
                    y *= Math.min(1, Math.max(0, k));
                }

                colSum += y;
                sum += y;
            }
            sums[j - 1] = colSum;
        }

        oval.set(paddingStart + _24dp, paddingTop + _24dp, paddingStart + _24dp + w,
                paddingTop + _24dp + w);

        float angle = 0;
        for (int k = 1; k < data.columns.length; k++) {
            p.setColor(data.columns[k].color);
            float columnAngle = angles[k - 1] = sums[k - 1] * 360f / sum;
            if (selectedIndex == k - 1) {
                int d = Utils.dpToPx(6);
                oval.offset((float) (d * Math.cos(Math.toRadians(angle + columnAngle / 2f))),
                        (float) (d * Math.sin(Math.toRadians(angle + columnAngle / 2f))));
                canvas.drawArc(oval, angle, columnAngle, true, p);
                oval.offset(-(float) (d * Math.cos(Math.toRadians(angle + columnAngle / 2f))),
                        -(float) (d * Math.sin(Math.toRadians(angle + columnAngle / 2f))));
            } else {
                canvas.drawArc(oval, angle, columnAngle, true, p);
            }
            angle += columnAngle;
        }

        angle = 0;

        for (int k = 1; k < data.columns.length; k++) {
            if (lineDisabled[k]) continue;
            float columnAngle = angles[k - 1];
            int value = Math.round(sums[k - 1] * 100f / sum);
            if (value == 0) continue;

            float x = (float) (oval.centerX() + oval.width() / 2.8f * Math.cos(
                    Math.toRadians(angle + columnAngle / 2f)));
            float y = (float) (oval.centerY() + oval.width() / 2.8f * Math.sin(
                    Math.toRadians(angle + columnAngle / 2f)));
            circleLabelP.setTextSize(Utils.dpToPx(Math.min(Math.max(value, 12), 40)));
            float labelW = circleLabelP.measureText(String.valueOf(value) + "%");
            float offsetX = 0;
            float offsetY = 0;
            if (selectedIndex == k - 1) {
                int d = Utils.dpToPx(6);
                offsetX = (float) (d * Math.cos(Math.toRadians(angle + columnAngle / 2f)));
                offsetY = (float) (d * Math.sin(Math.toRadians(angle + columnAngle / 2f)));
            }
            canvas.drawText(String.valueOf(value) + "%", x - labelW / 2 + offsetX,
                    y - circleLabelP.ascent() / 2 + offsetY, circleLabelP);
            angle += columnAngle;
        }
    }

    private void drawCircleLabel(Canvas canvas, float x0, float y0) {
        float w, h;
        float paddingStart = Utils.dpToPx(10);
        float paddingEnd = Utils.dpToPx(10);
        float paddingTop = Utils.dpToPx(10);
        float paddingBottom = Utils.dpToPx(10);

        Data.Column columnX = data.columns[0];
        Data.Column column = data.columns[selectedIndex + 1];

        long value = 0;
        for (int i = fromIndex; i < toIndex; i++) {
            value += column.value[i];
        }

        int minW = Utils.dpToPx(180);
        int minMargin = Utils.dpToPx(16);
        w = Math.max(minW, paddingStart
                + dataNameP.measureText(column.name)
                + minMargin
                + dataValueP.measureText(String.valueOf(value))
                + paddingEnd);

        Paint.FontMetrics dataPFM = dataValueP.getFontMetrics();
        float dataH = dataPFM.descent - dataPFM.ascent;

        Paint.FontMetrics dataLabelPFM = dataNameP.getFontMetrics();
        float dataLabelH = dataLabelPFM.descent - dataLabelPFM.ascent;

        h = paddingTop + Math.max(dataLabelH, dataH) + paddingBottom;

        float startX = x0;

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            shadowRectF.set(startX - 1, y0 - 1, startX + w + 1, y0 + h + 1);
            canvas.drawRoundRect(shadowRectF, 10, 10, shadowP);
        }

        labelRectF.set(startX, y0, startX + w, y0 + h);
        canvas.drawRoundRect(labelRectF, 10, 10, labelP);

        if (labelPressed) {
            canvas.drawRoundRect(labelRectF, 10, 10, labelPressedBackgroundP);
        }

        float valueW = dataValueP.measureText(String.valueOf(value));
        dataValueP.setColor(column.color);

        canvas.drawText(column.name, startX + paddingStart, y0 + paddingTop - dataPFM.ascent,
                dataNameP);
        canvas.drawText(String.valueOf(value), startX + w - paddingEnd - valueW,
                y0 + paddingTop - dataLabelPFM.ascent, dataValueP);
    }

    private void drawLabel(Canvas canvas, float x0, float y0, float minX, float maxX,
            float selectedX, float selectedEndX, int index) {
        float w, h;
        float paddingStart = Utils.dpToPx(10);
        float paddingEnd = Utils.dpToPx(10);
        float paddingTop = Utils.dpToPx(10);
        float paddingBottom = Utils.dpToPx(10);
        int marginPerc = Utils.dpToPx(6);

        Data.Column columnX = data.columns[0];
        long time = columnX.value[index];

        date.setTime(time);
        String dateText = labelDateFormat.format(date);

        float maxLineNameW = 0;
        float maxLineValueW = 0;
        float maxLinePercW = 0;
        long sum = 0;
        for (int i = 1; i < data.columns.length; i++) {
            if (lineDisabled[i]) continue;
            sum += data.columns[i].value[index];
        }
        for (int i = 1; i < data.columns.length; i++) {
            if (lineDisabled[i]) continue;
            Data.Column column = data.columns[i];
            long value = column.value[index];
            int perc = Math.round(value * 100f / sum);
            float valueW = dataValueP.measureText(formatForLabel(value));
            maxLineValueW = Math.max(maxLineValueW, valueW);

            float percW = dataPercP.measureText(String.valueOf(perc) + "%");
            maxLinePercW = Math.max(maxLinePercW, percW);

            float nameW = dataNameP.measureText(column.name);
            maxLineNameW = Math.max(maxLineNameW, nameW);
        }

        maxLineValueW = Math.max(maxLineValueW, dataValueP.measureText(String.valueOf(sum)));

        int minW = Utils.dpToPx(180);
        int minMargin = Utils.dpToPx(16);
        w = Math.max(minW, paddingStart
                + maxLinePercW
                + marginPerc
                + maxLineNameW
                + minMargin
                + maxLineValueW
                + paddingEnd);

        Paint.FontMetrics dateLabelPFM = dateLabelP.getFontMetrics();
        float dateLabelH = dateLabelPFM.descent - dateLabelPFM.ascent;

        Paint.FontMetrics dataPFM = dataValueP.getFontMetrics();
        float dataH = dataPFM.descent - dataPFM.ascent;

        Paint.FontMetrics dataLabelPFM = dataNameP.getFontMetrics();
        float dataLabelH = dataLabelPFM.descent - dataLabelPFM.ascent;

        int linesCount = 0;
        for (int i = 1; i < data.columns.length; i++) {
            if (lineDisabled[i]) continue;
            ++linesCount;
        }

        int marginBetweenLines = Utils.dpToPx(8);
        h = paddingTop + dateLabelH + linesCount * (Math.max(dataLabelH, dataH)
                + marginBetweenLines) + paddingBottom;

        float startX = selectedX - w - Utils.dpToPx(16);
        if (startX < Utils.dpToPx(16)) {
            if (selectedEndX + Utils.dpToPx(4) + w > getWidth()) {
                startX = Utils.dpToPx(16);
            } else {
                startX = selectedEndX + Utils.dpToPx(4);
            }
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            shadowRectF.set(startX - 1, y0 - 1, startX + w + 1, y0 + h + 1);
            canvas.drawRoundRect(shadowRectF, 10, 10, shadowP);
        }

        labelRectF.set(startX, y0, startX + w, y0 + h);
        canvas.drawRoundRect(labelRectF, 10, 10, labelP);

        if (labelPressed) {
            canvas.drawRoundRect(labelRectF, 10, 10, labelPressedBackgroundP);
        }

        canvas.drawText(dateText, startX + paddingStart, y0 + paddingTop - dateLabelPFM.ascent,
                dateLabelP);

        drawArrow(canvas, y0 + paddingTop, startX + w - paddingEnd - Utils.dpToPx(4));

        float currentH = dateLabelH + marginBetweenLines;
        for (int i = 1; i < data.columns.length; i++) {
            if (lineDisabled[i]) continue;
            Data.Column column = data.columns[i];
            long value = column.value[index];
            String vt = formatForLabel(value);
            int perc = Math.round(value * 100f / sum);
            float valueW = dataValueP.measureText(vt);
            float percW = dataPercP.measureText(String.valueOf(perc) + "%");
            dataValueP.setColor(column.color);

            canvas.drawText(String.valueOf(perc) + "%",
                    startX + paddingStart + maxLinePercW - percW,
                    y0 + paddingTop + currentH - dataLabelPFM.ascent, dataPercP);
            canvas.drawText(column.name, startX + paddingStart + maxLinePercW + marginPerc,
                    y0 + paddingTop + currentH - dataPFM.ascent, dataNameP);
            canvas.drawText(vt, startX + w - paddingEnd - valueW,
                    y0 + paddingTop + currentH - dataLabelPFM.ascent, dataValueP);

            currentH += Math.max(dataH, dataLabelH) + marginBetweenLines;
        }
    }

    private float convertToY(int h, float y) {
        if (maxY == 0f && step0 == 0f) return Float.POSITIVE_INFINITY;
        return h - h * y * 1f / (maxY + step0) - 1;
    }

    private float convertToY(int h, long y) {
        if (maxY == 0f && step0 == 0f) return Float.POSITIVE_INFINITY;
        return h - h * y * 1f / (maxY + step0) - 1;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        Data.Column columnX = data.columns[0];

        int paddingStart = getPaddingLeft();
        int paddingEnd = getPaddingRight();
        int paddingTop = getPaddingTop();
        int paddingBottom = getPaddingBottom();

        w = w - paddingStart - paddingEnd;
        h = h - paddingBottom - paddingTop;

        //Log.v("LineView", "w=" + w + " h=" + h + " ow=" + oldw + " oh=" + oldh);

        long start = System.currentTimeMillis();

        dateIndices.clear();
        dateIndices.add(toIndex - 1);
        int lastDateIndex = toIndex - 1;
        float lastCenterX = w * (columnX.value[lastDateIndex] - fromX) * 1f / (toX - fromX);
        d = 1;
        for (int i = lastDateIndex - 1; i >= fromIndex; i = lastDateIndex - d - 1) {
            float x = w * (columnX.value[i] - fromX) * 1f / (toX - fromX);
            if (lastCenterX - x - sw > DATE_MARGIN) {
                //Log.v("LineView", "x=" + x);
                break;
            } else {
                d = d * 2 + 1;
            }
        }

        for (int i = dateIndices.get(0) - d - 1; i >= 0; i -= d + 1) {
            dateIndices.add(i);

            //float x = w * (columnX.value[i] - fromX) * 1f / (toX - fromX);
            //if (lastCenterX == 0f) {
            //    if (w - x - sw / 2f > 0) {
            //        //Log.v("LineView", "x=" + x);
            //        lastCenterX = x;
            //        dateIndices.add(i);
            //    }
            //} else {
            //    if (lastCenterX - x - sw > DATE_MARGIN) {
            //        //Log.v("LineView", "x=" + x);
            //        lastCenterX = x;
            //        dateIndices.add(i);
            //    }
            //}
            //d = d * 2 + 1;
        }
        Collections.sort(dateIndices);

        //Log.v("LineView", "time=" + (System.currentTimeMillis() - start) + "ms");
    }

    Data diff = new Data();
    float progress;

    public void setDataWithoutUpdate(Data newData) {
        diff.columns = new Data.Column[data.columns.length];
        for (int i = 0; i < diff.columns.length; i++) {
            Data.Column column = new Data.Column();
            column.value = new long[data.columns[0].value.length];
            diff.columns[i] = column;
        }

        this.data = newData;
        lineDisabled = new boolean[data.columns.length];

        Data.Column columnX = data.columns[0];
        fromIndex = 0;
        toIndex = columnX.value.length - 1;
        minX = columnX.value[0];
        maxX = columnX.value[columnX.value.length - 1];
    }

    public void setMinMaxX(long minX, long maxX, final Data data) {
        //this.minX = minX;
        //this.maxX = maxX;

        dateIndices.clear();

        int fromIndex = Arrays.binarySearch(PercentageBarLineView.this.data.columns[0].value, minX);
        if (fromIndex < 0) fromIndex = -fromIndex - 1;
        int toIndex = Arrays.binarySearch(PercentageBarLineView.this.data.columns[0].value, maxX);
        if (toIndex < 0) toIndex = -toIndex - 1;

        PropertyValuesHolder minProp = PropertyValuesHolder.ofFloat("min", this.minX, minX);
        PropertyValuesHolder maxProp = PropertyValuesHolder.ofFloat("max", this.maxX, maxX);
        ValueAnimator valueAnimator = ValueAnimator.ofPropertyValuesHolder(minProp, maxProp);
        valueAnimator.setDuration(ANIMATION_DURATION);
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                PercentageBarLineView.this.minX =
                        ((Float) animation.getAnimatedValue("min")).longValue();
                PercentageBarLineView.this.maxX =
                        ((Float) animation.getAnimatedValue("max")).longValue();
                //StackedBarLineView.this.minX =
                //        data.columns[0].value[StackedBarLineView.this.fromIndex];
                //StackedBarLineView.this.maxX =
                //        data.columns[0].value[StackedBarLineView.this.toIndex];
                Log.v("lines", "minX="
                        + PercentageBarLineView.this.minX
                        + " maxX="
                        + PercentageBarLineView.this.maxX);

                invalidate();
            }
        });

        final int finalFromIndex = fromIndex;
        Log.v("lines", "fromIndex=" + finalFromIndex);
        valueAnimator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                //StackedBarLineView.this.fromIndex = finalFromIndex;
                //StackedBarLineView.this.toIndex = finalToIndex;
                //calculateMaxY();
                //invalidate();

                Data copy = new Data();
                copy.columns = new Data.Column[data.columns.length];
                copy.columns[0] = data.columns[0];
                for (int i = 1; i < copy.columns.length; i++) {
                    Data.Column column = new Data.Column();
                    column.value = new long[data.columns[0].value.length];
                    column.color = data.columns[i].color;
                    column.name = data.columns[i].name;
                    copy.columns[i] = column;
                }

                Data local = PercentageBarLineView.this.data;

                for (int j = 0; j < 7; j++) {
                    for (int i = 1; i < copy.columns.length; i++) {
                        Arrays.fill(copy.columns[i].value, j * 24, (j + 1) * 24,
                                local.columns[i].value[finalFromIndex + j]);
                    }
                }

                setDataWithoutUpdate(data);

                for (int j = PercentageBarLineView.this.fromIndex;
                        j < PercentageBarLineView.this.toIndex; ++j) {
                    if (data.columns[0].value[j] < PercentageBarLineView.this.minX
                            || data.columns[0].value[j] > PercentageBarLineView.this.maxX) {
                        continue;
                    }
                    long sum = 0;
                    for (int i = 1; i < data.columns.length; i++) {
                        if (lineDisabled[i] || lineToTime.get(i) != null && !lineToUp.get(i)) {
                            continue;
                        }
                        sum += data.columns[i].value[j];
                    }
                    maxY = Math.max(maxY, sum);
                }

                for (int i = 1; i < copy.columns.length; i++) {
                    Data.Column columnCopy = copy.columns[i];
                    Data.Column columnData = data.columns[i];
                    for (int j = 0; j < columnCopy.value.length; j++) {
                        diff.columns[i].value[j] = columnCopy.value[j] - columnData.value[j];
                    }
                }

                ValueAnimator diffAnim = ValueAnimator.ofFloat(1, 0);
                diffAnim.setDuration(ANIMATION_DURATION);
                diffAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        progress = (float) animation.getAnimatedValue();
                        invalidate();
                    }
                });
                diffAnim.start();
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        valueAnimator.start();

        //minFraction = 12 * 60 * 60 * 1000f / (maxX - minX);

        //setFrom(0f);
        //setTo(1f);

        invalidate();
    }

    public void setData(Data data) {
        fromIndex = 0;
        toIndex = 0;
        this.data = data;
        lineDisabled = new boolean[data.columns.length];
        sums = new long[data.columns.length - 1];
        angles = new float[data.columns.length - 1];

        Data.Column columnX = data.columns[0];
        minX = columnX.value[0];
        maxX = columnX.value[columnX.value.length - 1];
        maxIndex = columnX.value.length - 1;

        setFrom(0f);
        setTo(1f);

        int w = getWidth();
        if (w != 0) {
            dateIndices.clear();
            dateIndices.add(toIndex - 1);
            int lastDateIndex = toIndex - 1;
            float lastCenterX = w * (columnX.value[lastDateIndex] - fromX) * 1f / (toX - fromX);
            d = 1;
            for (int i = lastDateIndex - 1; i >= fromIndex; i = lastDateIndex - d - 1) {
                float x = w * (columnX.value[i] - fromX) * 1f / (toX - fromX);
                if (lastCenterX - x - sw > DATE_MARGIN) {
                    //Log.v("LineView", "x=" + x);
                    break;
                } else {
                    d = d * 2 + 1;
                }
            }

            for (int i = dateIndices.get(0) - d - 1; i >= 0; i -= d + 1) {
                dateIndices.add(i);
            }
            Collections.sort(dateIndices);
        }

        maxY = 100;
        step0 = (long) (maxY * 0.7f / 5f);

        coords = new float[data.columns.length][];
        for (int i = 0; i < coords.length; i++) {
            coords[i] = new float[columnX.value.length];
        }
        Arrays.fill(coords[0], 0f);
        Arrays.fill(coords[data.columns.length - 1], 100);

        paths = new Path[data.columns.length - 1];
        for (int i = 0; i < paths.length; i++) {
            paths[i] = new Path();
        }

        invalidate();
    }

    public void setFrom(float from) {
        labelPressed = false;
        labelShown = false;
        labelWasShown = false;
        selectedIndex = -1;
        labelRectF.set(0, 0, 0, 0);

        fromX = (long) (minX + from * (maxX - minX));
        if (fromX > toX) return;

        fromIndex = Arrays.binarySearch(data.columns[0].value, fromX);
        if (fromIndex < 0) fromIndex = Math.max(-fromIndex - 2, 0);
        //if (toIndex - fromIndex == 1) --fromIndex;

        log();

        invalidate();
    }

    public void setTo(float to) {
        labelPressed = false;
        labelShown = false;
        labelWasShown = false;
        selectedIndex = -1;
        labelRectF.set(0, 0, 0, 0);

        toX = (long) (minX + to * (maxX - minX));
        if (fromX > toX) return;

        Data.Column column = data.columns[0];
        toIndex = Arrays.binarySearch(column.value, toX);
        if (toIndex < 0) toIndex = Math.min(-toIndex, column.value.length);

        log();

        invalidate();
    }

    public void setLineEnabled(int index, boolean checked) {
        long time = System.currentTimeMillis();
        if (checked != lineDisabled[index]) return;

        if (lineToTime.get(index) == null) {
            lineToTime.put(index, time);
        } else {
            long value = time - (lineToTime.get(index) + ANIMATION_DURATION - time);
            lineToTime.put(index, value);
        }
        lineToUp.put(index, checked);
        if (checked) lineDisabled[index] = false;
        log();
        invalidate();
    }

    public void animateToCircle() {
        oldFromIndex = fromIndex;
        oldToIndex = toIndex;

        long sum = 0;
        for (int j = 1; j < data.columns.length; j++) {
            if (lineDisabled[j]) continue;

            Data.Column column = data.columns[j];
            float y = column.value[selectedIndex];

            sum += y;
        }

        float prevMinY = 0;
        for (int j = 1; j < data.columns.length; j++) {
            if (lineDisabled[j]) {
                coords[j][selectedIndex] = prevMinY;
                continue;
            }

            Data.Column column = data.columns[j];
            float y = column.value[selectedIndex];

            float percentage = y * 100f / sum;
            prevMinY += percentage;
            coords[j][selectedIndex] = prevMinY;
        }

        coords[0][selectedIndex] = 0;

        for (int i = 1; i < data.columns.length; i++) {
            float[] coord = coords[i - 1];
            float tg;
            if (toIndex - 1 != selectedIndex) {
                tg = (coord[selectedIndex] - 50) / (toIndex - 1 - selectedIndex);
            } else {
                tg = 0;
            }
            angles[i - 1] = (float) Math.toDegrees(
                    Math.atan2(coords[i][selectedIndex] - coord[selectedIndex],
                            (toIndex - 1 - selectedIndex) + tg * (coords[i][selectedIndex] - 50)));
        }

        state = ANIMATING;
        ValueAnimator diffAnim = ValueAnimator.ofFloat(0, 1);
        diffAnim.setDuration(STATE_ANIMATION_DURATION);
        diffAnim.setInterpolator(new AccelerateDecelerateInterpolator());
        diffAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                progress = (float) animation.getAnimatedValue();
                invalidate();
            }
        });
        diffAnim.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                oldSelectedIndex = selectedIndex;
                selectedIndex = -1;
                state = CIRCLE;
                invalidate();
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        diffAnim.start();
        invalidate();
    }

    public void animateToBar() {
        selectedIndex = oldSelectedIndex;
        setFromIndex(oldFromIndex);
        setToIndex(oldToIndex);

        long sum = 0;
        for (int j = 1; j < data.columns.length; j++) {
            if (lineDisabled[j]) continue;

            Data.Column column = data.columns[j];
            float y = column.value[selectedIndex];

            sum += y;
        }

        float prevMinY = 0;
        for (int j = 1; j < data.columns.length; j++) {
            if (lineDisabled[j]) {
                coords[j][selectedIndex] = prevMinY;
                continue;
            }

            Data.Column column = data.columns[j];
            float y = column.value[selectedIndex];

            float percentage = y * 100f / sum;
            prevMinY += percentage;
            coords[j][selectedIndex] = prevMinY;
        }

        coords[0][selectedIndex] = 0;

        for (int i = 1; i < data.columns.length; i++) {
            float[] coord = coords[i - 1];
            float tg = (coord[selectedIndex] - 50) / (toIndex - 1 - selectedIndex);
            angles[i - 1] = (float) Math.toDegrees(
                    Math.atan2(coords[i][selectedIndex] - coord[selectedIndex],
                            (toIndex - 1 - selectedIndex) + tg * (coords[i][selectedIndex] - 50)));
        }

        state = ANIMATING;
        ValueAnimator diffAnim = ValueAnimator.ofFloat(1, 0);
        diffAnim.setDuration(STATE_ANIMATION_DURATION);
        diffAnim.setInterpolator(new AccelerateDecelerateInterpolator());
        diffAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                progress = (float) animation.getAnimatedValue();
                invalidate();
            }
        });
        diffAnim.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                state = BAR;
                invalidate();
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        diffAnim.start();
        invalidate();
    }

    void drawPaths(Canvas canvas) {
        int paddingStart = getPaddingLeft();
        int paddingEnd = getPaddingRight();
        int paddingTop = getPaddingTop();
        int paddingBottom = getPaddingBottom();

        Paint.FontMetrics titlePFM = titleP.getFontMetrics();
        float titleH = titlePFM.descent - titlePFM.ascent;
        int w = getWidth() - paddingStart - paddingEnd;
        int h = (int) (getHeight()
                - paddingBottom
                - paddingTop
                - titleH
                - titleMargin
                - xTextP.getTextSize()
                - Utils.dpToPx(6));

        Data.Column columnX = data.columns[0];

        if (progress <= 0.5f) {
            progress *= 2;
            for (int i = fromIndex; i <= selectedIndex; i++) {
                long sum = 0;
                for (int j = 1; j < data.columns.length; j++) {
                    if (lineDisabled[j]) continue;

                    Data.Column column = data.columns[j];
                    float y = column.value[i];

                    sum += y;
                }

                float prevMinY = 0;
                for (int j = 1; j < data.columns.length; j++) {
                    if (lineDisabled[j]) {
                        coords[j][i] = prevMinY;
                        continue;
                    }

                    Data.Column column = data.columns[j];
                    float y = column.value[i];

                    float percentage = y * 100f / sum;
                    prevMinY += percentage;
                    if (i <= selectedIndex) {
                        coords[j][i] = prevMinY;
                    } else {
                        float cy = prevMinY + (coords[j][selectedIndex] - prevMinY) * progress;
                        coords[j][i] = cy + (50 - cy) * ((i - selectedIndex) * 1f / (toIndex
                                - selectedIndex)) * progress;
                    }
                }

                if (i <= selectedIndex) {
                    coords[0][i] = 0;
                } else {
                    coords[0][i] =
                            50 * ((i - selectedIndex) * 1f / (toIndex - selectedIndex)) * progress;
                }
            }

            for (int i = 1; i < data.columns.length; i++) {
                paths[i - 1].reset();
                //if (lineDisabled[i - 1]) continue;
                float[] coord = coords[i];
                //Log.v("perc", "i=" + i + " coord=" + Arrays.toString(coord));
                for (int j = fromIndex; j <= selectedIndex; j++) {
                    if (j == fromIndex) {
                        paths[i - 1].moveTo(w * (columnX.value[j] - fromX) * 1f / (toX - fromX),
                                paddingTop + convertToY(h, coord[j]));
                    } else {
                        paths[i - 1].lineTo(w * (columnX.value[j] - fromX) * 1f / (toX - fromX),
                                paddingTop + convertToY(h, coord[j]));
                    }
                }

                coord = coords[i - 1];
                for (int j = selectedIndex; j >= fromIndex; j--) {
                    paths[i - 1].lineTo(w * (columnX.value[j] - fromX) * 1f / (toX - fromX),
                            paddingTop + convertToY(h, coord[j]));
                }
            }

            for (int k = 1; k < data.columns.length; k++) {
                p.setColor(data.columns[k].color);
                p.setAlpha((int) (255 * (1 - progress / 2f)));
                canvas.drawPath(paths[k - 1], p);
            }

            for (int i = selectedIndex; i < toIndex; i++) {
                long sum = 0;
                for (int j = 1; j < data.columns.length; j++) {
                    if (lineDisabled[j]) continue;

                    Data.Column column = data.columns[j];
                    float y = column.value[i];

                    sum += y;
                }

                float prevMinY = 0;
                for (int j = 1; j < data.columns.length; j++) {
                    if (lineDisabled[j]) {
                        float cy = prevMinY + (coords[j][selectedIndex] - prevMinY) * progress;
                        coords[j][i] = cy + (50 - cy) * ((i - selectedIndex) * 1f / (toIndex
                                - selectedIndex)) * progress;
                        continue;
                    }

                    Data.Column column = data.columns[j];
                    float y = column.value[i];

                    float percentage = y * 100f / sum;
                    prevMinY += percentage;
                    float cy = prevMinY + (coords[j][selectedIndex] - prevMinY) * progress;
                    coords[j][i] = cy + (50 - cy)
                            * ((i - selectedIndex) * 1f / (toIndex - selectedIndex))
                            * progress;
                }

                coords[0][i] =
                        50 * ((i - selectedIndex) * 1f / (toIndex - selectedIndex)) * progress;
            }

            for (int i = 1; i < data.columns.length; i++) {
                paths[i - 1].reset();
                //if (lineDisabled[i - 1]) continue;
                float[] coord = coords[i];
                //Log.v("perc", "i=" + i + " coord=" + Arrays.toString(coord));
                for (int j = selectedIndex; j < toIndex; j++) {
                    if (j == selectedIndex) {
                        paths[i - 1].moveTo(w * (columnX.value[j] - fromX) * 1f / (toX - fromX),
                                paddingTop + convertToY(h, coord[j]));
                    } else {
                        paths[i - 1].lineTo(w * (columnX.value[j] - fromX) * 1f / (toX - fromX),
                                paddingTop + convertToY(h, coord[j]));
                    }
                }

                coord = coords[i - 1];
                for (int j = toIndex - 1; j >= selectedIndex; j--) {
                    paths[i - 1].lineTo(w * (columnX.value[j] - fromX) * 1f / (toX - fromX),
                            paddingTop + convertToY(h, coord[j]));
                }
            }

            for (int k = 1; k < data.columns.length; k++) {
                p.setColor(data.columns[k].color);
                canvas.drawPath(paths[k - 1], p);
            }

            if (selectedIndex != -1) {
                float x = w * (data.columns[0].value[selectedIndex] - fromX) * 1f / (toX - fromX);
                canvas.drawLine(paddingStart + x, paddingTop + convertToY(h, maxY),
                        paddingStart + x, paddingTop + h, vertAxisP);
            }
        } else {
            for (int i = fromIndex; i <= selectedIndex; i++) {
                long sum = 0;
                for (int j = 1; j < data.columns.length; j++) {
                    if (lineDisabled[j]) continue;

                    Data.Column column = data.columns[j];
                    float y = column.value[i];

                    sum += y;
                }

                float prevMinY = 0;
                for (int j = 1; j < data.columns.length; j++) {
                    if (lineDisabled[j]) {
                        coords[j][i] = prevMinY;
                        continue;
                    }

                    Data.Column column = data.columns[j];
                    float y = column.value[i];

                    float percentage = y * 100f / sum;
                    prevMinY += percentage;
                    coords[j][i] = prevMinY;
                }
            }

            for (int i = 1; i < data.columns.length; i++) {
                paths[i - 1].reset();
                float[] coord = coords[i];
                for (int j = fromIndex; j <= selectedIndex; j++) {
                    if (j == fromIndex) {
                        paths[i - 1].moveTo(w * (columnX.value[j] - fromX) * 1f / (toX - fromX),
                                paddingTop + convertToY(h, coord[j]));
                    } else {
                        paths[i - 1].lineTo(w * (columnX.value[j] - fromX) * 1f / (toX - fromX),
                                paddingTop + convertToY(h, coord[j]));
                    }
                }

                coord = coords[i - 1];
                for (int j = selectedIndex; j >= fromIndex; j--) {
                    paths[i - 1].lineTo(w * (columnX.value[j] - fromX) * 1f / (toX - fromX),
                            paddingTop + convertToY(h, coord[j]));
                }
            }

            for (int k = 1; k < data.columns.length; k++) {
                p.setColor(data.columns[k].color);
                p.setAlpha((int) (255 * (1 - progress)));
                canvas.drawPath(paths[k - 1], p);
            }

            long sum = 0;
            for (int j = 1; j < data.columns.length; j++) {
                long colSum = 0;
                for (int i = fromIndex; i < toIndex; i++) {
                    if (lineDisabled[j]) continue;

                    Data.Column column = data.columns[j];
                    float y = column.value[i];
                    colSum += y;
                    sum += y;
                }
                sums[j - 1] = colSum;
            }

            long ex = columnX.value[toIndex - 1];
            float exc = w * (ex - fromX) * 1f / (toX - fromX);
            long sx = columnX.value[selectedIndex];
            float sxc = w * (sx - fromX) * 1f / (toX - fromX);

            float ey = coords[0][toIndex - 1];
            float eyc = convertToY(h, ey);
            float sy = coords[0][selectedIndex];
            float syc = convertToY(h, sy);

            float r = (float) Math.sqrt(Math.pow(syc - eyc, 2) + Math.pow(sxc - exc, 2));
            float cx = paddingStart + exc;
            float cy = paddingTop + eyc;

            float fromL = cx - r - Utils.dpToPx(14);
            float fromT = cy - r + Utils.dpToPx(10);
            float fromR = cx + r;
            float fromB = cy + r - Utils.dpToPx(10);

            w = getWidth() - paddingStart - paddingEnd - 2 * _24dp;
            float toL = paddingStart + _24dp;
            float toT = paddingTop + _24dp;
            float toR = paddingStart + _24dp + w;
            float toB = paddingTop + _24dp + w;

            oval.set(fromL + (toL - fromL) * (2 * progress - 1),
                    fromT + (toT - fromT) * (2 * progress - 1),
                    fromR + (toR - fromR) * (2 * progress - 1),
                    fromB + (toB - fromB) * (2 * progress - 1));

            float angle = (float) Math.toDegrees(Math.atan2(50, -(toIndex - 1 - selectedIndex))) * (
                    2
                            - 2 * progress);
            for (int k = 1; k < data.columns.length; k++) {
                p.setColor(data.columns[k].color);
                float columnAngle = angles[k - 1];
                float sweepAngle =
                        columnAngle + (sums[k - 1] * 360f / sum - columnAngle) * (2 * progress - 1);
                canvas.drawArc(oval, angle, sweepAngle, true, p);
                angle += sweepAngle;
            }

            angle = (float) Math.toDegrees(Math.atan2(50, -(toIndex - 1 - selectedIndex))) * (2
                    - 2 * progress);

            for (int k = 1; k < data.columns.length; k++) {
                if (lineDisabled[k]) continue;
                float columnAngle = angles[k - 1];
                float sweepAngle =
                        columnAngle + (sums[k - 1] * 360f / sum - columnAngle) * (2 * progress - 1);
                int value = Math.round(sums[k - 1] * 100f / sum);
                if (value == 0) continue;

                float x = (float) (oval.centerX() + oval.width() / 2.8f * Math.cos(
                        Math.toRadians(angle + sweepAngle / 2f)));
                float y = (float) (oval.centerY() + oval.width() / 2.8f * Math.sin(
                        Math.toRadians(angle + sweepAngle / 2f)));
                circleLabelP.setTextSize(Utils.dpToPx(Math.min(Math.max(value, 12), 40)));
                circleLabelP.setAlpha(Math.max(0, (int) (2550 * (progress - 0.9f))));
                float labelW = circleLabelP.measureText(String.valueOf(value) + "%");
                canvas.drawText(String.valueOf(value) + "%", x - labelW / 2,
                        y - circleLabelP.ascent() / 2, circleLabelP);
                angle += sweepAngle;
            }
        }
    }

    private void log() {
        Log.v("LineView", "fromIndex = "
                + fromIndex
                + ", toIndex = "
                + toIndex
                + ", maxY = "
                + maxY
                + ", step0 = "
                + step0);
    }

    public void setFromIndex(int from) {
        if (state == ANIMATING) return;
        fromIndex = from;
        log();
        invalidate();
    }

    public void setToIndex(int to) {
        if (state == ANIMATING) return;
        toIndex = to;
        log();
        invalidate();
    }

    interface Listener {
        void onPressed(long l);

        void onZoomOut();
    }
}
