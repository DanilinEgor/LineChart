package com.kuxurum.smoothlinechart;

import android.animation.Animator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
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
import java.util.Arrays;
import java.util.List;

public class BigStackedBarLineView extends View {
    private static int ANIMATION_DURATION = 1000;
    private static int MAX_ALPHA = 255;
    private Data data;
    private Paint p;

    private long minX, maxX;
    private int fromIndex, toIndex;
    private long maxY, prevMaxY;
    private float fromX, toX;
    private long step0, step0Time;
    private float step0k, step0b;
    private boolean step0Down;

    private SparseArray<Long> lineToTime = new SparseArray<>();
    private SparseArray<Boolean> lineToUp = new SparseArray<>();
    private boolean[] lineDisabled;

    private static final int MIN_P_ALPHA = 50;
    private static final int MAX_P_ALPHA = 150;
    private Paint fp, fp2;

    private int borderW;
    private ValueAnimator colorAnimator = new ValueAnimator();
    private boolean isStartPressed = false;
    private boolean isEndPressed = false;
    private boolean isInsidePressed = false;
    private List<MoveListener> listeners = new ArrayList<>();
    private float minFraction;
    private float fromLimit, limit;

    private int stepIndex = 1;

    //LinearGradient shader;
    //Matrix m = new Matrix();

    public BigStackedBarLineView(Context context) {
        super(context);
        init();
    }

    public BigStackedBarLineView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public BigStackedBarLineView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setStyle(Paint.Style.FILL);

        setPadding(Utils.dpToPx(24), 0, Utils.dpToPx(24), 0);
        borderW = Utils.dpToPx(6);

        fp = new Paint();
        fp.setStyle(Paint.Style.FILL);

        fp2 = new Paint();
        fp2.setStyle(Paint.Style.FILL);

        colorAnimator.setDuration(150);
        colorAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                fp2.setAlpha((int) animation.getAnimatedValue());
                invalidate();
            }
        });

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
                    if (isStartPressed || isInsidePressed || isEndPressed) {
                        colorAnimator.setIntValues(MIN_P_ALPHA, MAX_P_ALPHA);
                        colorAnimator.start();
                    }
                    prevX = startPressX;
                } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
                    if (Math.abs(event.getY() - startY) > Utils.dpToPx(30)) {
                        getParent().requestDisallowInterceptTouchEvent(false);
                    }

                    //Log.v("BigLineBorderView", "event.getX()=" + event.getX());
                    //Log.v("BigLineBorderView", "toX=" + toX);
                    //Log.v("BigLineBorderView", "fromX=" + fromX);
                    float diff = (event.getX() - startPressX) / w;

                    if (isStartPressed) {
                        float newFromX = startFromX + diff;
                        setFrom(Math.min(newFromX, toX - minFraction));
                        setTo(toX);
                    } else if (isEndPressed) {
                        float newToX = startToX + diff;
                        setTo(Math.max(newToX, fromX + minFraction));
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
                    if (colorAnimator.isStarted()) {
                        colorAnimator.cancel();
                    }
                    if (isStartPressed || isEndPressed || isInsidePressed) {
                        colorAnimator.setIntValues(MAX_P_ALPHA, MIN_P_ALPHA);
                        colorAnimator.start();
                    }
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

    void setChartForegroundColor(int color) {
        fp.setColor(color);
    }

    void setChartForegroundBorderColor(int color) {
        fp2.setColor(color);
        fp2.setAlpha(MIN_P_ALPHA);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        long time = System.currentTimeMillis();
        super.onDraw(canvas);

        int paddingStart = getPaddingLeft();
        int paddingEnd = getPaddingRight();
        int paddingTop = getPaddingTop();
        int paddingBottom = getPaddingBottom();

        int w = getWidth() - paddingStart - paddingEnd - 2 * Utils.dpToPx(7);
        int h = getHeight() - paddingTop - paddingBottom - Utils.dpToPx(5);

        //canvas.clipRect(paddingStart, paddingTop, getWidth() - paddingEnd,
        //        getHeight() - paddingBottom);

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

        drawLines(canvas, time, w, h, paddingStart + Utils.dpToPx(7), paddingTop,
                paddingEnd + Utils.dpToPx(7));

        w = getWidth() - paddingStart - paddingEnd - 2 * borderW;
        float startBorder = w * fromX + borderW;
        float endBorder = w * toX + borderW;

        if (borderW < startBorder - borderW) {
            canvas.drawRect(paddingStart + borderW, paddingTop,
                    paddingStart + startBorder - borderW, getHeight() - paddingBottom, fp);
        }
        if (paddingStart + endBorder + borderW < getWidth() - paddingEnd - borderW) {
            canvas.drawRect(paddingStart + endBorder + borderW, paddingTop,
                    getWidth() - paddingEnd - borderW, getHeight() - paddingBottom, fp);
        }

        //if (isStartPressed) {
        //    m.reset();
        //    m.postScale(toX - fromX, 1f);
        //    m.postTranslate(startBorder, 0);
        //    shader.setLocalMatrix(m);
        //} else if (isEndPressed) {
        //    m.reset();
        //    m.postScale(-1, 1, w / 2f, h / 2f);
        //    m.postScale(toX - fromX, 1f);
        //    m.postTranslate(startBorder, 0);
        //    shader.setLocalMatrix(m);
        //} else if (isInsidePressed) {
        //    m.reset();
        //    m.postTranslate(w, 0);
        //    shader.setLocalMatrix(m);
        //}

        canvas.drawRect(paddingStart + startBorder - borderW, paddingTop,
                paddingStart + startBorder, getHeight() - paddingBottom, fp2);
        canvas.drawRect(paddingStart + endBorder, paddingTop, paddingStart + endBorder + borderW,
                getHeight() - paddingBottom, fp2);
        canvas.drawRect(paddingStart + startBorder, paddingTop, paddingStart + endBorder,
                paddingTop + Utils.dpToPx(2), fp2);
        canvas.drawRect(paddingStart + startBorder, getHeight() - paddingBottom - Utils.dpToPx(2),
                paddingStart + endBorder, getHeight() - paddingBottom, fp2);

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

        //Log.v("BigLineView", "time=" + (System.currentTimeMillis() - time) + "ms");

        if (lineToTime.size() != 0 || step0Time != 0L) {
            invalidate();
        }

        //Paint p = new Paint();
        //p.setColor(Color.parseColor("#80ff0000"));
        //canvas.drawRect(paddingStart + startBorder - 3 * borderW, 0,
        //        paddingStart + startBorder + borderW, h, p);
        //canvas.drawRect(paddingStart + endBorder - borderW, 0,
        //        paddingStart + endBorder + 3 * borderW, h, p);
    }

    private void drawLines(Canvas canvas, long time, int w, int h, int paddingStart, int paddingTop,
            int paddingEnd) {
        Data.Column columnX = data.columns[0];

        int size = columnX.value.length;

        for (int i = fromIndex; i < toIndex; i += stepIndex) {
            long prevMinY = 0;
            for (int j = 1; j < data.columns.length; j++) {
                if (lineDisabled[j]) continue;

                Data.Column column = data.columns[j];

                //Log.v("LineView", "lineToTime.get(j)=" + lineToTime.get(j));
                p.setColor(column.color);

                float y = column.value[i] + progress * diff.columns[j].value[i];
                if (j == 1) {
                    Log.v("lines", "y="
                            + y
                            + " diff="
                            + diff.columns[j].value[i]
                            + " progress="
                            + progress);
                }
                if (lineToTime.get(j) != null) {
                    //int alpha;
                    if (lineToUp.get(j)) {
                        float k = Math.min(1,
                                Math.max(0, (time - lineToTime.get(j)) * 1f / ANIMATION_DURATION));
                        Log.v("SV", "k=" + k);
                        y *= k;
                        //alpha = Math.min(
                        //        (int) (1f * MAX_ALPHA / ANIMATION_DURATION * (time - lineToTime.get(
                        //                j))), MAX_ALPHA);
                    } else {
                        float k = Math.min(1, Math.max(0,
                                1 + (lineToTime.get(j) - time) * 1f / ANIMATION_DURATION));
                        Log.v("SV", "k=" + k);
                        y *= k;

                        //alpha = Math.max(
                        //        (int) (1f * MAX_ALPHA / ANIMATION_DURATION * (lineToTime.get(j)
                        //                - time + ANIMATION_DURATION)), 0);
                    }
                    //p.setAlpha(alpha);
                } else {
                    //if (p.getAlpha() != MAX_ALPHA) p.setAlpha(MAX_ALPHA);
                }

                float startX = Math.min(getWidth() - paddingEnd, Math.max(paddingStart,
                        paddingStart + w * (columnX.value[i] - minX) * 1f / (maxX - minX)));
                float stopX = Math.min(getWidth() - paddingEnd, Math.max(paddingStart, paddingStart
                        + w * (columnX.value[Math.min(i + stepIndex,
                        data.columns[0].value.length - 1)] - minX) * 1f / (maxX - minX)));
                float startY = Utils.dpToPx(2) + paddingTop + convertToY(h, prevMinY + (long) y);
                float stopY = Utils.dpToPx(2) + paddingTop + convertToY(h, prevMinY);

                canvas.drawRect((float) Math.floor(startX), startY, (float) Math.ceil(stopX), stopY,
                        p);

                //canvas.drawLine(startX, 0, startX, h, fp2);

                prevMinY += y;
            }
        }
    }

    private float convertToY(int h, long y) {
        return h - h * y * 1f / (maxY + step0) - 1;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        limit = Utils.dpToPx(24) * 1f / w;
        if (oldh == 0 && oldw == 0) {
            setFrom(0.75f);
            setTo(2f);
        }

        Data.Column columnX = data.columns[0];
        float diff = 0;
        stepIndex = 0;
        while (diff < w / 100f) {
            diff = w * (columnX.value[++stepIndex] - columnX.value[0]) * 1f / (maxX - minX);
        }

        //Log.v("BigLineView", "toLimit=" + fromLimit);
        //shader = new LinearGradient(0, h / 2f, w, h / 2f, 0xff507da1, 0x32507da1, Shader.TileMode.CLAMP);
        //fp3.setShader(shader);
    }

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
        minFraction = 12 * 60 * 60 * 1000f / (maxX - minX);
    }

    public void setData(Data data) {
        diff.columns = new Data.Column[data.columns.length];
        for (int i = 0; i < diff.columns.length; i++) {
            Data.Column column = new Data.Column();
            column.value = new long[data.columns[0].value.length];
            diff.columns[i] = column;
        }

        this.data = data;
        lineDisabled = new boolean[data.columns.length];

        Data.Column columnX = data.columns[0];
        fromIndex = 0;
        toIndex = columnX.value.length - 1;
        minX = columnX.value[0];
        maxX = columnX.value[columnX.value.length - 1];
        minFraction = 12 * 60 * 60 * 1000f / (maxX - minX);

        //setFrom(0f);
        //setTo(1f);

        calculateMaxY();

        invalidate();
    }

    Data diff = new Data();
    float progress;

    public void setMinMaxX(long minX, long maxX, final Data data) {
        //this.minX = minX;
        //this.maxX = maxX;

        int fromIndex = Arrays.binarySearch(BigStackedBarLineView.this.data.columns[0].value, minX);
        if (fromIndex < 0) fromIndex = -fromIndex - 1;
        int toIndex = Arrays.binarySearch(BigStackedBarLineView.this.data.columns[0].value, maxX);
        if (toIndex < 0) toIndex = -toIndex - 1;

        PropertyValuesHolder minProp = PropertyValuesHolder.ofFloat("min", this.minX, minX);
        PropertyValuesHolder maxProp = PropertyValuesHolder.ofFloat("max", this.maxX, maxX);
        ValueAnimator valueAnimator = ValueAnimator.ofPropertyValuesHolder(minProp, maxProp);
        valueAnimator.setDuration(ANIMATION_DURATION);
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                BigStackedBarLineView.this.minX =
                        ((Float) animation.getAnimatedValue("min")).longValue();
                BigStackedBarLineView.this.maxX =
                        ((Float) animation.getAnimatedValue("max")).longValue();
                //BigStackedBarLineView.this.minX =
                //        data.columns[0].value[BigStackedBarLineView.this.fromIndex];
                //BigStackedBarLineView.this.maxX =
                //        data.columns[0].value[BigStackedBarLineView.this.toIndex];
                Log.v("lines", "minX="
                        + BigStackedBarLineView.this.minX
                        + " maxX="
                        + BigStackedBarLineView.this.maxX);
                calculateMaxY();

                long start = System.currentTimeMillis();
                Data.Column columnX = BigStackedBarLineView.this.data.columns[0];
                float diff = 0;
                stepIndex = 0;
                while (diff < getWidth() / 100f) {
                    diff = getWidth() * (columnX.value[++stepIndex] - columnX.value[0]) * 1f / (
                            BigStackedBarLineView.this.maxX
                                    - BigStackedBarLineView.this.minX);
                }
                long end = System.currentTimeMillis();
                Log.v("lines", (end - start) + "ms");

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
                //BigStackedBarLineView.this.fromIndex = finalFromIndex;
                //BigStackedBarLineView.this.toIndex = finalToIndex;
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

                Data local = BigStackedBarLineView.this.data;

                for (int j = 0; j < 7; j++) {
                    for (int i = 1; i < copy.columns.length; i++) {
                        Arrays.fill(copy.columns[i].value, j * 24, (j + 1) * 24,
                                local.columns[i].value[finalFromIndex + j]);
                    }
                }

                setDataWithoutUpdate(data);

                for (int j = BigStackedBarLineView.this.fromIndex; j < BigStackedBarLineView.this.toIndex; ++j) {
                    if (data.columns[0].value[j] < BigStackedBarLineView.this.minX || data.columns[0].value[j] > BigStackedBarLineView.this.maxX) continue;
                    long sum = 0;
                    for (int i = 1; i < data.columns.length; i++) {
                        if (lineDisabled[i] || lineToTime.get(i) != null && !lineToUp.get(i)) continue;
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
        for (int j = fromIndex; j < toIndex; ++j) {
            if (data.columns[0].value[j] < minX || data.columns[0].value[j] > maxX) continue;
            long sum = 0;
            for (int i = 1; i < data.columns.length; i++) {
                if (lineDisabled[i] || lineToTime.get(i) != null && !lineToUp.get(i)) continue;
                sum += data.columns[i].value[j];
            }
            maxY = Math.max(maxY, sum);
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

        Log.v("lines", "maxY=" + maxY);
        if (prevMaxY != maxY) {
            long time = System.currentTimeMillis();
            BigStackedBarLineView.this.maxY = maxY;

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

    interface MoveListener {
        void onUpdateFrom(float from);

        void onUpdateTo(float to);
    }
}
