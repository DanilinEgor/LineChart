package com.kuxurum.smoothlinechart;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.View;
import java.util.ArrayList;
import java.util.List;

public class BigTwoAxisLineView extends BaseBigLineView {
    private static int ANIMATION_DURATION = 200;
    private static int MAX_ALPHA = 255;
    private Data data;
    private Paint p;
    private float[] points;

    private long minX, maxX;
    private long maxY, minY;
    private long maxY2, minY2;
    private float fromX, toX;

    private long step0, step0Time;
    private float step0k, step0b;
    private boolean step0Down;
    private long step1, step1Time;
    private float step1k, step1b;
    private boolean step1Down;

    private long step02, step02Time;
    private float step02k, step02b;
    private boolean step02Down;
    private long step12, step12Time;
    private float step12k, step12b;
    private boolean step12Down;

    private SparseArray<Long> lineToTime = new SparseArray<>();
    private SparseArray<Boolean> lineToUp = new SparseArray<>();
    private boolean[] lineDisabled;

    private boolean isStartPressed = false;
    private boolean isEndPressed = false;
    private boolean isInsidePressed = false;
    private List<MoveListener> listeners = new ArrayList<>();
    private float fromLimit, limit;

    public BigTwoAxisLineView(Context context) {
        super(context);
        init();
    }

    public BigTwoAxisLineView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public BigTwoAxisLineView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    void init() {
        super.init();

        p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(2f);
        p.setStrokeCap(Paint.Cap.SQUARE);

        setPadding(Utils.dpToPx(24), 0, Utils.dpToPx(24), 0);

        setOnTouchListener(new OnTouchListener() {
            private float startPressX = 0f;
            private float startFromX = 0f;
            private float startToX = 0f;
            private float startY = 0f;
            private float prevX = 0f;
            private boolean toRight = false;

            @SuppressLint("ClickableViewAccessibility")
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int paddingStart = getPaddingLeft();
                int paddingEnd = getPaddingRight();

                int w = getWidth() - paddingStart - paddingEnd - 2 * borderW;
                float startBorder = w * fromX + borderW + paddingStart;
                float endBorder = Math.min(w * toX, w) + borderW + paddingStart;

                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    startY = event.getY();
                    getParent().requestDisallowInterceptTouchEvent(true);

                    float minDist = Math.min(borderW, endBorder - startBorder);
                    if (event.getX() > startBorder - 3 * borderW
                            && event.getX() < startBorder + minDist) {
                        isStartPressed = true;
                        //Log.v("BigLineBorderView", "pressed startBorder");
                    } else if (event.getX() > endBorder - minDist
                            && event.getX() < endBorder + 3 * borderW) {
                        isEndPressed = true;
                        //Log.v("BigLineBorderView", "pressed endBorder");
                    } else if (event.getX() > startBorder + minDist
                            && event.getX() < endBorder - minDist) {
                        isInsidePressed = true;
                        //Log.v("BigLineBorderView", "pressed inside");
                    } else {
                        //Log.v("BigLineBorderView", "pressed outside");
                    }
                    startPressX = event.getX();
                    startFromX = fromX;
                    startToX = toX;
                    //Log.v("BigLineBorderView", "startPressX=" + startPressX);
                    //Log.v("BigLineBorderView", "startFromX=" + startFromX);
                    //Log.v("BigLineBorderView", "startBorder=" + startBorder);
                    //drawPic();
                    prevX = startPressX;
                } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
                    if (Math.abs(event.getY() - startY) > Utils.dpToPx(30)) {
                        getParent().requestDisallowInterceptTouchEvent(false);
                    }

                    //Log.v("BigLineBorderView", "event.getX()=" + event.getX());
                    //Log.v("BigLineBorderView", "toX=" + toX);
                    //Log.v("BigLineBorderView", "fromX=" + fromX);
                    float diff = (event.getX() - startPressX) / w;
                    float m = 2f * borderW / w;
                    if (isStartPressed) {
                        float newFromX = startFromX + diff;
                        setFrom(Math.min(newFromX, toX - m));
                        setTo(toX);
                    } else if (isEndPressed) {
                        float newToX = startToX + diff;
                        setTo(Math.max(newToX, fromX + m));
                        setFrom(fromX);
                    } else if (isInsidePressed) {
                        float newFromX = startFromX + diff;
                        float newToX = startToX + diff;
                        //Log.v("BigLineBorderView", "newToX=" + newToX);
                        //Log.v("BigLineBorderView", "newFromX=" + newFromX);
                        if (newFromX < 0 - fromLimit) {
                            newFromX = -fromLimit;
                            newToX = newFromX - startFromX + startToX;
                        } else if (newToX > 1f + fromLimit) {
                            newToX = 1f + fromLimit;
                            newFromX = newToX - startToX + startFromX;
                        }
                        setFrom(newFromX);
                        setTo(newToX);
                    }

                    boolean wasToRight = toRight;
                    toRight = event.getX() - prevX > 0;
                    if (wasToRight != toRight) {
                        startPressX = event.getX();
                        startFromX = fromX;
                        startToX = toX;
                    }

                    prevX = event.getX();
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

    @Override
    protected void onDraw(Canvas canvas) {
        long time = System.currentTimeMillis();
        super.onDraw(canvas);

        int paddingStart = getPaddingLeft();
        int paddingEnd = getPaddingRight();
        int paddingTop = getPaddingTop();
        int paddingBottom = getPaddingBottom();

        int w = getWidth() - paddingStart - paddingEnd - 2 * borderW;
        int h = (getHeight() - paddingBottom - paddingTop - Utils.dpToPx(2));

        if (data.columns.length == 0) return;

        if (step0Time != 0L) {
            step0 = (long) (step0k * (time - step0Time) + step0b);
            if (step0Down) {
                step0 = Math.min(step0, (long) ((maxY - minY) * 0.2f / 5f));
            } else {
                step0 = Math.max(step0, (long) ((maxY - minY) * 0.2f / 5f));
            }
            //Log.v("TwoAxisLineView", "maxY=" + maxY + " step0=" + step0);
        }

        if (step1Time != 0L) {
            step1 = (long) (step1k * (time - step1Time) + step1b);
            if (step1Down) {
                step1 = Math.min(step1, 0);
            } else {
                step1 = Math.max(step1, 0);
            }
            //Log.v("TwoAxisLineView", "maxY=" + maxY + " step1=" + step1);
        }

        if (step02Time != 0L) {
            step02 = (long) (step02k * (time - step02Time) + step02b);
            if (step02Down) {
                step02 = Math.min(step02, (long) ((maxY2 - minY2) * 0.2f / 5f));
            } else {
                step02 = Math.max(step02, (long) ((maxY2 - minY2) * 0.2f / 5f));
            }
            //Log.v("TwoAxisLineView", "maxY=" + maxY + " step02=" + step02);
        }

        if (step12Time != 0L) {
            step12 = (long) (step12k * (time - step12Time) + step12b);
            if (step12Down) {
                step12 = Math.min(step12, 0);
            } else {
                step12 = Math.max(step12, 0);
            }
            //Log.v("TwoAxisLineView", "maxY=" + maxY + " step1=" + step1);
        }

        drawLines(canvas, time, w, paddingStart, paddingTop, paddingEnd, paddingBottom);

        w = getWidth() - paddingStart - paddingEnd - 2 * borderW;
        float startBorder = w * fromX + borderW;
        float endBorder = w * toX + borderW;

        drawBorder(canvas, startBorder, endBorder, paddingStart, paddingTop, paddingEnd,
                paddingBottom);

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
        if (time - step02Time > ANIMATION_DURATION) {
            step02Time = 0L;
            step02 = (long) ((maxY2 - minY2) * 0.2f / 5f);
        }

        if (time - step12Time > ANIMATION_DURATION) {
            step12Time = 0L;
            step12 = 0;
        }

        //Log.v("BigLineView", "time=" + (System.currentTimeMillis() - time) + "ms");

        if (lineToTime.size() != 0
                || step0Time != 0L
                || step1Time != 0L
                || step02Time != 0L
                || step12Time != 0L) {
            postInvalidateOnAnimation();
        }

        //Paint p = new Paint();
        //p.setColor(Color.parseColor("#80ff0000"));
        //canvas.drawRect(paddingStart + startBorder - 3 * borderW, 0,
        //        paddingStart + startBorder + borderW, h, p);
        //canvas.drawRect(paddingStart + endBorder - borderW, 0,
        //        paddingStart + endBorder + 3 * borderW, h, p);
    }

    private void drawLines(Canvas canvas, long time, int w, int paddingStart, int paddingTop,
            int paddingEnd, int paddingBottom) {
        Data.Column columnX = data.columns[0];
        int h = (getHeight() - paddingBottom - paddingTop - Utils.dpToPx(2));

        for (int j = 1; j < data.columns.length; j++) {
            if (lineDisabled[j]) continue;
            Data.Column column = data.columns[j];

            int size = column.value.length;
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
            } else {
                if (p.getAlpha() != MAX_ALPHA) p.setAlpha(MAX_ALPHA);
            }

            for (int i = 0; i < size; i++) {
                float startX =
                        paddingStart + borderW + w * (columnX.value[i] - minX) * 1f / (maxX - minX);
                float startY;
                if (j == 1) {
                    startY = Utils.dpToPx(1) + paddingTop + convertToY(h, column.value[i]);
                } else {
                    startY = Utils.dpToPx(1) + paddingTop + convertToY2(h, column.value[i]);
                }

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
                //canvas.drawCircle(startX, startY, 0.1f, p);
            }

            canvas.drawLines(points, p);
        }
    }

    private float convertToY(int h, long y) {
        return h - h * (y - minY - step1) * 1f / (maxY - minY + step0 - step1);
    }

    private float convertToY2(int h, float y) {
        return h - h * (y - minY2 - step12) * 1f / (maxY2 - minY2 + step02 - step12);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        limit = Utils.dpToPx(24) * 1f / w;
        if (oldh == 0 && oldw == 0) {
            setFrom(0.75f);
            setTo(2f);
        }
        //Log.v("BigLineView", "toLimit=" + fromLimit);
        //shader = new LinearGradient(0, h / 2f, w, h / 2f, 0xff507da1, 0x32507da1, Shader.TileMode.CLAMP);
        //fp3.setShader(shader);
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

        calculateMinMaxY();

        invalidate();
    }

    public void setFrom(float from) {
        Data.Column columnX = data.columns[0];
        long minX1 = columnX.value[0];
        long maxX1 = columnX.value[columnX.value.length - 1];

        long fromX1 = (long) (minX1 + fromX * (maxX1 - minX1));
        long toX1 = (long) (minX1 + toX * (maxX1 - minX1));

        fromLimit = Math.min(limit * (toX1 - fromX1) / (maxX1 - minX1),
                Utils.dpToPx(24) * 1f / (getWidth() + 2 * Utils.dpToPx(24)));

        fromX = Math.max(-fromLimit, from);

        //Log.v("BigLineView", "fromLimit=" + fromLimit);
        for (MoveListener listener : listeners) {
            listener.onUpdateFrom(fromX);
        }
        log();

        invalidate();
    }

    public void setTo(float to) {
        Data.Column columnX = data.columns[0];
        long minX1 = columnX.value[0];
        long maxX1 = columnX.value[columnX.value.length - 1];

        long fromX1 = (long) (minX1 + fromX * (maxX1 - minX1));
        long toX1 = (long) (minX1 + toX * (maxX1 - minX1));

        fromLimit = Math.min(limit * (toX1 - fromX1) / (maxX1 - minX1),
                Utils.dpToPx(24) * 1f / (getWidth() + 2 * Utils.dpToPx(24)));

        toX = Math.min(1 + fromLimit, to);

        //Log.v("BigLineView", "toLimit=" + fromLimit);
        for (MoveListener listener : listeners) {
            listener.onUpdateTo(toX);
        }
        log();

        invalidate();
    }

    private void calculateMinMaxY() {
        long time = System.currentTimeMillis();

        long prevMinY = minY;
        minY = Long.MAX_VALUE;
        {
            //if (lineDisabled[1] || lineToTime.get(1) != null && !lineToUp.get(1)) continue;
            long[] y = data.columns[1].value;
            for (long aY : y) {
                minY = Math.min(minY, aY);
            }
        }

        int pow = 10;
        if (minY >= 133) {
            long min = (long) (minY * 10f / 11f);
            long max = (long) (minY * 50f / 51f);

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
        {
            //if (lineDisabled[1] || lineToTime.get(1) != null && !lineToUp.get(1)) continue;
            long[] y = data.columns[1].value;
            for (long aY : y) {
                maxY = Math.max(maxY, aY);
            }
        }

        pow /= 10;
        maxY = (long) ((maxY / pow + (maxY % pow * 1f / pow > 0.5f ? 1 : 0.5f)) * pow);

        if (prevMaxY != maxY) {
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

        calculateMinMaxY2();
    }

    private void calculateMinMaxY2() {
        long time = System.currentTimeMillis();

        long prevMinY2 = minY2;
        minY2 = Long.MAX_VALUE;
        {
            //if (lineDisabled[2] || lineToTime.get(2) != null && !lineToUp.get(2)) continue;
            long[] y = data.columns[2].value;
            for (long aY : y) {
                minY2 = Math.min(minY2, aY);
            }
        }

        int pow = 10;
        if (minY2 >= 133) {
            long min = (long) (minY2 * 10f / 11f);
            long max = (long) (minY2 * 50f / 51f);

            while (true) {
                long prevMax = max;
                max = max - max % pow;
                if (max < min) {
                    minY2 = prevMax;
                    break;
                }
                pow *= 10;
            }
        } else if (minY2 > 0) {
            minY2 = minY2 - minY2 % 10 + 10;
        }

        if (prevMinY2 != minY2) {
            int prev;
            if (step12Time == 0L) {
                prev = (int) (prevMinY2 - minY2);
            } else {
                prev = (int) (prevMinY2 + step12 - minY2);
            }

            int next = 0;

            step12Time = time;
            step12k = (next - prev) * 1f / ANIMATION_DURATION;
            step12b = prev;
            step12Down = next > prev;
        }

        long prevMaxY2 = maxY2;
        maxY2 = 0;
        {
            //if (lineDisabled[2] || lineToTime.get(2) != null && !lineToUp.get(2)) continue;
            long[] y = data.columns[2].value;
            for (long aY : y) {
                maxY2 = Math.max(maxY2, aY);
            }
        }

        pow /= 10;
        maxY2 = (long) ((maxY2 / pow + (maxY2 % pow * 1f / pow > 0.5f ? 1 : 0.5f)) * pow);

        if (prevMaxY2 != maxY2) {
            int prev;
            if (step02Time == 0L) {
                prev = (int) ((prevMaxY2 - prevMinY2) * 0.2f / 5f + prevMaxY2 - maxY2);
            } else {
                prev = (int) (prevMaxY2 + step02 - maxY2);
            }

            int next = (int) ((maxY2 - minY2) * 0.2f / 5f);

            step02Time = time;
            step02k = (next - prev) * 1f / ANIMATION_DURATION;
            step02b = prev;
            step02Down = next > prev;
        }

        //Log.v("BigTwoAxisLineView", "maxY2 = "
        //        + maxY2
        //        + ", minY2 = "
        //        + minY2
        //        + ", step02 = "
        //        + step02
        //        + ", step12 = "
        //        + step12);
    }

    private void log() {
        //Log.v("BigLineView", "maxY = " + maxY + ", step0 = " + step0);
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
        calculateMinMaxY();
        log();
        invalidate();
    }

    void addListener(MoveListener listener) {
        listeners.add(listener);
    }

    void clearListeners() {
        listeners.clear();
    }

    interface MoveListener {
        void onUpdateFrom(float from);

        void onUpdateTo(float to);
    }
}
