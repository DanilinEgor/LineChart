package com.kuxurum.smoothlinechart;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.Nullable;

public class BaseBigLineView extends View {
    protected Paint fp, fp2, fp3;
    protected RectF[] borderLineRect = new RectF[2];
    protected RectF[] borderRect = new RectF[2];
    protected Path borderPath = new Path();
    protected int borderW;

    public BaseBigLineView(Context context) {
        super(context);
        init();
    }

    public BaseBigLineView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public BaseBigLineView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    void init() {
        borderW = Utils.dpToPx(8);

        borderLineRect[0] = new RectF();
        borderLineRect[1] = new RectF();
        borderRect[0] = new RectF();
        borderRect[1] = new RectF();

        fp = new Paint();
        fp.setStyle(Paint.Style.FILL);

        fp2 = new Paint();
        fp2.setStyle(Paint.Style.FILL);

        fp3 = new Paint();
        fp3.setStyle(Paint.Style.FILL);
        fp3.setColor(Color.WHITE);
    }

    void setChartForegroundColor(int color) {
        fp.setColor(color);
    }

    void setChartForegroundBorderColor(int color) {
        fp2.setColor(color);
    }

    protected void drawBorder(Canvas canvas, float startBorder, float endBorder, int paddingStart,
            int paddingTop, int paddingEnd, int paddingBottom) {
        canvas.drawRect(paddingStart + borderW, paddingTop + Utils.dpToPx(1),
                paddingStart + startBorder + borderW, getHeight() - paddingBottom - Utils.dpToPx(1),
                fp);
        canvas.drawRect(paddingStart + endBorder - borderW, paddingTop + Utils.dpToPx(1),
                getWidth() - paddingEnd - borderW, getHeight() - paddingBottom - Utils.dpToPx(1),
                fp);

        int h = getHeight() - paddingBottom - paddingTop;

        canvas.drawRect(paddingStart + startBorder + borderW, paddingTop,
                paddingStart + endBorder - borderW, paddingTop + Utils.dpToPx(1), fp2);
        canvas.drawRect(paddingStart + startBorder + borderW,
                getHeight() - paddingBottom - Utils.dpToPx(1), paddingStart + endBorder - borderW,
                getHeight() - paddingBottom, fp2);

        borderLineRect[0].set(paddingStart + startBorder + borderW / 2f - Utils.dpToPx(1),
                paddingTop + h / 2f - Utils.dpToPx(6),
                paddingStart + startBorder + borderW / 2f + Utils.dpToPx(1),
                paddingTop + h / 2f + Utils.dpToPx(6));

        borderLineRect[1].set(paddingStart + endBorder - borderW / 2f - Utils.dpToPx(1),
                paddingTop + h / 2f - Utils.dpToPx(6),
                paddingStart + endBorder - borderW / 2f + Utils.dpToPx(1),
                paddingTop + h / 2f + Utils.dpToPx(6));

        borderPath.reset();
        borderPath.moveTo(paddingStart + startBorder + borderW, paddingTop + h);
        borderRect[0].set(paddingStart + startBorder, paddingTop + h - 2 * borderW,
                paddingStart + startBorder + 2 * borderW, paddingTop + h);
        borderPath.arcTo(borderRect[0], 90, 90, false);
        borderRect[1].set(paddingStart + startBorder, paddingTop,
                paddingStart + startBorder + 2 * borderW, paddingTop + 2 * borderW);
        borderPath.arcTo(borderRect[1], 180, 90, false);
        canvas.drawPath(borderPath, fp2);

        borderPath.reset();
        borderPath.moveTo(paddingStart + endBorder - borderW, paddingTop);
        borderRect[0].set(paddingStart + endBorder - 2 * borderW, paddingTop,
                paddingEnd + endBorder, paddingTop + 2 * borderW);
        borderPath.arcTo(borderRect[0], 270, 90, false);
        borderRect[1].set(paddingStart + endBorder - 2 * borderW, paddingTop + h - 2 * borderW,
                paddingStart + endBorder, paddingTop + h);
        borderPath.arcTo(borderRect[1], 0, 90, false);
        canvas.drawPath(borderPath, fp2);

        canvas.drawRoundRect(borderLineRect[0], Utils.dpToPx(2), Utils.dpToPx(2), fp3);
        canvas.drawRoundRect(borderLineRect[1], Utils.dpToPx(2), Utils.dpToPx(2), fp3);
    }
}
