package com.kuxurum.smoothlinechart;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.View;
import java.util.ArrayList;
import java.util.List;

public class BigLineView extends View {
    private static int ANIMATION_DURATION = 200;
    private static int MAX_ALPHA = 255;
    private Data data;
    private Paint p;
    private Paint fp, fp2;
    private float[] points;

    private long minX, maxX;
    private long maxY, prevMaxY;
    private float fromX, toX;
    private long step0, step0Time;
    private float step0k, step0b;
    private boolean step0Down;
    private int borderW;

    private SparseArray<Long> lineToTime = new SparseArray<>();
    private SparseArray<Boolean> lineToUp = new SparseArray<>();
    private boolean[] lineDisabled;

    private List<MoveListener> listeners = new ArrayList<>();

    public BigLineView(Context context) {
        super(context);
        init();
    }

    public BigLineView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public BigLineView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        borderW = Utils.dpToPx(6);

        p = new Paint();
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(5f);
        p.setAntiAlias(true);

        fp = new Paint();
        fp.setStyle(Paint.Style.FILL);

        fp2 = new Paint();
        fp2.setStyle(Paint.Style.FILL);

        setOnTouchListener(new OnTouchListener() {
            private boolean isStartPressed = false;
            private boolean isEndPressed = false;
            private boolean isInsidePressed = false;
            private float startPressX = 0f;
            private float startFromX = 0f;
            private float startToX = 0f;
            private float startY = 0f;

            @SuppressLint("ClickableViewAccessibility")
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int paddingStart = getPaddingLeft();
                int paddingEnd = getPaddingRight();

                int w = getWidth() - paddingStart - paddingEnd;

                float startBorder = paddingStart + w * fromX;
                float endBorder = paddingStart + w * toX;

                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    startY = event.getY();
                    getParent().requestDisallowInterceptTouchEvent(true);

                    if (event.getX() > startBorder - 2 * borderW
                            && event.getX() < startBorder + borderW) {
                        isStartPressed = true;
                        Log.v("BigLineView", "pressed startBorder");
                    } else if (event.getX() > endBorder - borderW
                            && event.getX() < endBorder + 2 * borderW) {
                        isEndPressed = true;
                        Log.v("BigLineView", "pressed endBorder");
                    } else if (event.getX() > startBorder + borderW
                            && event.getX() < endBorder - borderW) {
                        isInsidePressed = true;
                        Log.v("BigLineView", "pressed inside");
                    } else {
                        Log.v("BigLineView", "pressed outside");
                    }
                    startPressX = event.getX();
                    startFromX = fromX;
                    startToX = toX;
                    Log.v("BigLineView", "startPressX=" + startPressX);
                    Log.v("BigLineView", "startFromX=" + startFromX);
                    Log.v("BigLineView", "startBorder=" + startBorder);
                } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
                    if (Math.abs(event.getY() - startY) > Utils.dpToPx(30)) {
                        getParent().requestDisallowInterceptTouchEvent(false);
                    }

                    Log.v("BigLineView", "event.getX()=" + event.getX());
                    //Log.v("BigLineView", "toX=" + toX);
                    //Log.v("BigLineView", "fromX=" + fromX);
                    float diff = (event.getX() - startPressX) / w;
                    if (isStartPressed) {
                        float newFromX = startFromX + diff;
                        if (newFromX > toX - 0.01) return true;
                        setFrom(newFromX);
                    } else if (isEndPressed) {
                        float newToX = startToX + diff;
                        if (newToX < fromX + 0.01) return true;
                        setTo(newToX);
                    } else if (isInsidePressed) {
                        float newFromX = startFromX + diff;
                        float newToX = startToX + diff;
                        if (newFromX < 0) {
                            newFromX = 0f;
                            newToX = startToX - startFromX;
                        } else if (newToX > 1) {
                            newToX = 1f;
                            newFromX = 1 - startToX + startFromX;
                        }
                        setFrom(newFromX);
                        setTo(newToX);
                    }
                } else if (event.getAction() == MotionEvent.ACTION_CANCEL
                        || event.getAction() == MotionEvent.ACTION_UP) {
                    getParent().requestDisallowInterceptTouchEvent(false);
                    isStartPressed = false;
                    isEndPressed = false;
                    isInsidePressed = false;
                } else {
                    getParent().requestDisallowInterceptTouchEvent(false);
                }
                return true;
            }
        });
    }

    void setChartBackgroundColor(int color) {
        //bp.setColor(color);
    }

    void setChartForegroundColor(int color) {
        fp.setColor(color);
    }

    void setChartForegroundBorderColor(int color) {
        fp2.setColor(color);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        long time = System.currentTimeMillis();
        super.onDraw(canvas);

        int paddingStart = getPaddingLeft();
        int paddingEnd = getPaddingRight();
        int paddingTop = getPaddingTop();
        int paddingBottom = getPaddingBottom();

        int w = getWidth() - paddingStart - paddingEnd;
        int h = (getHeight() - paddingBottom - paddingTop - Utils.dpToPx(6));

        if (data.columns.length == 0) return;

        if (step0Time != 0L) {
            step0 = (long) (step0k * (time - step0Time) + step0b);
            if (step0Down) {
                step0 = Math.min(step0, (long) (maxY * 0.7f / 5f));
            } else {
                step0 = Math.max(step0, (long) (maxY * 0.7f / 5f));
            }
            //Log.v("BigLineView", "maxY=" + maxY + " step0=" + step0);
        }

        Data.Column columnX = data.columns[0];

        for (int j = 1; j < data.columns.length; j++) {
            Data.Column column = data.columns[j];

            int size = column.value.length;
            for (int i = 0; i < size; i++) {
                float startX = w * (columnX.value[i] - minX) * 1f / (maxX - minX);
                float startY = convertToY(h, column.value[i]);

                if (i == 0) {
                    points[4 * i] = startX;
                    points[4 * i + 1] = startY;
                } else if (i == size - 1) {
                    points[4 * i - 2] = startX;
                    points[4 * i - 1] = startY;
                } else {
                    points[4 * i - 2] = startX;
                    points[4 * i - 1] = startY;
                    points[4 * i] = startX;
                    points[4 * i + 1] = startY;
                }
            }

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
                if (p.getAlpha() != 0) p.setAlpha(0);
            } else {
                if (p.getAlpha() != MAX_ALPHA) p.setAlpha(MAX_ALPHA);
            }

            canvas.drawLines(points, p);
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

        float startBorder = w * fromX;
        float endBorder = Math.min(w * toX, w);

        canvas.drawRect(paddingStart, paddingTop, paddingStart + startBorder,
                getHeight() - paddingBottom, fp);
        canvas.drawRect(paddingStart + endBorder, paddingTop, getWidth() - paddingEnd,
                getHeight() - paddingBottom, fp);
        canvas.drawRect(paddingStart + startBorder, paddingTop,
                paddingStart + startBorder + Utils.dpToPx(6), getHeight() - paddingBottom, fp2);
        canvas.drawRect(paddingStart + endBorder - Utils.dpToPx(6), paddingTop,
                paddingStart + endBorder, getHeight() - paddingBottom, fp2);
        canvas.drawRect(paddingStart + startBorder + Utils.dpToPx(6), paddingTop,
                paddingStart + endBorder - Utils.dpToPx(6), paddingTop + Utils.dpToPx(2), fp2);
        canvas.drawRect(paddingStart + startBorder + Utils.dpToPx(6),
                getHeight() - paddingBottom - Utils.dpToPx(2),
                paddingStart + endBorder - Utils.dpToPx(6), getHeight() - paddingBottom, fp2);

        //Log.v("BigLineView", "time=" + (System.currentTimeMillis() - time) + "ms");

        if (lineToTime.size() != 0 || step0Time != 0L) {
            postInvalidateOnAnimation();
        }
    }

    private float convertToY(int h, long y) {
        return h - h * y * 1f / (maxY + step0) - 1;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        Log.v("onMeasure w", MeasureSpec.toString(widthMeasureSpec));
        Log.v("onMeasure h", MeasureSpec.toString(heightMeasureSpec));
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    public void setData(Data data) {
        this.data = data;
        lineDisabled = new boolean[data.columns.length];

        Data.Column columnX = data.columns[0];
        points = new float[(columnX.value.length - 1) * 4];
        minX = columnX.value[0];
        maxX = columnX.value[columnX.value.length - 1];
        setFrom(0f);
        setTo(1f);

        calculateMaxY();

        invalidate();
    }

    public void setFrom(float from) {
        fromX = Math.max(0, from);
        for (MoveListener listener : listeners) {
            listener.onUpdateFrom(fromX);
        }
        log();

        invalidate();
    }

    public void setTo(float to) {
        toX = Math.min(1, to);
        for (MoveListener listener : listeners) {
            listener.onUpdateTo(toX);
        }
        log();

        invalidate();
    }

    private void calculateMaxY() {
        long maxY = 0;
        for (int i = 1; i < data.columns.length; i++) {
            if (lineDisabled[i] || lineToTime.get(i) != null && !lineToUp.get(i)) continue;
            long[] y = data.columns[i].value;
            for (long aY : y) {
                maxY = Math.max(maxY, aY);
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
            long time = System.currentTimeMillis();
            BigLineView.this.maxY = maxY;

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
        Log.v("BigLineView", "maxY = " + maxY + ", step0 = " + step0);
    }

    void addListener(MoveListener listener) {
        listeners.add(listener);
    }

    void clearListeners() {
        listeners.clear();
    }

    public float getFromX() {
        return fromX;
    }

    public float getToX() {
        return toX;
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

    interface MoveListener {
        void onUpdateFrom(float from);

        void onUpdateTo(float to);
    }
}
