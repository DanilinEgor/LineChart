package com.kuxurum.smoothlinechart;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;

public class CheckBox extends View {
    private boolean isChecked;
    private Paint bp, p;
    private int checkedColor;
    private Path path = new Path();

    private float _2dp;

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

    private void init() {
        _2dp = Utils.dpToPx(2);

        checkedColor = Color.parseColor("#ff0000");

        bp = new Paint();
        bp.setColor(checkedColor);
        bp.setAntiAlias(true);

        p = new Paint();
        p.setColor(Color.WHITE);
        p.setStyle(Paint.Style.STROKE);
        p.setAntiAlias(true);
        p.setStrokeWidth(_2dp);
    }

    public void setCheckedColor(int checkedColor) {
        this.checkedColor = checkedColor;
        bp.setColor(checkedColor);
        invalidate();
    }

    public void setChecked(boolean checked) {
        if (checked == isChecked) return;
        isChecked = checked;
        invalidate();
    }

    public boolean isChecked() {
        return isChecked;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (isChecked) {
            bp.setStyle(Paint.Style.FILL_AND_STROKE);
        } else {
            bp.setStyle(Paint.Style.STROKE);
            bp.setStrokeWidth(_2dp);
        }
        int w = getWidth();
        int h = getHeight();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            canvas.drawRoundRect(0, 0, w, h, _2dp, _2dp, bp);
        } else {
            canvas.drawRect(0, 0, w, h, bp);
        }

        if (isChecked) {
            path.reset();
            path.moveTo(0.14f * w, 0.47f * h);
            path.lineTo(0.42f * w, 0.75f * h);
            path.lineTo(0.86f * w, 0.25f * w);
            canvas.drawPath(path, p);
        }
    }
}
