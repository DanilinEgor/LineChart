package com.kuxurum.smoothlinechart;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

public class CheckBox extends View {
    private boolean isChecked = false;
    private Paint bgp, bp, p, tp, tp2;
    private Path path = new Path();
    private ValueAnimator progressAnimator = new ValueAnimator();
    private RectF rectF = new RectF();
    private float _2dp;
    private float _16dp;
    private float _18dp;
    private float _21dp;
    private float progress = 0f;
    String text = "";

    public CheckBox(Context context) {
        super(context);
        init();
    }

    public CheckBox(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CheckBox(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int desiredW =
                (int) (Utils.dpToPx(18 + 4) + getPaddingLeft() + getPaddingRight() + tp.measureText(
                        text));

        Paint.FontMetrics fm = tp.getFontMetrics();
        int textH = (int) (fm.descent - fm.ascent);
        int desiredH = Math.max(Utils.dpToPx(18), textH) + getPaddingTop() + getPaddingBottom();
        setMeasuredDimension(resolveSize(desiredW, widthMeasureSpec),
                resolveSize(desiredH, heightMeasureSpec));
    }

    private void init() {
        _2dp = Utils.dpToPx(2);
        _16dp = Utils.dpToPx(16);
        _18dp = Utils.dpToPx(18);
        _21dp = Utils.dpToPx(21);

        bp = new Paint(Paint.ANTI_ALIAS_FLAG);
        bp.setStyle(Paint.Style.FILL);

        bgp = new Paint(Paint.ANTI_ALIAS_FLAG);
        bgp.setStyle(Paint.Style.FILL);

        p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(Color.WHITE);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(_2dp);

        tp = new Paint(Paint.ANTI_ALIAS_FLAG);
        tp.setColor(Color.WHITE);
        tp.setTextSize(Utils.dpToPx(16));

        tp2 = new Paint(Paint.ANTI_ALIAS_FLAG);
        tp2.setTextSize(Utils.dpToPx(16));
    }

    public void setBgColor(int color) {
        bgp.setColor(color);
        invalidate();
    }

    public void setCheckedColor(int checkedColor) {
        bp.setColor(checkedColor);
        tp2.setColor(checkedColor);
        invalidate();
    }

    public void setChecked(boolean checked) {
        if (checked == isChecked) return;
        isChecked = checked;

        if (progressAnimator.isStarted()) {
            progressAnimator.cancel();
        }

        progressAnimator = ValueAnimator.ofFloat(isChecked ? 0 : 1, isChecked ? 1 : 0);
        progressAnimator.setDuration(200);
        progressAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                progress = (float) animation.getAnimatedValue();
                invalidate();
            }
        });
        progressAnimator.start();
    }

    public boolean isChecked() {
        return isChecked;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int w = getWidth();
        int h = getHeight();

        float b;
        if (progress <= 0.5f) {
            b = 2 * _2dp * progress;
        } else {
            b = 2 * _2dp * (1 - progress);
        }

        rectF.set(b, b, w - b, h - b);
        canvas.drawRoundRect(rectF, _21dp, _21dp, bp);

        if (isChecked) {
            if (progress <= 0.5f) {
                float left = _2dp + progress * (w - 2 * _2dp);
                float top = _2dp + progress * (h - 2 * _2dp);
                float right = (2 * _2dp - w) * progress + w - _2dp;
                float bottom = (2 * _2dp - h) * progress + h - _2dp;
                rectF.set(left, top, right, bottom);
                canvas.drawRoundRect(rectF, _21dp, _21dp, bgp);
            } else {
                path.reset();
                float x1 = (0.42f - 0.28f * 2 * (progress - 0.5f)) * _18dp;
                float y1 = (0.75f - 0.28f * 2 * (progress - 0.5f)) * _18dp;
                float x2 = (0.42f + 0.44f * 2 * (progress - 0.5f)) * _18dp;
                float y2 = (0.75f - 0.5f * 2 * (progress - 0.5f)) * _18dp;

                path.moveTo(getPaddingLeft() + x1, getPaddingTop() + y1);
                path.lineTo(getPaddingLeft() + 0.42f * _18dp, getPaddingTop() + 0.75f * _18dp);
                path.lineTo(getPaddingLeft() + x2, getPaddingTop() + y2);
                canvas.drawPath(path, p);
            }
            Paint.FontMetrics fm = tp.getFontMetrics();
            canvas.drawText(text, getPaddingLeft() + Utils.dpToPx(13 + progress * 9),
                    getPaddingTop() - fm.ascent, tp);
        } else {
            float left = _2dp + progress * (w / 2f - _2dp);
            float top = _2dp + progress * (h / 2f - _2dp);
            float right = (_2dp - w / 2f) * progress + w - _2dp;
            float bottom = (_2dp - h / 2f) * progress + h - _2dp;
            rectF.set(left, top, right, bottom);
            canvas.drawRoundRect(rectF, _21dp, _21dp, bgp);
            Paint.FontMetrics fm = tp.getFontMetrics();
            canvas.drawText(text, getPaddingLeft() + Utils.dpToPx(13 + progress * 9),
                    getPaddingTop() - fm.ascent, tp2);
        }
    }
}
