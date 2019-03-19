package com.kuxurum.smoothlinechart;

import android.content.Context;
import android.content.res.Resources;
import android.util.TypedValue;

class Utils {
    private static Utils instance;
    private Resources res;

    private Utils(Context context) {
        res = context.getResources();
    }

    static int dpToPx(float dip) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dip,
                instance.res.getDisplayMetrics());
    }

    static void init(Context context) {
        if (instance == null) {
            instance = new Utils(context);
        }
    }
}
