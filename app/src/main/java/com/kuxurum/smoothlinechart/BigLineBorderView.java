package com.kuxurum.smoothlinechart;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import java.util.ArrayList;
import java.util.List;

public class BigLineBorderView extends View {
    private Paint fp, fp2;

    private float fromX, toX;
    private int borderW;
    private ViewConfiguration vc;

    public BigLineBorderView(Context context) {
        super(context);
        init();
    }

    public BigLineBorderView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public BigLineBorderView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        vc = ViewConfiguration.get(getContext());

        setLayerType(LAYER_TYPE_HARDWARE, null);
        borderW = Utils.dpToPx(6);

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
                        Log.v("BigLineBorderView", "pressed startBorder");
                    } else if (event.getX() > endBorder - borderW
                            && event.getX() < endBorder + 2 * borderW) {
                        isEndPressed = true;
                        Log.v("BigLineBorderView", "pressed endBorder");
                    } else if (event.getX() > startBorder + borderW
                            && event.getX() < endBorder - borderW) {
                        isInsidePressed = true;
                        Log.v("BigLineBorderView", "pressed inside");
                    } else {
                        Log.v("BigLineBorderView", "pressed outside");
                    }
                    startPressX = event.getX();
                    startFromX = fromX;
                    startToX = toX;
                    Log.v("BigLineBorderView", "startPressX=" + startPressX);
                    Log.v("BigLineBorderView", "startFromX=" + startFromX);
                    Log.v("BigLineBorderView", "startBorder=" + startBorder);
                } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
                    if (Math.abs(event.getY() - startY) > Utils.dpToPx(30)) {
                        getParent().requestDisallowInterceptTouchEvent(false);
                    }

                    Log.v("BigLineBorderView", "event.getX()=" + event.getX());
                    //Log.v("BigLineBorderView", "toX=" + toX);
                    //Log.v("BigLineBorderView", "fromX=" + fromX);
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

        Log.v("BigLineBorderView", "time=" + (System.currentTimeMillis() - time) + "ms");
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        Log.v("onMeasure w", MeasureSpec.toString(widthMeasureSpec));
        Log.v("onMeasure h", MeasureSpec.toString(heightMeasureSpec));
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    public void setFrom(float from) {
        fromX = Math.max(0, from);
        for (MoveListener listener : listeners) {
            listener.onUpdateFrom(fromX);
        }

        invalidate();
    }

    public void setTo(float to) {
        toX = Math.min(1, to);
        for (MoveListener listener : listeners) {
            listener.onUpdateTo(toX);
        }

        invalidate();
    }

    private List<MoveListener> listeners = new ArrayList<>();

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
