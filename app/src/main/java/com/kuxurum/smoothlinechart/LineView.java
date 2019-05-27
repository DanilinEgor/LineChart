package com.kuxurum.smoothlinechart;

import android.animation.Animator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Build;
import android.util.AttributeSet;
import android.util.LongSparseArray;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.View;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class LineView extends BaseLineView {
    private static int DATE_MARGIN = 20;
    private static int ANIMATION_DURATION = 200;
    private static int DATE_ANIMATION_DURATION = 150;
    private static int MAX_AXIS_TEXT_ALPHA = 255;
    private static int MAX_AXIS_ALPHA = 25;
    private static int MAX_ALPHA = 255;
    private Data data;
    private Paint circleP;
    private Paint bgP;
    private float[] points;

    private int fromIndex;
    private int toIndex;

    private long minX, maxX;
    private long maxY;
    private long minY;
    private long fromX, toX;

    private long step0, step0Time;
    private float step0k, step0b;
    private boolean step0Down;
    private long step1, step1Time;
    private float step1k, step1b;
    private boolean step1Down;

    private float sw = 0f;

    private LongSparseArray<Long> yToTime = new LongSparseArray<>();
    private LongSparseArray<Long> dateToTime = new LongSparseArray<>();
    private LongSparseArray<Boolean> dateToUp = new LongSparseArray<>();
    private List<Integer> dateIndices = new ArrayList<>();
    private SparseArray<Long> lineToTime = new SparseArray<>();
    private SparseArray<Boolean> lineToUp = new SparseArray<>();
    private boolean[] lineDisabled;
    private int selectedIndex = -1;

    int _24dp;

    private int maxIndex;
    private int d;

    Listener listener;

    private static final int WHOLE = 0;
    private static final int ANIMATING = 1;
    private static final int DETAILED = 2;
    int state = WHOLE;

    private Data oldData;
    private long oldFromX, oldToX;
    private int oldFromIndex, oldToIndex;

    public LineView(Context context) {
        super(context);
        init();
    }

    public LineView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public LineView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    void init() {
        super.init();
        setPadding(0, Utils.dpToPx(16), 0, 0);

        _24dp = Utils.dpToPx(24);

        p.setStrokeWidth(5f);
        p.setStrokeCap(Paint.Cap.SQUARE);

        bgP = new Paint(Paint.ANTI_ALIAS_FLAG);
        bgP.setStyle(Paint.Style.FILL);

        circleP = new Paint(Paint.ANTI_ALIAS_FLAG);
        circleP.setStyle(Paint.Style.FILL_AND_STROKE);
        circleP.setStrokeWidth(5f);

        sw = xTextP.measureText(new SimpleDateFormat("MMM dd", Locale.US).format(0));

        setOnTouchListener(new OnTouchListener() {
            private float startY = 0f;
            boolean startOnZoomOut = false;

            @SuppressLint("ClickableViewAccessibility")
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int paddingStart = getPaddingLeft();
                int paddingEnd = getPaddingRight();

                int w = getWidth() - paddingStart - paddingEnd;
                boolean needToInvalidate = false;
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    startY = event.getY();
                    touchStart = System.currentTimeMillis();

                    if (state == DETAILED && zoomOutRectF.contains(event.getX(), event.getY())) {
                        startOnZoomOut = true;
                        return true;
                    }

                    labelWasShown = labelShown;
                    labelPressed = labelRectF.contains(event.getX(), event.getY());
                    needToInvalidate = true;
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
                if (event.getAction() == MotionEvent.ACTION_DOWN
                        || event.getAction() == MotionEvent.ACTION_MOVE) {
                    if (event.getX() < paddingStart || event.getX() > getWidth() - paddingEnd) {
                        return true;
                    }

                    if (labelWasShown && System.currentTimeMillis() - touchStart < TAP_TIMEOUT) {
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
                        round = Math.min(columnX.length - 1, -search - 1);
                    } else {
                        round = Math.min(columnX.length - 1, search);
                    }
                    int round1 = Math.max(round - 1, 0);
                    if (Math.abs(columnX[round] - x) > Math.abs(columnX[round1] - x)) {
                        round = round1;
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
                        if (state == WHOLE && listener != null) {
                            listener.onPressed(data.columns[0].value[selectedIndex]);
                        }
                        selectedIndex = -1;
                        labelPressed = false;
                        labelShown = false;
                        labelWasShown = false;
                        labelRectF.set(0, 0, 0, 0);
                    } else {
                        if (state == DETAILED
                                && event.getAction() == MotionEvent.ACTION_UP
                                && zoomOutRectF.contains(event.getX(), event.getY())
                                && startOnZoomOut) {
                            listener.onZoomOut();
                            return true;
                        }

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
            }
        });
    }

    void setChartBackgroundColor(int color) {
        bgP.setColor(color);
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

        int w = getWidth() - paddingStart - paddingEnd;
        Paint.FontMetrics titlePFM = titleP.getFontMetrics();
        float titleH = titlePFM.descent - titlePFM.ascent;
        int h = (int) (getHeight()
                - paddingBottom
                - paddingTop
                - titleH
                - 2 * titleMargin
                - xTextP.getTextSize()
                - Utils.dpToPx(6));

        if (data.columns.length == 0) return;

        if (step0Time != 0L) {
            step0 = (long) (step0k * (time - step0Time) + step0b);
            if (step0Down) {
                step0 = Math.min(step0, (long) ((maxY - minY) * 0.2f / 5f));
            } else {
                step0 = Math.max(step0, (long) ((maxY - minY) * 0.2f / 5f));
            }
            //Log.v("LineView", "maxY=" + maxY + " step0=" + step0);
        }

        if (step1Time != 0L) {
            step1 = (long) (step1k * (time - step1Time) + step1b);
            if (step1Down) {
                step1 = Math.min(step1, 0);
            } else {
                step1 = Math.max(step1, 0);
            }
            //Log.v("LineView", "maxY=" + maxY + " step1=" + step1);
        }

        if (state == WHOLE) {
            titleP.setColor(titleColor);
            canvas.drawText("Followers", paddingStart + _24dp, paddingTop - titlePFM.ascent,
                    titleP);
        } else if (state == DETAILED) {
            titleP.setColor(zoomOutColor);
            zoomOutRectF.set(paddingStart + _24dp, paddingTop,
                    paddingStart + _24dp + titleP.measureText("Zoom out"), paddingTop + titleH);
            canvas.drawText("Zoom out", paddingStart + _24dp, paddingTop - titlePFM.ascent, titleP);
        }

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

        canvas.save();
        canvas.translate(0, titleH + 2 * titleMargin);

        // draw axis
        int size = yToTime.size();
        boolean maxWasDrawn = false;
        for (int j = 0; j < size; j++) {
            long yKey = yToTime.keyAt(j);
            if (yKey == 0) continue;
            int alpha;
            int textAlpha;
            if (yKey == maxY) {
                maxWasDrawn = true;
                alpha = Math.min(
                        (int) (1f * MAX_AXIS_ALPHA / ANIMATION_DURATION * (time - yToTime.get(
                                yKey))), MAX_AXIS_ALPHA);
                textAlpha = Math.min(
                        (int) (1f * MAX_AXIS_TEXT_ALPHA / ANIMATION_DURATION * (time - yToTime.get(
                                yKey))), MAX_AXIS_TEXT_ALPHA);
            } else {
                alpha = Math.max(
                        (int) (1f * MAX_AXIS_ALPHA / ANIMATION_DURATION * (yToTime.get(yKey) - time
                                + ANIMATION_DURATION)), 0);
                textAlpha = Math.max(
                        (int) (1f * MAX_AXIS_TEXT_ALPHA / ANIMATION_DURATION * (yToTime.get(yKey)
                                - time + ANIMATION_DURATION)), 0);
            }
            //Log.v("LineView", "maxY=" + maxY + " y=" + yKey + ", alpha=" + alpha);
            axisP.setColor(axisColor);
            axisP.setAlpha(alpha);
            axisTextP.setAlpha(textAlpha);
            getAxisTexts(yKey, minY);
            for (int i = 0; i < 6; i++) {
                float y = convertToY(h, minY + i * (yKey - minY) / 5f);
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
            getAxisTexts(maxY, minY);
            for (int i = 0; i < 6; i++) {
                axisP.setColor(axisColor);
                axisP.setAlpha(MAX_AXIS_ALPHA);
                axisTextP.setAlpha(MAX_AXIS_TEXT_ALPHA);
                float y = convertToY(h, minY + i * (maxY - minY) / 5f);
                canvas.drawLine(paddingStart + _24dp, paddingTop + y,
                        getWidth() - paddingEnd - _24dp, paddingTop + y, axisP);
                canvas.drawText(axisTexts[i], paddingStart + _24dp,
                        paddingTop + y - axisTextP.descent() - Utils.dpToPx(3), axisTextP);
            }
        }

        {
            //axisP.setColor(axisColorDark);
            //axisP.setAlpha(MAX_AXIS_ALPHA);
            //axisTextP.setAlpha(MAX_AXIS_TEXT_ALPHA);
            //float y = h - 1;
            //canvas.drawLine(paddingStart + _24dp, paddingTop + y, getWidth() - paddingEnd - _24dp,
            //        paddingTop + y, axisP);
            //canvas.drawText(axisTexts[0], paddingStart + _24dp,
            //        paddingTop + y - axisTextP.descent() - Utils.dpToPx(3), axisTextP);
        }

        Data.Column columnX = data.columns[0];
        if (selectedIndex != -1) {
            float x = w * (columnX.value[selectedIndex] - fromX) * 1f / (toX - fromX);

            //Log.v("LineView", "x=" + x);

            canvas.drawLine(paddingStart + x,
                    Math.max(paddingTop + Utils.dpToPx(5), convertToY(h, maxY) - Utils.dpToPx(20)),
                    paddingStart + x, paddingTop + h, vertAxisP);
        }

        canvas.restore();
        for (int i : dateIndices) {
            String s = "";
            if (state == WHOLE) {
                s = formatDate(columnX.value[i]);
            } else if (state == DETAILED) {
                s = formatTime(columnX.value[i]);
            }
            float x = w * (columnX.value[i] - fromX) * 1f / (toX - fromX);

            int alpha;
            Boolean up = dateToUp.get(i);
            if (up == null) {
                alpha = 255;
            } else if (up) {
                alpha = Math.min(
                        (int) (1f * MAX_ALPHA / DATE_ANIMATION_DURATION * (time - dateToTime.get(
                                i))), MAX_ALPHA);
            } else {
                alpha = Math.max(
                        (int) (1f * MAX_ALPHA / DATE_ANIMATION_DURATION * (dateToTime.get(i) - time
                                + DATE_ANIMATION_DURATION)), 0);
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

        canvas.translate(0, titleH + 2 * titleMargin);
        drawLines(canvas, time, fromIndex, toIndex);

        for (int j = 1; j < data.columns.length; j++) {
            Long lineTime = lineToTime.get(j);
            if (lineTime == null) continue;
            if (time - lineTime > ANIMATION_DURATION) {
                lineDisabled[j] = !lineToUp.get(j);
                lineToUp.remove(j);
                lineToTime.remove(j);
            }
        }

        if (time - step0Time > ANIMATION_DURATION) {
            step0Time = 0L;
            step0 = (long) ((maxY - minY) * 0.2f / 5f);
        }

        if (time - step1Time > ANIMATION_DURATION) {
            step1Time = 0L;
            step1 = 0;
        }

        //canvas.drawLine(drawX, 0, drawX, h, axisP);

        //Log.v("LineView", "time=" + (System.currentTimeMillis() - time) + "ms");

        if (lineToTime.size() != 0
                || dateToTime.size() != 0
                || yToTime.size() != 0
                || step0Time != 0L
                || step1Time != 0L) {
            postInvalidateOnAnimation();
        }
    }

    private void drawLines(Canvas canvas, long time, int fromIndex, int toIndex) {
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
                - 2 * titleMargin
                - xTextP.getTextSize()
                - Utils.dpToPx(6));

        Data.Column columnX = data.columns[0];
        float maxSelectedY = Float.MAX_VALUE;
        for (int j = 1; j < data.columns.length; j++) {
            if (lineDisabled[j]) continue;

            Data.Column column = data.columns[j];

            //Log.v("LineView", "lineToTime.get(j)=" + lineToTime.get(j));
            p.setColor(column.color);
            circleP.setColor(column.color);
            if (lineToTime.get(j) != null) {
                int alpha;
                if (lineToUp.get(j)) {
                    alpha = Math.min(
                            (int) (1f * MAX_ALPHA / ANIMATION_DURATION * (time - lineToTime.get(
                                    j))), MAX_ALPHA);
                } else {
                    alpha = Math.max(
                            (int) (1f * MAX_ALPHA / ANIMATION_DURATION * (lineToTime.get(j) - time
                                    + ANIMATION_DURATION)), 0);
                }
                p.setAlpha(alpha);
            } else {
                if (p.getAlpha() != MAX_ALPHA) p.setAlpha(MAX_ALPHA);
            }

            for (int i = fromIndex; i < toIndex; i++) {
                float startX = w * (columnX.value[i] - fromX) * 1f / (toX - fromX);
                float startY = paddingTop + convertToY(h, column.value[i]);
                if (i == fromIndex) {
                    points[4 * i] = startX;
                    points[4 * i + 1] = startY;
                } else if (i == toIndex - 1) {
                    points[4 * i - 2] = startX;
                    points[4 * i - 1] = startY;
                } else {
                    points[4 * i - 2] = startX;
                    points[4 * i - 1] = startY;
                    points[4 * i] = startX;
                    points[4 * i + 1] = startY;
                }
            }
            canvas.drawLines(points, 4 * fromIndex, (toIndex - fromIndex - 1) * 4, p);

            if (selectedIndex != -1) {
                float x = paddingStart + w * (columnX.value[selectedIndex] - fromX) * 1f / (toX
                        - fromX);
                float y = paddingTop + convertToY(h, column.value[selectedIndex]);
                maxSelectedY = Math.min(maxSelectedY, y);

                circleP.setColor(column.color);
                if (lineDisabled[j]) {
                    circleP.setAlpha(0);
                    bgP.setAlpha(0);
                } else {
                    circleP.setAlpha(MAX_ALPHA);
                    bgP.setAlpha(MAX_ALPHA);
                }

                canvas.drawCircle(x, y, Utils.dpToPx(4), circleP);
                canvas.drawCircle(x, y, Utils.dpToPx(3), bgP);
            }
        }

        if (selectedIndex != -1) {
            float x = w * (columnX.value[selectedIndex] - fromX) * 1f / (toX - fromX);
            int minX = paddingStart + Utils.dpToPx(5);
            int maxX = getWidth() - paddingEnd - Utils.dpToPx(5);
            drawLabel(canvas,
                    Math.max(paddingTop + Utils.dpToPx(5), convertToY(h, maxY) - Utils.dpToPx(20)),
                    x, selectedIndex);
        }
    }

    private void drawLabel(Canvas canvas, float y0, float selectedX, int index) {
        float w, h;
        float paddingStart = Utils.dpToPx(10);
        float paddingEnd = Utils.dpToPx(10);
        float paddingTop = Utils.dpToPx(10);
        float paddingBottom = Utils.dpToPx(10);

        Data.Column columnX = data.columns[0];
        long time = columnX.value[index];

        String dateText;
        if (state == WHOLE) {
            dateText = labelDateFormat.format(date);
        } else {
            dateText = detailedLabelDateFormat.format(date);
        }
        float dateW = dateLabelP.measureText(dateText);

        float maxLineNameW = 0;
        float maxLineValueW = 0;
        long sum = 0;
        for (int i = 1; i < data.columns.length; i++) {
            if (lineDisabled[i]) continue;
            Data.Column column = data.columns[i];
            long value = column.value[index];
            float valueW = dataValueP.measureText(formatForLabel(value));
            maxLineValueW = Math.max(maxLineValueW, valueW);

            float nameW = dataNameP.measureText(column.name);
            maxLineNameW = Math.max(maxLineNameW, nameW);

            sum += value;
        }
        maxLineValueW = Math.max(maxLineValueW, dataValueP.measureText(formatForLabel(sum)));

        int minW = Utils.dpToPx(160);
        int minMargin = Utils.dpToPx(16);
        w = Math.max(minW, Math.max(dateW, maxLineNameW + minMargin + maxLineValueW)
                + paddingStart
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

        float startX = selectedX - w - Utils.dpToPx(4);
        if (startX < Utils.dpToPx(4)) {
            startX = selectedX + Utils.dpToPx(4);
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

        float currentH = dateLabelH + marginBetweenLines;
        for (int i = 1; i < data.columns.length; i++) {
            if (lineDisabled[i]) continue;
            Data.Column column = data.columns[i];
            long value = column.value[index];
            String vt = formatForLabel(value);
            float valueW = dataValueP.measureText(vt);

            dataValueP.setColor(column.color);

            canvas.drawText(column.name, startX + paddingStart,
                    y0 + paddingTop + currentH - dataPFM.ascent, dataNameP);
            canvas.drawText(vt, startX + w - paddingEnd - valueW,
                    y0 + paddingTop + currentH - dataLabelPFM.ascent, dataValueP);

            currentH += Math.max(dataH, dataLabelH) + marginBetweenLines;
        }

        if (state == WHOLE) {
            drawArrow(canvas, y0 + paddingTop, startX + w - paddingEnd - Utils.dpToPx(4));
        }
    }

    private float convertToY(int h, float y) {
        if (maxY == 0f && step0 == 0f) return Float.POSITIVE_INFINITY;
        return h - h * (y - minY - step1) * 1f / (maxY - minY + step0 - step1);
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

    public void setData(Data data) {
        setData(data, 0f, 1f, true);
    }

    public void setData(Data data, float from, float to, boolean invalidate) {
        fromIndex = 0;
        toIndex = 0;
        this.data = data;
        lineDisabled = new boolean[data.columns.length];

        Data.Column columnX = data.columns[0];
        points = new float[(columnX.value.length - 1) * 4];
        minX = columnX.value[0];
        maxX = columnX.value[columnX.value.length - 1];
        maxIndex = columnX.value.length - 1;
        setFrom(from, invalidate);
        setTo(to, invalidate);

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

        if (invalidate) invalidate();
    }

    public void setFrom(float from) {
        if (state == ANIMATING) return;
        setFrom(from, true);
    }

    public void setFrom(float from, boolean invalidate) {
        labelPressed = false;
        labelShown = false;
        labelWasShown = false;
        selectedIndex = -1;
        labelRectF.set(0, 0, 0, 0);

        fromX = (long) (minX + from * (maxX - minX));
        if (fromX > toX) return;

        fromIndex = Arrays.binarySearch(data.columns[0].value, fromX);
        if (fromIndex < 0) fromIndex = Math.max(-fromIndex - 2, 0);
        if (toIndex - fromIndex == 1) --fromIndex;

        calculateMinMaxY();
        log();

        if (invalidate) invalidate();
    }

    public void setTo(float to) {
        if (state == ANIMATING) return;
        setTo(to, true);
    }

    public void setTo(float to, boolean invalidate) {
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

        calculateMinMaxY();
        log();

        if (invalidate) invalidate();
    }

    private void calculateMinMaxY() {
        long time = System.currentTimeMillis();

        long prevMinY = minY;
        minY = Long.MAX_VALUE;
        if (toIndex <= fromIndex) minY = 0;
        for (int i = 1; i < data.columns.length; i++) {
            if (lineDisabled[i] || lineToTime.get(i) != null && !lineToUp.get(i)) continue;
            long[] y = data.columns[i].value;
            for (int j = fromIndex; j < toIndex; ++j) {
                minY = Math.min(minY, y[j]);
            }
        }

        if (minY >= 133) {
            long min = (long) (minY * 10f / 11f);
            long max = (long) (minY * 50f / 51f);

            int pow = 10;
            while (true) {
                long prevMax = max;
                max = max - max % pow;
                if (max < min) {
                    minY = prevMax;
                    break;
                }
                pow *= 10;
            }
        } else if (minY > 0) {
            minY = minY - minY % 10 + 10;
        }

        if (prevMinY != minY) {
            int prev;
            if (step1Time == 0L) {
                prev = (int) (prevMinY - minY);
            } else {
                prev = (int) (prevMinY + step1 - minY);
            }

            int next = 0;

            step1Time = time;
            step1k = (next - prev) * 1f / ANIMATION_DURATION;
            step1b = prev;
            step1Down = next > prev;
        }

        long prevMaxY = maxY;
        maxY = 0;
        for (int i = 1; i < data.columns.length; i++) {
            if (lineDisabled[i] || lineToTime.get(i) != null && !lineToUp.get(i)) continue;
            long[] y = data.columns[i].value;
            for (int j = fromIndex; j < toIndex; ++j) {
                maxY = Math.max(maxY, y[j]);
            }
        }

        if (maxY >= 133) {
            long min = (long) (maxY * 10f / 11f);
            long max = (long) (maxY * 50f / 51f);

            int pow = 10;
            while (true) {
                long prevMax = max;
                max = max - max % pow;
                if (max < min) {
                    maxY = prevMax;
                    break;
                }
                pow *= 10;
            }
        } else if (maxY > 0) {
            maxY = maxY - maxY % 10 + 10;
        }

        if (prevMaxY != maxY) {
            boolean maxHandled = false;
            boolean prevMaxHandled = false;
            for (int j = 0; j < yToTime.size(); j++) {
                long yKey = yToTime.keyAt(j);
                if (yKey != maxY && yKey != prevMaxY) continue;

                if (yKey == maxY) {
                    maxHandled = true;
                }

                if (yKey == prevMaxY) {
                    prevMaxHandled = true;
                }

                long value = time - (yToTime.get(yKey) + ANIMATION_DURATION - time);
                yToTime.put(yKey, value);
            }

            if (!prevMaxHandled) {
                yToTime.put(prevMaxY, time);
            }

            if (!maxHandled) {
                yToTime.put(maxY, time);
            }

            int prev;
            if (step0Time == 0L) {
                prev = (int) ((prevMaxY - prevMinY) * 0.2f / 5f + prevMaxY - maxY);
            } else {
                prev = (int) (prevMaxY + step0 - maxY);
            }

            int next = (int) ((maxY - minY) * 0.2f / 5f);

            step0Time = time;
            step0k = (next - prev) * 1f / ANIMATION_DURATION;
            step0b = prev;
            step0Down = next > prev;
        }
    }

    public void setLineEnabled(int index, boolean checked) {
        long time = System.currentTimeMillis();
        if (lineToTime.get(index) == null) {
            lineToTime.put(index, time);
        } else {
            long value = time - (lineToTime.get(index) + ANIMATION_DURATION - time);
            lineToTime.put(index, value);
        }
        lineToUp.put(index, checked);
        if (checked) lineDisabled[index] = false;
        calculateMinMaxY();
        log();
        invalidate();
    }

    private void log() {
        //Log.v("LineView", "fromIndex = "
        //        + fromIndex
        //        + ", toIndex = "
        //        + toIndex
        //        + ", maxY = "
        //        + maxY
        //        + ", step0 = "
        //        + step0);
    }

    public void setDetailed(Data newData) {
        state = ANIMATING;
        oldFromX = fromX;
        oldFromIndex = fromIndex;
        oldToX = toX;
        oldToIndex = toIndex;

        oldData = data;
        setData(newData, 0f, 1f, false);
        yToTime.clear();

        maxY = 0;
        for (int i = 1; i < data.columns.length; i++) {
            if (lineDisabled[i] || lineToTime.get(i) != null && !lineToUp.get(i)) continue;
            long[] y = data.columns[i].value;
            for (int j = fromIndex; j < toIndex; ++j) {
                maxY = Math.max(maxY, y[j]);
            }
        }

        if (maxY >= 133) {
            long min = (long) (maxY * 10f / 11f);
            long max = (long) (maxY * 50f / 51f);

            int pow = 10;
            while (true) {
                long prevMax = max;
                max = max - max % pow;
                if (max < min) {
                    maxY = prevMax;
                    break;
                }
                pow *= 10;
            }
        } else if (maxY > 0) {
            maxY = maxY - maxY % 10 + 10;
        }

        minY = Long.MAX_VALUE;
        if (toIndex <= fromIndex) minY = 0;
        for (int i = 1; i < data.columns.length; i++) {
            if (lineDisabled[i] || lineToTime.get(i) != null && !lineToUp.get(i)) continue;
            long[] y = data.columns[i].value;
            for (int j = fromIndex; j < toIndex; ++j) {
                minY = Math.min(minY, y[j]);
            }
        }

        if (minY >= 133) {
            long min = (long) (minY * 10f / 11f);
            long max = (long) (minY * 50f / 51f);

            int pow = 10;
            while (true) {
                long prevMax = max;
                max = max - max % pow;
                if (max < min) {
                    minY = prevMax;
                    break;
                }
                pow *= 10;
            }
        } else if (minY > 0) {
            minY = minY - minY % 10 + 10;
        }

        step0Time = 0;
        step0 = (long) (maxY * 0.7f / 5f);
        step1Time = 0;
        step1 = 0;

        invalidate();


        PropertyValuesHolder fromProp = PropertyValuesHolder.ofFloat("from", 0, 3f/7);
        PropertyValuesHolder toProp = PropertyValuesHolder.ofFloat("to", 1, 4f/7);
        ValueAnimator valueAnimator = ValueAnimator.ofPropertyValuesHolder(fromProp, toProp);
        valueAnimator.setDuration(ANIMATION_DURATION);
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                setFrom((float) animation.getAnimatedValue("from"), false);
                setTo((float) animation.getAnimatedValue("to"), false);

                //fromX = ((Float) animation.getAnimatedValue("from")).longValue();
                //toX = ((Float) animation.getAnimatedValue("to")).longValue();
                //
                //fromIndex = Arrays.binarySearch(data.columns[0].value, fromX);
                //if (fromIndex < 0) fromIndex = Math.max(-fromIndex - 2, 0);
                //toIndex = Arrays.binarySearch(data.columns[0].value, toX);
                //if (toIndex < 0) toIndex = Math.min(-toIndex, data.columns[0].value.length);

                invalidate();
            }
        });
        valueAnimator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                state = DETAILED;
                //setFrom(3f / 7);
                //setTo(4f / 7);
                invalidate();
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        valueAnimator.start();
    }

    public void setWhole() {
        state = ANIMATING;

        long minX = oldData.columns[0].value[0];
        long maxX = oldData.columns[0].value[oldData.columns[0].value.length - 1];

        float from = (oldFromX - minX) * 1f / (maxX - minX);
        float to = (oldToX - minX) * 1f / (maxX - minX);

        setData(oldData, from, to, false);

        fromX = oldFromX;
        fromIndex = oldFromIndex;
        toX = oldToX;
        toIndex = oldToIndex;
        yToTime.clear();

        maxY = 0;
        for (int i = 1; i < data.columns.length; i++) {
            if (lineDisabled[i] || lineToTime.get(i) != null && !lineToUp.get(i)) continue;
            long[] y = data.columns[i].value;
            for (int j = fromIndex; j < toIndex; ++j) {
                maxY = Math.max(maxY, y[j]);
            }
        }

        if (maxY >= 133) {
            long min = (long) (maxY * 10f / 11f);
            long max = (long) (maxY * 50f / 51f);

            int pow = 10;
            while (true) {
                long prevMax = max;
                max = max - max % pow;
                if (max < min) {
                    maxY = prevMax;
                    break;
                }
                pow *= 10;
            }
        } else if (maxY > 0) {
            maxY = maxY - maxY % 10 + 10;
        }

        minY = Long.MAX_VALUE;
        if (toIndex <= fromIndex) minY = 0;
        for (int i = 1; i < data.columns.length; i++) {
            if (lineDisabled[i] || lineToTime.get(i) != null && !lineToUp.get(i)) continue;
            long[] y = data.columns[i].value;
            for (int j = fromIndex; j < toIndex; ++j) {
                minY = Math.min(minY, y[j]);
            }
        }

        if (minY >= 133) {
            long min = (long) (minY * 10f / 11f);
            long max = (long) (minY * 50f / 51f);

            int pow = 10;
            while (true) {
                long prevMax = max;
                max = max - max % pow;
                if (max < min) {
                    minY = prevMax;
                    break;
                }
                pow *= 10;
            }
        } else if (minY > 0) {
            minY = minY - minY % 10 + 10;
        }

        step0Time = 0;
        step0 = (long) (maxY * 0.7f / 5f);
        step1Time = 0;
        step1 = 0;

        PropertyValuesHolder fromProp = PropertyValuesHolder.ofFloat("from", 0, from);
        PropertyValuesHolder toProp = PropertyValuesHolder.ofFloat("to", 1, to);
        ValueAnimator valueAnimator = ValueAnimator.ofPropertyValuesHolder(fromProp, toProp);
        valueAnimator.setDuration(ANIMATION_DURATION);
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                setFrom((float) animation.getAnimatedValue("from"), false);
                setTo((float) animation.getAnimatedValue("to"), false);

                //fromX = ((Float) animation.getAnimatedValue("from")).longValue();
                //toX = ((Float) animation.getAnimatedValue("to")).longValue();
                //
                //fromIndex = Arrays.binarySearch(data.columns[0].value, fromX);
                //if (fromIndex < 0) fromIndex = Math.max(-fromIndex - 2, 0);
                //toIndex = Arrays.binarySearch(data.columns[0].value, toX);
                //if (toIndex < 0) toIndex = Math.min(-toIndex, data.columns[0].value.length);

                invalidate();
            }
        });
        valueAnimator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                state = WHOLE;
                //setFrom(3f / 7);
                //setTo(4f / 7);
                invalidate();
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        valueAnimator.start();
    }

    interface Listener {
        void onPressed(long l);

        void onZoomOut();
    }
}
