package com.kuxurum.smoothlinechart;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewConfiguration;
import androidx.annotation.Nullable;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;

public class BaseLineView extends View {
    protected static int TAP_TIMEOUT = ViewConfiguration.getTapTimeout();

    protected Paint p;
    protected Paint axisTextP;
    protected Paint xTextP;
    protected Paint axisP;
    protected Paint vertAxisP;
    protected Paint shadowP;
    protected Paint labelPressedBackgroundP;
    protected Paint labelP;
    protected Paint dateLabelP;
    protected Paint dataPercP;
    protected Paint dataValueP;
    protected Paint dataNameP;
    protected Paint maskP;
    protected Paint circleLabelP;
    protected Paint titleP;
    protected Paint dataLabelArrowP;
    protected Paint titleDateP;

    protected int zoomOutColor;
    protected int titleColor;
    protected int maskColor;
    protected int axisColor;
    protected int axisColorDark;

    protected Path arrowPath = new Path();

    protected RectF labelRectF = new RectF();
    protected RectF zoomOutRectF = new RectF();
    protected RectF shadowRectF = new RectF();

    protected SimpleDateFormat titleDateFormat =
            new SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.US);
    protected SimpleDateFormat shortDateFormat = new SimpleDateFormat("dd MMMM yyyy", Locale.US);
    protected SimpleDateFormat labelDateFormat =
            new SimpleDateFormat("EEE, dd MMM yyyy", Locale.US);
    protected SimpleDateFormat detailedLabelDateFormat =
            new SimpleDateFormat("EEE, dd MMM yyyy, HH:mm", Locale.US);
    protected Date date = new Date();
    protected Calendar calendar = GregorianCalendar.getInstance();

    protected int titleMargin;

    protected boolean labelPressed, labelShown, labelWasShown;
    protected long touchStart = 0;

    protected String[] axisTexts = new String[6];
    private StringBuilder builder = new StringBuilder();

    public BaseLineView(Context context) {
        super(context);
        init();
    }

    public BaseLineView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public BaseLineView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    void init() {
        titleMargin = Utils.dpToPx(8);

        p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setStyle(Paint.Style.FILL);

        axisP = new Paint(Paint.ANTI_ALIAS_FLAG);
        axisP.setStrokeWidth(Utils.dpToPx(1));

        vertAxisP = new Paint(Paint.ANTI_ALIAS_FLAG);
        vertAxisP.setStrokeWidth(Utils.dpToPx(1.5f));

        axisTextP = new Paint(Paint.ANTI_ALIAS_FLAG);
        axisTextP.setTextSize(Utils.dpToPx(12));

        xTextP = new Paint(Paint.ANTI_ALIAS_FLAG);
        xTextP.setTextSize(Utils.dpToPx(12));
        xTextP.setTextAlign(Paint.Align.CENTER);

        shadowP = new Paint(Paint.ANTI_ALIAS_FLAG);
        shadowP.setColor(Color.parseColor("#40000000"));
        shadowP.setStyle(Paint.Style.FILL);

        labelPressedBackgroundP = new Paint(Paint.ANTI_ALIAS_FLAG);
        labelPressedBackgroundP.setColor(Color.parseColor("#10000000"));
        labelPressedBackgroundP.setStyle(Paint.Style.FILL);

        labelP = new Paint(Paint.ANTI_ALIAS_FLAG);
        labelP.setShadowLayer(4, 0, 0, Color.parseColor("#40000000"));
        labelP.setStyle(Paint.Style.FILL);

        dateLabelP = new Paint(Paint.ANTI_ALIAS_FLAG);
        dateLabelP.setTextSize(Utils.dpToPx(14));
        dateLabelP.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));

        dataPercP = new Paint(Paint.ANTI_ALIAS_FLAG);
        dataPercP.setTextSize(Utils.dpToPx(14));
        dataPercP.setFakeBoldText(true);

        dataValueP = new Paint(Paint.ANTI_ALIAS_FLAG);
        dataValueP.setTextSize(Utils.dpToPx(14));
        dataValueP.setFakeBoldText(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            dataValueP.setLetterSpacing(-0.025f);
        }

        dataNameP = new Paint(Paint.ANTI_ALIAS_FLAG);
        dataNameP.setTextSize(Utils.dpToPx(14));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            dataNameP.setLetterSpacing(-0.025f);
        }

        maskP = new Paint(Paint.ANTI_ALIAS_FLAG);
        maskP.setColor(maskColor);
        maskP.setStyle(Paint.Style.FILL);

        circleLabelP = new Paint(Paint.ANTI_ALIAS_FLAG);
        circleLabelP.setColor(Color.WHITE);
        circleLabelP.setFakeBoldText(true);

        titleP = new Paint(Paint.ANTI_ALIAS_FLAG);
        titleP.setTextSize(Utils.dpToPx(16));
        titleP.setFakeBoldText(true);

        titleDateP = new Paint(Paint.ANTI_ALIAS_FLAG);
        titleDateP.setTextSize(Utils.dpToPx(13));
        titleDateP.setFakeBoldText(true);

        dataLabelArrowP = new Paint(Paint.ANTI_ALIAS_FLAG);
        dataLabelArrowP.setStrokeCap(Paint.Cap.ROUND);
        dataLabelArrowP.setStyle(Paint.Style.STROKE);
        dataLabelArrowP.setStrokeWidth(Utils.dpToPx(2));
    }

    void setMaskColor(int color) {
        maskColor = color;
        maskP.setColor(maskColor);
    }

    void setAxisColor(int color) {
        axisColor = color;
    }

    void setAxisColorDark(int color) {
        axisColorDark = color;
    }

    void setVertAxisColor(int color) {
        vertAxisP.setColor(color);
    }

    void setAxisTextColor(int color) {
        axisTextP.setColor(color);
    }

    void setXAxisTextColor(int color) {
        xTextP.setColor(color);
    }

    void setChartLabelColor(int color) {
        labelP.setColor(color);
    }

    void setChartDateLabelColor(int color) {
        dateLabelP.setColor(color);
    }

    void setChartLabelDataNameColor(int color) {
        dataPercP.setColor(color);
        dataNameP.setColor(color);
    }

    void setDataLabelArrowColor(int color) {
        dataLabelArrowP.setColor(color);
    }

    void setZoomOutColor(int color) {
        zoomOutColor = color;
    }

    void setTitleColor(int color) {
        titleColor = color;
        titleDateP.setColor(color);
    }

    protected void drawArrow(Canvas canvas, float y0, float x0) {
        arrowPath.reset();
        arrowPath.moveTo(x0, y0 + Utils.dpToPx(4));
        arrowPath.lineTo(x0 + Utils.dpToPx(4), y0 + Utils.dpToPx(8));
        arrowPath.lineTo(x0, y0 + Utils.dpToPx(12));
        canvas.drawPath(arrowPath, dataLabelArrowP);
    }

    protected void getAxisTexts(long maxValue, long minValue) {
        if (maxValue > 1_000_000_000) {
            for (int i = 0; i < 6; i++) {
                long v = (minValue + (maxValue - minValue) / 5 * i) / 10_000_000;
                if (v % 100 != 0) {
                    axisTexts[i] = String.format(Locale.US, "%d.%02dB", v / 100, v % 100);
                } else {
                    axisTexts[i] = String.format(Locale.US, "%dB", v / 100);
                }
            }
        } else if (maxValue > 1_000_000) {
            for (int i = 0; i < 6; i++) {
                long v = (minValue + (maxValue - minValue) * i / 5) / 10_000;
                if (v % 100 != 0) {
                    axisTexts[i] = String.format(Locale.US, "%d.%02dM", v / 100, v % 100);
                } else {
                    axisTexts[i] = String.format(Locale.US, "%dM", v / 100);
                }
            }
        } else if (maxValue > 1_000) {
            for (int i = 0; i < 6; i++) {
                long v = (minValue + (maxValue - minValue) * i / 5) / 10;
                if (v % 100 != 0) {
                    axisTexts[i] = String.format(Locale.US, "%d.%02dK", v / 100, v % 100);
                } else {
                    axisTexts[i] = String.format(Locale.US, "%dK", v / 100);
                }
            }
        } else {
            for (int i = 0; i < 6; i++) {
                long v = minValue + (maxValue - minValue) / 5 * i;
                axisTexts[i] = String.valueOf(v);
            }
        }
    }

    protected String formatForLabel(long value) {
        if (value > 1_000_000) {
            return String.format(Locale.US, "%d %03d %03d", value / 1_000_000,
                    value / 1_000 % 1_000, value % 1_000);
        } else if (value > 1_000) {
            return String.format(Locale.US, "%d %03d", value / 1_000 % 1_000, value % 1_000);
        } else {
            return String.valueOf(value);
        }
    }

    protected String formatTime(long time) {
        calendar.setTimeInMillis(time);
        return String.format(Locale.US, "%02d:%02d", calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE));
    }

    protected String formatDate(long time) {
        calendar.setTimeInMillis(time);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        builder.setLength(0);
        switch (month) {
            case Calendar.JANUARY:
                builder.append("Jan ");
                break;
            case Calendar.FEBRUARY:
                builder.append("Feb ");
                break;
            case Calendar.MARCH:
                builder.append("Mar ");
                break;
            case Calendar.APRIL:
                builder.append("Apr ");
                break;
            case Calendar.MAY:
                builder.append("May ");
                break;
            case Calendar.JUNE:
                builder.append("Jun ");
                break;
            case Calendar.JULY:
                builder.append("Jul ");
                break;
            case Calendar.AUGUST:
                builder.append("Aug ");
                break;
            case Calendar.SEPTEMBER:
                builder.append("Sep ");
                break;
            case Calendar.OCTOBER:
                builder.append("Oct ");
                break;
            case Calendar.NOVEMBER:
                builder.append("Nov ");
                break;
            case Calendar.DECEMBER:
                builder.append("Dec ");
                break;
        }
        builder.append(day);
        return builder.toString();
    }

    protected static boolean isSameDay(long time1, long time2) {
        Calendar cal1 = Calendar.getInstance();
        cal1.setTimeInMillis(time1);
        Calendar cal2 = Calendar.getInstance();
        cal2.setTimeInMillis(time2);
        return (cal1.get(Calendar.ERA) == cal2.get(Calendar.ERA)
                && cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR)
                && cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR));
    }
}
