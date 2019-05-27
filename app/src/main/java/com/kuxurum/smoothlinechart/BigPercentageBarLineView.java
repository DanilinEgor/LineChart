package com.kuxurum.smoothlinechart;

import android.animation.Animator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.View;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BigPercentageBarLineView extends BaseBigLineView {
    private static int ANIMATION_DURATION = 500;
    private static int MAX_ALPHA = 255;
    private Data data;
    private Paint p;

    private long minX, maxX;
    private int fromIndex, toIndex;
    private long maxY, prevMaxY;
    private float fromX, toX;
    private float fromXDetailed, toXDetailed;

    private SparseArray<Long> lineToTime = new SparseArray<>();
    private SparseArray<Boolean> lineToUp = new SparseArray<>();
    private boolean[] lineDisabled;

    private static final int MIN_P_ALPHA = 128;
    private static final int MAX_P_ALPHA = 128;

    private boolean isStartPressed = false;
    private boolean isEndPressed = false;
    private boolean isInsidePressed = false;
    private List<MoveListener> listeners = new ArrayList<>();
    private float fromLimit, limit;

    private int stepIndex = 1;
    float[][] coords;
    Path[] paths;

    private static final int WHOLE = 0;
    private static final int ANIMATING = 1;
    private static final int DETAILED = 2;
    private int state = WHOLE;

    private float oldFrom, oldTo;

    public BigPercentageBarLineView(Context context) {
        super(context);
        init();
    }

    public BigPercentageBarLineView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public BigPercentageBarLineView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    void init() {
        super.init();

        setPadding(Utils.dpToPx(24), 0, Utils.dpToPx(24), 0);

        p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setStyle(Paint.Style.FILL);

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

                if (state == WHOLE) {
                    float startBorder = w * fromX + borderW + paddingStart;
                    float endBorder = Math.min(w * toX, w) + borderW + paddingStart;
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        startY = event.getY();
                        getParent().requestDisallowInterceptTouchEvent(true);

                        float minDist = Math.min(borderW, endBorder - startBorder);
                        if (event.getX() > startBorder - 3 * borderW
                                && event.getX() < startBorder + borderW + minDist) {
                            isStartPressed = true;
                            //Log.v("BigLineBorderView", "pressed startBorder");
                        } else if (event.getX() > endBorder - borderW - minDist
                                && event.getX() < endBorder + 2 * borderW) {
                            isEndPressed = true;
                            //Log.v("BigLineBorderView", "pressed endBorder");
                        } else if (event.getX() > startBorder + borderW + minDist
                                && event.getX() < endBorder - borderW - minDist) {
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
                } else if (state == DETAILED) {
                    float startBorder = w * fromXDetailed + borderW + paddingStart;
                    float endBorder = Math.min(w * toXDetailed, w) + borderW + paddingStart;
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        startY = event.getY();
                        getParent().requestDisallowInterceptTouchEvent(true);

                        float minDist = Math.min(borderW, endBorder - startBorder);
                        if (event.getX() > startBorder - 3 * borderW
                                && event.getX() < startBorder + borderW + minDist) {
                            isStartPressed = true;
                            //Log.v("BigLineBorderView", "pressed startBorder");
                        } else if (event.getX() > endBorder - borderW - minDist
                                && event.getX() < endBorder + 2 * borderW) {
                            isEndPressed = true;
                            //Log.v("BigLineBorderView", "pressed endBorder");
                        } else if (event.getX() > startBorder + borderW + minDist
                                && event.getX() < endBorder - borderW - minDist) {
                            isInsidePressed = true;
                            //Log.v("BigLineBorderView", "pressed inside");
                        } else {
                            //Log.v("BigLineBorderView", "pressed outside");
                        }
                        startPressX = event.getX();
                        startFromX = fromXDetailed;
                        startToX = toXDetailed;
                        //Log.v("BigLineBorderView", "startPressX=" + startPressX);
                        Log.v("BigLineBorderView", "startFromX=" + startFromX);
                        Log.v("BigLineBorderView", "startToX=" + startToX);
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
                        Log.v("BigLineBorderView", "diff=" + diff);

                        if (isStartPressed) {
                            float newFromX = startFromX + diff;
                            setFromDetailed(Math.min(newFromX, toXDetailed));
                            setToDetailed(toXDetailed);
                        } else if (isEndPressed) {
                            float newToX = startToX + diff;
                            setToDetailed(Math.max(newToX, fromXDetailed));
                            setFromDetailed(fromXDetailed);
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
                            float oldFromX = fromX;
                            float oldFromXDetailed = fromXDetailed;
                            int oldIndex = fromDetailedIndex;
                            setFromDetailed(newFromX);
                            if (oldIndex != fromDetailedIndex) {
                                toXDetailed += fromXDetailed - oldFromXDetailed;
                                toX += fromX - oldFromX;
                                toDetailedIndex += fromDetailedIndex - oldIndex;

                                for (MoveListener listener : listeners) {
                                    listener.onUpdateToIndex(toDetailedIndex);
                                }
                                invalidate();
                            }
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
                return false;
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

        if (data.columns.length == 0) return;

        int w = getWidth() - paddingStart - paddingEnd - 2 * borderW;

        float startBorder = w * fromX + borderW;
        float endBorder = w * toX + borderW;

        if (state == WHOLE || state == ANIMATING) {
            drawLines(canvas, time, fromIndex, toIndex);
        } else if (state == DETAILED) {
            drawLinesDetailed(canvas, time, fromIndex, toIndex);
            startBorder = w * fromXDetailed + borderW;
            endBorder = w * toXDetailed + borderW;
        }

        drawBorder(canvas, startBorder, endBorder, paddingStart, paddingTop, paddingEnd,
                paddingBottom);

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

        //Log.v("BigLineView", "time=" + (System.currentTimeMillis() - time) + "ms");

        if (lineToTime.size() != 0) {
            invalidate();
        }

        //Paint p = new Paint();
        //p.setColor(Color.parseColor("#80ff0000"));
        //canvas.drawRect(paddingStart + startBorder - 3 * borderW, 0,
        //        paddingStart + startBorder + borderW, h, p);
        //canvas.drawRect(paddingStart + endBorder - borderW, 0,
        //        paddingStart + endBorder + 3 * borderW, h, p);
    }

    void drawLines(Canvas canvas, long time, int fromIndex, int toIndex) {
        int paddingStart = getPaddingLeft();
        int paddingEnd = getPaddingRight();
        int paddingTop = getPaddingTop();
        int paddingBottom = getPaddingBottom();

        int w = getWidth() - paddingStart - paddingEnd - 2 * borderW;
        int h = (getHeight() - paddingBottom - paddingTop - Utils.dpToPx(2));

        Data.Column columnX = data.columns[0];

        for (int i = fromIndex; i < toIndex; ++i) {
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
        }

        for (int i = 1; i < data.columns.length; i++) {
            paths[i - 1].reset();
            float[] coord = coords[i];
            int endedOn = fromIndex;
            for (int j = fromIndex; j < toIndex; j += stepIndex) {
                float x = Math.min(getWidth() - paddingEnd - borderW,
                        Math.max(paddingStart + borderW,
                                paddingStart + borderW + w * (columnX.value[j] - minX) * 1f / (maxX
                                        - minX)));
                if (j == fromIndex) {
                    paths[i - 1].moveTo(x, Utils.dpToPx(1) + paddingTop + convertToY(h, coord[j]));
                } else {
                    paths[i - 1].lineTo(x, Utils.dpToPx(1) + paddingTop + convertToY(h, coord[j]));
                }
                endedOn = j;
            }

            {
                float x = Math.min(getWidth() - paddingEnd - borderW,
                        Math.max(paddingStart + borderW, paddingStart
                                + borderW
                                + w * (columnX.value[toIndex - 1] - minX) * 1f / (maxX - minX)));
                paths[i - 1].lineTo(x,
                        Utils.dpToPx(1) + paddingTop + convertToY(h, coord[toIndex - 1]));
            }
            coord = coords[i - 1];
            {
                float x = Math.min(getWidth() - paddingEnd - borderW,
                        Math.max(paddingStart + borderW, paddingStart
                                + borderW
                                + w * (columnX.value[toIndex - 1] - minX) * 1f / (maxX - minX)));
                paths[i - 1].lineTo(x,
                        Utils.dpToPx(1) + paddingTop + convertToY(h, coord[toIndex - 1]));
            }
            for (int j = endedOn; j >= fromIndex; j -= stepIndex) {
                float x = Math.min(getWidth() - paddingEnd - borderW,
                        Math.max(paddingStart + borderW,
                                paddingStart + borderW + w * (columnX.value[j] - minX) * 1f / (maxX
                                        - minX)));
                paths[i - 1].lineTo(x, Utils.dpToPx(1) + paddingTop + convertToY(h, coord[j]));
            }
        }

        for (int k = 1; k < data.columns.length; k++) {
            p.setColor(data.columns[k].color);
            canvas.drawPath(paths[k - 1], p);
        }
    }

    void drawLinesDetailed(Canvas canvas, long time, int fromIndex, int toIndex) {
        int paddingStart = getPaddingLeft();
        int paddingEnd = getPaddingRight();
        int paddingTop = getPaddingTop();
        int paddingBottom = getPaddingBottom();

        int w = getWidth() - paddingStart - paddingEnd - 2 * borderW;
        int h = (getHeight() - paddingBottom - paddingTop - Utils.dpToPx(1));

        Data.Column columnX = data.columns[0];

        for (int i = fromIndex; i < toIndex; i += stepIndex) {
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

            long prevMinY = 0;
            for (int j = 1; j < data.columns.length; j++) {
                if (lineDisabled[j]) continue;

                Data.Column column = data.columns[j];

                //Log.v("LineView", "lineToTime.get(j)=" + lineToTime.get(j));
                p.setColor(column.color);

                float y = column.value[i];
                if (lineToTime.get(j) != null) {
                    //int alpha;
                    if (lineToUp.get(j)) {
                        float k = Math.min(1,
                                Math.max(0, (time - lineToTime.get(j)) * 1f / ANIMATION_DURATION));
                        y *= k;
                    } else {
                        float k = Math.min(1, Math.max(0,
                                1 + (lineToTime.get(j) - time) * 1f / ANIMATION_DURATION));
                        y *= k;
                    }
                }

                float percentage = y * 100f / sum;

                float startX = Math.min(getWidth() - paddingEnd - borderW,
                        Math.max(paddingStart + borderW,
                                paddingStart + borderW + w * (columnX.value[i] - minX) * 1f / (maxX
                                        - minX)));
                float stopX = Math.min(getWidth() - paddingEnd - borderW,
                        Math.max(paddingStart + borderW, paddingStart
                                + borderW
                                + w * (columnX.value[Math.min(i + stepIndex,
                                data.columns[0].value.length - 1)] - minX) * 1f / (maxX - minX)));
                float startY = Utils.dpToPx(1) + paddingTop + convertToY(h, prevMinY + percentage);
                if (j == data.columns.length - 1) {
                    startY = Utils.dpToPx(1) + paddingTop + convertToY(h, 100);
                }

                float stopY = Utils.dpToPx(1) + paddingTop + convertToY(h, prevMinY);

                canvas.drawRect((float) Math.floor(startX), startY, (float) Math.ceil(stopX), stopY,
                        p);

                prevMinY += percentage;
            }
        }
    }

    private float convertToY(int h, float y) {
        return h - h * y * 1f / (maxY);
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

    public void setData(Data data) {
        this.data = data;
        lineDisabled = new boolean[data.columns.length];

        Data.Column columnX = data.columns[0];
        fromIndex = 0;
        toIndex = columnX.value.length - 1;
        minX = columnX.value[0];
        maxX = columnX.value[columnX.value.length - 1];

        maxY = 100;

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

    public void setDetailed(long time) {
        oldFrom = fromX;
        oldTo = toX;

        state = ANIMATING;
        final long newMinX = time - 1000 * 60 * 60 * 24 * 3;
        final long newMaxX = time + 1000 * 60 * 60 * 24 * 4;

        PropertyValuesHolder minProp = PropertyValuesHolder.ofFloat("min", minX, newMinX);
        PropertyValuesHolder maxProp = PropertyValuesHolder.ofFloat("max", maxX, newMaxX);
        PropertyValuesHolder fromProp = PropertyValuesHolder.ofFloat("from", fromX, 3f / 7);
        PropertyValuesHolder toProp = PropertyValuesHolder.ofFloat("to", toX, 4f / 7);
        ValueAnimator valueAnimator =
                ValueAnimator.ofPropertyValuesHolder(minProp, maxProp, fromProp, toProp);
        valueAnimator.setDuration(ANIMATION_DURATION);
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                minX = ((Float) animation.getAnimatedValue("min")).longValue();
                maxX = ((Float) animation.getAnimatedValue("max")).longValue();
                fromX = (float) animation.getAnimatedValue("from");
                toX = (float) animation.getAnimatedValue("to");

                fromIndex = Arrays.binarySearch(data.columns[0].value, minX);
                if (fromIndex < 0) fromIndex = Math.max(-fromIndex - 2, 0);
                toIndex = Arrays.binarySearch(data.columns[0].value, maxX);
                if (toIndex < 0) toIndex = Math.min(-toIndex, data.columns[0].value.length);

                //Log.v("lines", "minX=" + minX + " maxX=" + maxX);

                Data.Column columnX = data.columns[0];
                float diff = 0;
                stepIndex = 0;
                while (diff < getWidth() / 100f) {
                    diff = getWidth() * (columnX.value[++stepIndex] - columnX.value[0]) * 1f / (maxX
                            - minX);
                }

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

                fromIndex = Arrays.binarySearch(data.columns[0].value, newMinX);
                if (fromIndex < 0) fromIndex = Math.max(-fromIndex - 2, 0);
                toIndex = Arrays.binarySearch(data.columns[0].value, newMaxX);
                if (toIndex < 0) toIndex = Math.min(-toIndex, data.columns[0].value.length);

                setFromDetailed(0.49f);
                setToDetailed(0.51f);
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

    int fromDetailedIndex = 0;
    int toDetailedIndex = 0;

    public void setFromDetailed(float from) {
        Data.Column columnX = data.columns[0];
        long minX1 = columnX.value[0];
        long maxX1 = columnX.value[columnX.value.length - 1];

        long fromTime = (long) (columnX.value[fromIndex] + from * (columnX.value[toIndex]
                - columnX.value[fromIndex]));
        long fromLeft = fromTime - 1000 * 60 * 60 * 23;
        fromDetailedIndex = fromIndex;
        int oldIndex = fromDetailedIndex;
        float oldFromXDetailed = fromXDetailed;
        for (int i = fromIndex; i <= toIndex; i++) {
            //Log.v("perc", "from dim=" + (columnX.value[i] - minX) * 1f / (maxX - minX));
            //Log.v("perc", "from left=" + fromLeft);
            //Log.v("perc", "from time=" + fromTime);
            //Log.v("perc", "time=" + columnX.value[i]);
            if (columnX.value[i] > fromLeft && columnX.value[i] < fromTime + 1000 * 60 * 60) {
                fromDetailedIndex = i;
                fromX = (columnX.value[i] - minX1) * 1f / (maxX1 - minX1);
                if (oldIndex != fromDetailedIndex) {
                    fromXDetailed = (columnX.value[i] - minX) * 1f / (maxX - minX);
                }
                break;
            }
        }

        if (oldIndex != fromDetailedIndex) {
            //PropertyValuesHolder fromProp =
            //        PropertyValuesHolder.ofFloat("from", oldFromXDetailed, fromXDetailed);
            //ValueAnimator valueAnimator = ValueAnimator.ofPropertyValuesHolder(fromProp);
            //valueAnimator.setDuration(ANIMATION_DURATION);
            //valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            //    @Override
            //    public void onAnimationUpdate(ValueAnimator animation) {
            //        fromXDetailed = (float) animation.getAnimatedValue("from");
            //        invalidate();
            //    }
            //});
            //valueAnimator.start();

            for (MoveListener listener : listeners) {
                listener.onUpdateFromIndex(fromDetailedIndex);
            }
        }
        log();

        invalidate();
    }

    public void setToDetailed(float to) {
        Data.Column columnX = data.columns[0];
        long minX1 = columnX.value[0];
        long maxX1 = columnX.value[columnX.value.length - 1];

        long toTime = (long) (columnX.value[fromIndex] + to * (columnX.value[toIndex]
                - columnX.value[fromIndex]));
        long toRight = toTime + 1000 * 60 * 60 * 23;
        toDetailedIndex = toIndex;

        int oldIndex = toDetailedIndex;
        float oldToXDetailed = toXDetailed;
        for (int i = fromIndex; i <= toIndex; i++) {
            //Log.v("perc", "to dim=" + (columnX.value[i] - minX) * 1f / (maxX - minX));
            //Log.v("perc", "to left=" + toRight);
            //Log.v("perc", "to time=" + toTime);
            if (columnX.value[i] < toRight && columnX.value[i] > toTime - 1000 * 60 * 60) {
                toDetailedIndex = i;
                toX = (columnX.value[i] - minX1) * 1f / (maxX1 - minX1);
                if (oldIndex != toDetailedIndex) {
                    toXDetailed = (columnX.value[i] - minX) * 1f / (maxX - minX);
                }
                break;
            }
        }

        if (oldIndex != toDetailedIndex) {
            //PropertyValuesHolder toProp =
            //        PropertyValuesHolder.ofFloat("to", oldToXDetailed, toXDetailed);
            //ValueAnimator valueAnimator = ValueAnimator.ofPropertyValuesHolder(toProp);
            //valueAnimator.setDuration(ANIMATION_DURATION);
            //valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            //    @Override
            //    public void onAnimationUpdate(ValueAnimator animation) {
            //        Log.v("perc", "toxdet" + toXDetailed);
            //        toXDetailed = (float) animation.getAnimatedValue("to");
            //        invalidate();
            //    }
            //});
            //valueAnimator.start();

            for (MoveListener listener : listeners) {
                listener.onUpdateToIndex(toDetailedIndex);
            }
        }
        log();

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

        if (state == WHOLE) {
            fromX = Math.max(-fromLimit, from);
        } else if (state == DETAILED) {
            long fromTime = (long) (columnX.value[fromIndex] + from * (columnX.value[toIndex - 1]
                    - columnX.value[fromIndex]));
            long fromLeft = fromTime - 1000 * 60 * 60 * 24;
            for (int i = fromIndex; i < toIndex; i++) {
                if (columnX.value[i] > fromLeft && columnX.value[i] < fromTime) {
                    fromX = (columnX.value[i] - minX1) * 1f / (maxX1 - minX1);
                    fromXDetailed = (columnX.value[i] - minX) * 1f / (maxX - minX);
                    break;
                }
            }
        }

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

        if (state == WHOLE) {
            toX = Math.min(1 + fromLimit, to);
        } else if (state == DETAILED) {
            long toTime = (long) (columnX.value[fromIndex] + to * (columnX.value[toIndex - 1]
                    - columnX.value[fromIndex]));
            long toRight = toTime + 1000 * 60 * 60 * 24;
            for (int i = fromIndex; i < toIndex; i++) {
                if (columnX.value[i] < toRight && columnX.value[i] > toTime) {
                    toX = (columnX.value[i] - minX1) * 1f / (maxX1 - minX1);
                    toXDetailed = (columnX.value[i] - minX) * 1f / (maxX - minX);
                    break;
                }
            }
        }

        //Log.v("BigLineView", "toLimit=" + fromLimit);
        for (MoveListener listener : listeners) {
            listener.onUpdateTo(toX);
        }
        log();

        invalidate();
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
        log();
        invalidate();
    }

    void addListener(MoveListener listener) {
        listeners.add(listener);
    }

    void clearListeners() {
        listeners.clear();
    }

    public void setWhole() {
        state = ANIMATING;
        fromX = (oldFrom);
        toX = (oldTo);
        Data.Column columnX = data.columns[0];
        long newMinX = columnX.value[0];
        long newMaxX = columnX.value[columnX.value.length - 1];

        PropertyValuesHolder minProp = PropertyValuesHolder.ofFloat("min", minX, newMinX);
        PropertyValuesHolder maxProp = PropertyValuesHolder.ofFloat("max", maxX, newMaxX);
        PropertyValuesHolder fromProp = PropertyValuesHolder.ofFloat("from", fromXDetailed, fromX);
        PropertyValuesHolder toProp = PropertyValuesHolder.ofFloat("to", toXDetailed, toX);

        ValueAnimator valueAnimator =
                ValueAnimator.ofPropertyValuesHolder(minProp, maxProp, fromProp, toProp);
        valueAnimator.setDuration(ANIMATION_DURATION);
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                minX = ((Float) animation.getAnimatedValue("min")).longValue();
                maxX = ((Float) animation.getAnimatedValue("max")).longValue();
                fromX = (float) animation.getAnimatedValue("from");
                toX = (float) animation.getAnimatedValue("to");

                fromIndex = Arrays.binarySearch(data.columns[0].value, minX);
                if (fromIndex < 0) fromIndex = Math.max(-fromIndex - 2, 0);
                toIndex = Arrays.binarySearch(data.columns[0].value, maxX);
                if (toIndex < 0) toIndex = Math.min(-toIndex, data.columns[0].value.length);

                //Log.v("lines", "minX=" + minX + " maxX=" + maxX);

                Data.Column columnX = data.columns[0];
                float diff = 0;
                stepIndex = 0;
                while (diff < getWidth() / 100f) {
                    diff = getWidth() * (columnX.value[++stepIndex] - columnX.value[0]) * 1f / (maxX
                            - minX);
                }

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

        //minFraction = 12 * 60 * 60 * 1000f / (maxX - minX);

        //setFrom(0f);
        //setTo(1f);

        invalidate();
    }

    interface MoveListener {
        void onUpdateFrom(float from);

        void onUpdateFromIndex(int from);

        void onUpdateTo(float to);

        void onUpdateToIndex(int to);
    }
}
