package com.kuxurum.smoothlinechart;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.util.LongSparseArray;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.View;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class LineView extends View {
    private static int DATE_MARGIN = 20;
    private static int ANIMATION_DURATION = 200;
    private static int DATE_ANIMATION_DURATION = 150;
    private static int MAX_ALPHA = 255;
    private Data data;
    private Paint p;
    private Paint bp, bp2;
    private Paint textP;
    private Paint xTextP;
    private Paint axisP;
    private Paint vertAxisP;
    private Paint circleP;
    private Paint shadowP;
    private Paint dateLabelP;
    private Paint dataP;
    private Paint dataLabelP;
    private Path path = new Path();

    private int fromIndex;
    private int toIndex;
    private float drawX;

    private long minX, maxX;
    private long maxY, prevMaxY;
    private long fromX, toX;
    private long step0, step0Time;
    private float step0k, step0b;
    private boolean step0Down;
    private float sw = 0f;

    private SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d", Locale.US);
    private SimpleDateFormat labelDateFormat = new SimpleDateFormat("EEE, MMM d", Locale.US);
    private Date date = new Date();
    private LongSparseArray<Long> yToTime = new LongSparseArray<>();
    private LongSparseArray<Long> dateToTime = new LongSparseArray<>();
    private LongSparseArray<Boolean> dateToUp = new LongSparseArray<>();
    private List<Integer> dateIndices = new ArrayList<>();
    private SparseArray<Long> lineToTime = new SparseArray<>();
    private SparseArray<Boolean> lineToUp = new SparseArray<>();
    private boolean[] lineDisabled;
    private int selectedIndex = -1;

    float _24dp;
    private int axisColor, axisColor2;

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

    private void init() {
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);

        _24dp = Utils.dpToPx(24);
        axisColor = Color.parseColor("#f0f0f0");
        axisColor2 = Color.parseColor("#e8e8e8");

        setMinimumWidth(Utils.dpToPx(100));
        setMinimumHeight(Utils.dpToPx(100));

        p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(5f);

        bp = new Paint(Paint.ANTI_ALIAS_FLAG);
        bp.setColor(Color.WHITE);
        bp.setStyle(Paint.Style.FILL);

        bp2 = new Paint(Paint.ANTI_ALIAS_FLAG);
        bp2.setColor(Color.parseColor("#00000000"));
        bp2.setStyle(Paint.Style.FILL);

        axisP = new Paint(Paint.ANTI_ALIAS_FLAG);
        axisP.setStrokeWidth(Utils.dpToPx(1));

        vertAxisP = new Paint(Paint.ANTI_ALIAS_FLAG);
        vertAxisP.setColor(Color.parseColor("#e4e9ed"));
        vertAxisP.setStrokeWidth(Utils.dpToPx(1.5f));

        textP = new Paint(Paint.ANTI_ALIAS_FLAG);
        textP.setColor(Color.parseColor("#96a1a8"));
        textP.setTextSize(Utils.dpToPx(12));

        xTextP = new Paint(Paint.ANTI_ALIAS_FLAG);
        xTextP.setColor(Color.parseColor("#96a1a8"));
        xTextP.setTextSize(Utils.dpToPx(12));
        xTextP.setTextAlign(Paint.Align.CENTER);

        circleP = new Paint(Paint.ANTI_ALIAS_FLAG);
        circleP.setStyle(Paint.Style.FILL_AND_STROKE);
        circleP.setStrokeWidth(5f);

        shadowP = new Paint(Paint.ANTI_ALIAS_FLAG);
        shadowP.setShadowLayer(4, 0, 0, Color.parseColor("#40000000"));
        shadowP.setColor(Color.WHITE);
        shadowP.setStyle(Paint.Style.FILL);
        setLayerType(LAYER_TYPE_HARDWARE, shadowP);

        dateLabelP = new Paint(Paint.ANTI_ALIAS_FLAG);
        dateLabelP.setColor(Color.BLACK);
        dateLabelP.setTextSize(Utils.dpToPx(13));
        dateLabelP.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));

        dataP = new Paint(Paint.ANTI_ALIAS_FLAG);
        dataP.setTextSize(Utils.dpToPx(15));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            dataP.setLetterSpacing(-0.025f);
        }

        dataLabelP = new Paint(Paint.ANTI_ALIAS_FLAG);
        dataLabelP.setTextSize(Utils.dpToPx(11));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            dataLabelP.setLetterSpacing(-0.025f);
        }

        sw = xTextP.measureText(new SimpleDateFormat("MMM dd", Locale.US).format(0));

        setOnTouchListener(new OnTouchListener() {
            private float startY = 0f;

            @SuppressLint("ClickableViewAccessibility")
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int paddingStart = getPaddingLeft();
                int paddingEnd = getPaddingRight();

                int w = getWidth() - paddingStart - paddingEnd;
                boolean needToInvalidate = false;
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    startY = event.getY();
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
                if (event.getAction() == MotionEvent.ACTION_DOWN
                        || event.getAction() == MotionEvent.ACTION_MOVE) {
                    if (event.getX() < paddingStart || event.getX() > getWidth() - paddingEnd) {
                        return true;
                    }
                    if (Math.abs(event.getY() - startY) > Utils.dpToPx(20)) {
                        getParent().requestDisallowInterceptTouchEvent(false);
                    }
                    float v1 = (event.getX() - paddingStart) / w;
                    long x = (long) (fromX + v1 * (toX - fromX));
                    long[] columnX = data.columns[0].value;
                    int search = Arrays.binarySearch(columnX, x);
                    int round;
                    if (search < 0) {
                        round = -search - 1;
                    } else {
                        round = search;
                    }
                    int round1 = round - 1;
                    if (Math.abs(columnX[round] - x) > Math.abs(columnX[round1] - x)) {
                        round = round1;
                    }
                    drawX = event.getX();
                    int newIndex = Math.min(toIndex - 1, Math.max(round, fromIndex + 1));
                    if (selectedIndex != newIndex) needToInvalidate = true;
                    selectedIndex = newIndex;
                    //Log.v("LineView", "v1=" + v1 + " x=" + x + " index=" + selectedIndex);
                } else {
                    getParent().requestDisallowInterceptTouchEvent(false);
                    needToInvalidate = true;
                    selectedIndex = -1;
                }
                if (needToInvalidate) invalidate();
                return true;
            }
        });
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        long time = System.currentTimeMillis();
        Log.v("LineView", "====");
        Log.v("LineView", "thread=" + Thread.currentThread().getName() + " " + hashCode());

        int paddingStart = getPaddingLeft();
        int paddingEnd = getPaddingRight();
        int paddingTop = getPaddingTop();
        int paddingBottom = getPaddingBottom();

        int w = getWidth() - paddingStart - paddingEnd;
        int h = (int) (getHeight()
                - paddingBottom
                - paddingTop
                - xTextP.getTextSize()
                - Utils.dpToPx(6));

        canvas.drawRect(0, 0, getWidth(), getHeight(), bp2);
        canvas.drawRect(paddingStart, paddingTop, getWidth() - paddingEnd,
                getHeight() - paddingBottom, bp);

        canvas.clipRect(paddingStart, paddingTop, getWidth() - paddingEnd,
                getHeight() - paddingBottom);

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
                        (int) (1f * MAX_ALPHA / ANIMATION_DURATION * (time - yToTime.get(yKey))),
                        MAX_ALPHA);
            } else {
                alpha = Math.max(
                        (int) (1f * MAX_ALPHA / ANIMATION_DURATION * (yToTime.get(yKey) - time
                                + ANIMATION_DURATION)), 0);
            }
            //Log.v("LineView", "maxY=" + maxY + " y=" + yKey + ", alpha=" + alpha);
            axisP.setColor(axisColor);
            axisP.setAlpha(alpha);
            textP.setAlpha(alpha);
            for (int i = 1; i < 6; i++) {
                float y = convertToY(h, (long) (i * yKey / 5f));
                canvas.drawLine(paddingStart + _24dp, paddingTop + y,
                        getWidth() - paddingEnd - _24dp, paddingTop + y, axisP);
                canvas.drawText(String.valueOf(yKey / 5 * i), paddingStart + _24dp,
                        paddingTop + y - textP.descent() - Utils.dpToPx(3), textP);
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
            for (int i = 1; i < 6; i++) {
                axisP.setColor(axisColor);
                axisP.setAlpha(MAX_ALPHA);
                textP.setAlpha(MAX_ALPHA);
                float y = convertToY(h, (long) (i * maxY / 5f));
                canvas.drawLine(paddingStart + _24dp, paddingTop + y,
                        getWidth() - paddingEnd - _24dp, paddingTop + y, axisP);
                canvas.drawText(String.valueOf(maxY / 5 * i), paddingStart + _24dp,
                        paddingTop + y - textP.descent() - Utils.dpToPx(3), textP);
            }
        }

        {
            axisP.setColor(axisColor2);
            axisP.setAlpha(MAX_ALPHA);
            textP.setAlpha(MAX_ALPHA);
            float y = h - 1;
            canvas.drawLine(paddingStart + _24dp, paddingTop + y, getWidth() - paddingEnd - _24dp,
                    paddingTop + y, axisP);
            canvas.drawText("0", paddingStart + _24dp,
                    paddingTop + y - textP.descent() - Utils.dpToPx(3), textP);
        }

        Data.Column columnX = data.columns[0];
        if (selectedIndex != -1) {
            float x = w * (columnX.value[selectedIndex] - fromX) * 1f / (toX - fromX);

            //Log.v("LineView", "x=" + x);

            canvas.drawLine(paddingStart + x, paddingTop + Utils.dpToPx(5), paddingStart + x,
                    paddingTop + h, vertAxisP);
        }

        for (int i : dateIndices) {
            date.setTime(columnX.value[i]);
            String s = dateFormat.format(date);
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

        //Log.v("LineView", "size1=" + dateIndices.size());
        for (int j = 0; j < dateIndices.size() - 1; ) {
            //Log.v("LineView", "=====");
            //Log.v("LineView", "j=" + j);

            int index = dateIndices.get(j);
            float x = w * (columnX.value[index] - fromX) * 1f / (toX - fromX);

            int k;
            for (k = j + 1; k < dateIndices.size(); k++) {
                int mbIndex = dateIndices.get(k);
                Boolean up = dateToUp.get(mbIndex);
                if (up != null && !up) continue;
                float x2 = w * (columnX.value[mbIndex] - fromX) * 1f / (toX - fromX);

                //Log.v("LineView", "x2 " + x2 + " x=" + x);
                //Log.v("LineView", "x2 - x - sw=" + (x2 - x - sw));

                if (x2 - x - sw > DATE_MARGIN) break;
            }
            //Log.v("LineView", "k=" + k);

            for (int i = j + 1; i < k; i++) {
                //Log.v("LineView", "deleting " + i2 + " dateToTime.get(i2)=" + dateToTime.get(i2));
                int dateIndex = dateIndices.get(i);
                if (dateToTime.get(dateIndex) == null) {
                    //Log.v("LineView", "deleting " + i2);
                    dateToTime.put(dateIndex, time);
                    dateToUp.put(dateIndex, false);
                } else {
                    if (dateToUp.get(dateIndex)) {
                        Long l = dateToTime.get(dateIndex);
                        long l1 = time - l;
                        long value = l + 2 * l1 - DATE_ANIMATION_DURATION;
                        dateToTime.put(dateIndex, value);
                        dateToUp.put(dateIndex, false);
                    }
                }
            }

            j = k;
        }

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
                } else {
                    //Log.v("LineView", "already here, up?=" + dateToUp.get(midJ));
                    if (!dateToUp.get(midJ)) {
                        Long l = dateToTime.get(midJ);
                        long l1 = time - l;
                        long value = l + 2 * l1 - DATE_ANIMATION_DURATION;
                        dateIndices.add(midJ);
                        dateToTime.put(midJ, value);
                        dateToUp.put(midJ, true);
                    }
                }
            }
            //date.setTime(columnX.value[i]);
            //String s = dateFormat.format(date);
            //x = w * (columnX.value[i] - fromX) * 1f / (toX - fromX);
            //canvas.drawText(s, paddingStart + x, getHeight() - paddingBottom - utils.dpToPx(3),
            //        xTextP);
        }

        if (!dateIndices.isEmpty()) {
            int lastDate = dateIndices.get(size1 - 1);
            float x = w * (columnX.value[lastDate] - fromX) * 1f / (toX - fromX);
            if (w - x - sw / 2f > DATE_MARGIN + sw) {
                //Log.v("LineView", "w - x - sw / 2f=" + (w - x - sw / 2f));
                int lastX = columnX.value.length - 1;
                dateIndices.add(lastX);
                dateToTime.put(lastX, time);
                dateToUp.put(lastX, true);
            }
        }

        if (!dateIndices.isEmpty()) {
            int firstDate = dateIndices.get(0);
            float x = w * (columnX.value[firstDate] - fromX) * 1f / (toX - fromX);
            if (x - sw / 2f > DATE_MARGIN + sw) {
                //Log.v("LineView", "x - sw / 2f=" + (x - sw / 2f));
                dateIndices.add(0);
                dateToTime.put(0, time);
                dateToUp.put(0, true);
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

        for (int j = 1; j < data.columns.length; j++) {
            Data.Column column = data.columns[j];

            path.reset();

            //Log.v("LineView", "lineToTime.get(j)=" + lineToTime.get(j));
            p.setColor(column.color);
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
            } else if (lineDisabled[j]) {
                p.setAlpha(0);
            } else {
                p.setAlpha(MAX_ALPHA);
            }
            long time1 = System.nanoTime();
            for (int i = fromIndex; i < toIndex; i++) {
                float startX = w * (columnX.value[i] - fromX) * 1f / (toX - fromX);
                float startY = convertToY(h, column.value[i]);

                if (i == fromIndex) {
                    path.moveTo(paddingStart + startX, paddingTop + startY);
                } else {
                    path.lineTo(paddingStart + startX, paddingTop + startY);
                }
            }
            canvas.drawPath(path, p);
            Log.v("LineView", "time1=" + ((System.nanoTime() - time1) / 1_000_000f) + "ms");

            if (selectedIndex != -1) {
                float x = w * (columnX.value[selectedIndex] - fromX) * 1f / (toX - fromX);
                float y = convertToY(h, column.value[selectedIndex]);

                circleP.setColor(column.color);
                if (lineDisabled[j]) {
                    circleP.setAlpha(0);
                    bp.setAlpha(0);
                } else {
                    circleP.setAlpha(MAX_ALPHA);
                    bp.setAlpha(MAX_ALPHA);
                }

                canvas.drawCircle(paddingStart + x, paddingTop + y, 10, circleP);
                canvas.drawCircle(paddingStart + x, paddingTop + y, 8, bp);
            }
        }

        if (selectedIndex != -1) {
            float x = w * (columnX.value[selectedIndex] - fromX) * 1f / (toX - fromX);
            drawLabel(canvas, paddingStart + x, paddingTop + Utils.dpToPx(5),
                    paddingStart + Utils.dpToPx(5), getWidth() - paddingEnd - Utils.dpToPx(5),
                    selectedIndex);
        }

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
            step0 = (long) (maxY * 0.7f / 5f);
        }

        //canvas.drawLine(drawX, 0, drawX, h, axisP);

        Log.v("LineView", "time=" + (System.currentTimeMillis() - time) + "ms");

        if (lineToTime.size() != 0
                || dateToTime.size() != 0
                || yToTime.size() != 0
                || step0Time != 0L) {
            postInvalidateOnAnimation();
        }
    }

    private void drawLabel(Canvas canvas, float x0, float y0, float minX, float maxX, int index) {
        float w, h;
        float paddingStart = Utils.dpToPx(10);
        float paddingEnd = Utils.dpToPx(10);
        float paddingTop = Utils.dpToPx(5);
        float paddingBottom = Utils.dpToPx(5);
        float margin = Utils.dpToPx(8);
        float margin2 = Utils.dpToPx(0);
        float margin3 = Utils.dpToPx(16);

        Data.Column columnX = data.columns[0];
        long time = columnX.value[index];

        date.setTime(time);
        String dateText = labelDateFormat.format(date);
        float dateLabelW = dateLabelP.measureText(dateText);

        float dataW = -margin3;
        for (int i = 1; i < data.columns.length; i++) {
            if (lineDisabled[i]) continue;
            Data.Column column = data.columns[i];
            long value = column.value[index];
            float valueW = dataP.measureText(String.valueOf(value));
            float nameW = dataLabelP.measureText(column.name);

            dataW += Math.max(valueW, nameW) + margin3;
        }

        w = Math.max(dateLabelW, dataW) + paddingStart + paddingEnd;

        Paint.FontMetrics dateLabelPFM = dateLabelP.getFontMetrics();
        float dateLabelH = dateLabelPFM.descent - dateLabelPFM.ascent;

        Paint.FontMetrics dataPFM = dataP.getFontMetrics();
        float dataH = dataPFM.descent - dataPFM.ascent;

        Paint.FontMetrics dataLabelPFM = dataLabelP.getFontMetrics();
        float dataLabelH = dataLabelPFM.descent - dataLabelPFM.ascent;

        if (dataW > 0) {
            h = paddingTop + dateLabelH + margin + dataH + margin2 + dataLabelH + paddingBottom;
        } else {
            h = paddingTop + dateLabelH + paddingBottom;
        }

        float startX = Math.min(maxX - w, Math.max(minX, x0 - w / 6f));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            canvas.drawRoundRect(startX, y0, startX + w, y0 + h, 10, 10, shadowP);
        } else {
            canvas.drawRect(startX, y0, startX + w, y0 + h, shadowP);
        }

        canvas.drawText(dateText, startX + paddingStart, y0 + paddingTop - dateLabelPFM.ascent,
                dateLabelP);

        dataW = 0f;
        for (int i = 1; i < data.columns.length; i++) {
            if (lineDisabled[i]) continue;
            Data.Column column = data.columns[i];
            long value = column.value[index];
            float valueW = dataP.measureText(String.valueOf(value));
            float nameW = dataLabelP.measureText(column.name);

            dataP.setColor(column.color);
            dataLabelP.setColor(column.color);

            canvas.drawText(String.valueOf(value), startX + paddingStart + dataW,
                    y0 + paddingTop + dateLabelH + margin - dataPFM.ascent, dataP);
            canvas.drawText(column.name, startX + paddingStart + dataW,
                    y0 + paddingTop + dateLabelH + margin + dataH + margin2 - dataLabelPFM.ascent,
                    dataLabelP);

            dataW += Math.max(valueW, nameW) + margin3;
        }
    }

    private float convertToY(int h, long y) {
        if (maxY == 0f && step0 == 0f) return Float.POSITIVE_INFINITY;
        return h - h * y * 1f / (maxY + step0) - 1;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        Log.v("onMeasure w", MeasureSpec.toString(widthMeasureSpec));
        Log.v("onMeasure h", MeasureSpec.toString(heightMeasureSpec));
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        int paddingStart = getPaddingLeft();
        int paddingEnd = getPaddingRight();
        int paddingTop = getPaddingTop();
        int paddingBottom = getPaddingBottom();

        w = w - paddingStart - paddingEnd;
        h = h - paddingBottom - paddingTop;

        Log.v("LineView", "w=" + w + " h=" + h + " ow=" + oldw + " oh=" + oldh);

        long start = System.currentTimeMillis();
        Data.Column columnX = data.columns[0];

        float lastCenterX = 0f;
        for (int i = toIndex - 1; i >= fromIndex; i--) {
            date.setTime(columnX.value[i]);
            float x = w * (columnX.value[i] - fromX) * 1f / (toX - fromX);
            if (lastCenterX == 0f) {
                if (w - x - sw / 2f > 0) {
                    Log.v("LineView", "x=" + x);
                    lastCenterX = x;
                    dateIndices.add(i);
                }
            } else {
                if (lastCenterX - x - sw > DATE_MARGIN) {
                    Log.v("LineView", "x=" + x);
                    lastCenterX = x;
                    dateIndices.add(i);
                }
            }
        }
        Collections.sort(dateIndices);
        Log.v("LineView", "time=" + (System.currentTimeMillis() - start) + "ms");
    }

    public void setData(Data data) {
        this.data = data;
        lineDisabled = new boolean[data.columns.length];

        Data.Column columnX = data.columns[0];
        minX = columnX.value[0];
        maxX = columnX.value[columnX.value.length - 1];
        setFrom(0f);
        setTo(1f);

        invalidate();
    }

    public void setFrom(float from) {
        fromX = (long) (minX + from * (maxX - minX));
        if (fromX > toX) return;

        fromIndex = Arrays.binarySearch(data.columns[0].value, fromX);
        if (fromIndex < 0) fromIndex = Math.max(-fromIndex - 2, 0);

        calculateMaxY();
        log();

        invalidate();
    }

    public void setTo(float to) {
        toX = (long) (minX + to * (maxX - minX));
        if (fromX > toX) return;

        Data.Column column = data.columns[0];
        toIndex = Arrays.binarySearch(column.value, toX);
        if (toIndex < 0) toIndex = Math.min(-toIndex, column.value.length);

        calculateMaxY();
        log();

        invalidate();
    }

    private void calculateMaxY() {
        long maxY = 0;
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
        } else {
            maxY = 0;
        }

        if (prevMaxY != maxY) {
            long time = System.currentTimeMillis();
            LineView.this.maxY = maxY;

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
                prev = (int) (prevMaxY * 0.7f / 5f + prevMaxY - maxY);
            } else {
                prev = (int) (prevMaxY + step0 - maxY);
            }

            int next = (int) (maxY * 0.7f / 5f);

            step0Time = time;
            step0k = (next - prev) * 1f / ANIMATION_DURATION;
            step0b = prev;
            step0Down = next > prev;

            prevMaxY = maxY;
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
        calculateMaxY();
        log();
        invalidate();
    }
}
