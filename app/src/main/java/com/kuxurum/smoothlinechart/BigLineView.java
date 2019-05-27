package com.kuxurum.smoothlinechart;

import android.animation.Animator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
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

public class BigLineView extends BaseBigLineView {
    private static int ANIMATION_DURATION = 200;
    private static int MAX_ALPHA = 255;
    private Data data;
    private Paint p;
    private float[] points;

    private long minX, maxX;
    private long maxY, prevMaxY;
    private float fromX, toX;
    private long step0, step0Time;
    private float step0k, step0b;
    private boolean step0Down;

    private SparseArray<Long> lineToTime = new SparseArray<>();
    private SparseArray<Boolean> lineToUp = new SparseArray<>();
    private boolean[] lineDisabled;

    private boolean isStartPressed = false;
    private boolean isEndPressed = false;
    private boolean isInsidePressed = false;
    private List<MoveListener> listeners = new ArrayList<>();
    private float fromLimit, limit;

    private static final int WHOLE = 0;
    private static final int ANIMATING = 1;
    private static final int DETAILED = 2;
    int state = WHOLE;

    private Data oldData;
    private float oldFromX, oldToX;

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
                step0 = Math.min(step0, (long) (maxY * 0.7f / 5f));
            } else {
                step0 = Math.max(step0, (long) (maxY * 0.7f / 5f));
            }
            //Log.v("BigLineView", "maxY=" + maxY + " step0=" + step0);
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
            step0 = (long) (maxY * 0.7f / 5f);
        }

        //Log.v("BigLineView", "time=" + (System.currentTimeMillis() - time) + "ms");

        if (lineToTime.size() != 0 || step0Time != 0L) {
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
                float startY = Utils.dpToPx(1) + paddingTop + convertToY(h, column.value[i]);

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
        return h - h * y * 1f / (maxY + step0);
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

        calculateMaxY();

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
        calculateMaxY();
        log();
        invalidate();
    }

    void addListener(MoveListener listener) {
        listeners.add(listener);
    }

    void clearListeners() {
        listeners.clear();
    }

    public void setDetailed(Data newData) {
        oldFromX = fromX;
        oldToX = toX;

        oldData = data;
        state = ANIMATING;
        setData(newData);

        PropertyValuesHolder fromProp = PropertyValuesHolder.ofFloat("from", fromX, 3f / 7);
        PropertyValuesHolder toProp = PropertyValuesHolder.ofFloat("to", toX, 4f / 7);
        ValueAnimator valueAnimator = ValueAnimator.ofPropertyValuesHolder(fromProp, toProp);
        valueAnimator.setDuration(ANIMATION_DURATION);
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                fromX = (float) animation.getAnimatedValue("from");
                toX = (float) animation.getAnimatedValue("to");

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
                setFrom(3f / 7);
                setTo(4f / 7);
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
        setData(oldData);

        PropertyValuesHolder fromProp = PropertyValuesHolder.ofFloat("from", fromX, oldFromX);
        PropertyValuesHolder toProp = PropertyValuesHolder.ofFloat("to", toX, oldToX);
        ValueAnimator valueAnimator = ValueAnimator.ofPropertyValuesHolder(fromProp, toProp);
        valueAnimator.setDuration(ANIMATION_DURATION);
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                fromX = (float) animation.getAnimatedValue("from");
                toX = (float) animation.getAnimatedValue("to");

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
                setFrom(oldFromX);
                setTo(oldToX);
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

    interface MoveListener {
        void onUpdateFrom(float from);

        void onUpdateTo(float to);
    }
}
