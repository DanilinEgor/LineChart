package com.kuxurum.smoothlinechart;

import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {
    private Data[] datas;
    private List<LinearLayout> roots = new ArrayList<>();
    private List<LineView> lineViews = new ArrayList<>();
    private List<BigLineView> bigLineViews = new ArrayList<>();
    private List<CheckBox> checkBoxes = new ArrayList<>();
    private List<TextView> textViews = new ArrayList<>();
    private List<View> dividers = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Utils.init(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        float[] to;
        float[] from;
        boolean[] checked;
        try {
            datas = DataParser.parse(this);
        } catch (Exception e) {
            Log.e("LineView", "Exception: ", e);
        }
        from = new float[datas.length];
        to = new float[datas.length];
        for (int i = 0; i < from.length; i++) {
            from[i] = 0.75f;
            to[i] = 1f;
        }

        int linesCount = 0;
        for (Data data : datas) {
            linesCount += data.columns.length;
        }
        checked = new boolean[linesCount];
        for (int i = 0; i < linesCount; i++) {
            checked[i] = true;
        }

        ViewGroup mainRoot = findViewById(R.id.layout_root);

        int orientation = getResources().getConfiguration().orientation;
        boolean isPortrait = orientation != Configuration.ORIENTATION_LANDSCAPE;

        int counter = 0;
        for (int j = 0; j < datas.length; j++) {
            Data data = datas[j];

            LinearLayout root = new LinearLayout(this);
            roots.add(root);
            {
                LinearLayout.LayoutParams params =
                        new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT);
                if (j != 0) {
                    params.topMargin = Utils.dpToPx(24);
                }
                root.setLayoutParams(params);
                root.setOrientation(LinearLayout.VERTICAL);
            }

            final LineView lineView = new LineView(this);
            lineViews.add(lineView);
            {
                lineView.setLayoutParams(
                        new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                isPortrait ? Utils.dpToPx(400) : Utils.dpToPx(200)));
                lineView.setData(data);
                //lineView.setLineEnabled(counter - linesOffset, checked[counter]);
            }
            root.addView(lineView);

            //FrameLayout frameLayout = new FrameLayout(this);
            //{
            //    LinearLayout.LayoutParams params =
            //            new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
            //                    Utils.dpToPx(50));
            //    params.topMargin = Utils.dpToPx(8);
            //    params.leftMargin = Utils.dpToPx(24);
            //    params.rightMargin = Utils.dpToPx(24);
            //    frameLayout.setLayoutParams(params);
            //}
            //root.addView(frameLayout);

            final BigLineView bigLineView = new BigLineView(this);
            bigLineViews.add(bigLineView);
            {
                LinearLayout.LayoutParams params =
                        new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                Utils.dpToPx(50));

                params.topMargin = Utils.dpToPx(8);
                params.leftMargin = Utils.dpToPx(12);
                params.rightMargin = Utils.dpToPx(12);

                bigLineView.setLayoutParams(params);
                bigLineView.setData(data);
                bigLineView.addListener(new BigLineView.MoveListener() {
                    @Override
                    public void onUpdateFrom(float from) {
                        lineView.setFrom(from);
                    }

                    @Override
                    public void onUpdateTo(float to) {
                        lineView.setTo(to);
                    }
                });
                bigLineView.setTo(to[j]);
                bigLineView.setFrom(from[j]);
            }
            root.addView(bigLineView);

            //final BigLineBorderView bigLineBorderView = new BigLineBorderView(this);
            //bigLineBorderViews.add(bigLineBorderView);
            //{
            //    ViewGroup.LayoutParams params =
            //            new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
            //                    ViewGroup.LayoutParams.MATCH_PARENT);
            //    bigLineBorderView.setLayoutParams(params);
            //    bigLineBorderView.addListener(new BigLineBorderView.MoveListener() {
            //        @Override
            //        public void onUpdateFrom(float from) {
            //            lineView.setFrom(from);
            //        }
            //
            //        @Override
            //        public void onUpdateTo(float to) {
            //            lineView.setTo(to);
            //        }
            //    });
            //    bigLineBorderView.setTo(to[j]);
            //    bigLineBorderView.setFrom(from[j]);
            //}
            //frameLayout.addView(bigLineBorderView);

            for (int i = 1; i < data.columns.length; i++) {
                Data.Column column = data.columns[i];
                FrameLayout layout = new FrameLayout(this);
                {
                    LinearLayout.LayoutParams params =
                            new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.WRAP_CONTENT);
                    if (i == 1) {
                        params.topMargin = Utils.dpToPx(8);
                    }

                    layout.setLayoutParams(params);
                    layout.setPadding(0, Utils.dpToPx(16), 0, Utils.dpToPx(16));
                    layout.setBackgroundResource(R.drawable.selectable_background);
                }

                final CheckBox cb = new CheckBox(this);
                checkBoxes.add(cb);
                {
                    FrameLayout.LayoutParams params =
                            new FrameLayout.LayoutParams(Utils.dpToPx(18), Utils.dpToPx(18));
                    params.gravity = Gravity.CENTER_VERTICAL;
                    params.leftMargin = Utils.dpToPx(24);
                    cb.setLayoutParams(params);
                    cb.setCheckedColor(column.color);
                    cb.setChecked(checked[counter++]);
                    layout.addView(cb);
                }

                {
                    TextView tv = new TextView(this);
                    textViews.add(tv);
                    FrameLayout.LayoutParams params =
                            new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                                    ViewGroup.LayoutParams.WRAP_CONTENT);
                    params.leftMargin = Utils.dpToPx(72);
                    params.gravity = Gravity.CENTER_VERTICAL;
                    tv.setLayoutParams(params);
                    tv.setText(column.name);
                    tv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
                    layout.addView(tv);
                }

                root.addView(layout);
                final int finalI = i;
                layout.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        cb.setChecked(!cb.isChecked());
                        lineView.setLineEnabled(finalI, cb.isChecked());
                        bigLineView.setLineEnabled(finalI, cb.isChecked());
                    }
                });

                if (i != data.columns.length - 1) {
                    View divider = new View(this);
                    dividers.add(divider);
                    LinearLayout.LayoutParams params =
                            new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                    Utils.dpToPx(1));
                    params.leftMargin = Utils.dpToPx(72);
                    divider.setLayoutParams(params);
                    root.addView(divider);
                }
            }

            mainRoot.addView(root);
        }

        applyColor(false);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_night) {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
            boolean isDay = preferences.getBoolean("day", true);
            preferences.edit().putBoolean("day", !isDay).apply();
            applyColor(true);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void applyColor(boolean animate) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean isDay = preferences.getBoolean("day", true);

        Resources res = getResources();
        int textColor = res.getColor(R.color.textColorPrimary);
        int nightTextColor = res.getColor(R.color.night_textColorPrimary);
        int chartBg = res.getColor(R.color.chartBackground);
        int nightChartBg = res.getColor(R.color.night_chartBackground);
        int windowBg = res.getColor(R.color.colorWindowBackground);
        int nightWindowBg = res.getColor(R.color.night_colorWindowBackground);
        int axisColor = res.getColor(R.color.axisColor);
        int nightAxisColor = res.getColor(R.color.night_axisColor);
        int axisColorDark = res.getColor(R.color.axisColorDark);
        int nightAxisColorDark = res.getColor(R.color.night_axisColorDark);
        int vertAxisColor = res.getColor(R.color.vertAxisColor);
        int nightVertAxisColor = res.getColor(R.color.night_vertAxisColor);
        int axisTextColor = res.getColor(R.color.axisTextColor);
        int nightAxisTextColor = res.getColor(R.color.night_axisTextColor);
        int chartLabelBackground = res.getColor(R.color.chartLabelBackground);
        int nightChartLabelBackground = res.getColor(R.color.night_chartLabelBackground);
        int chartDateLabelColor = res.getColor(R.color.chartDateLabelColor);
        int nightChartDateLabelColor = res.getColor(R.color.night_chartDateLabelColor);
        int foregroundColor = res.getColor(R.color.foregroundColor);
        int nightForegroundColor = res.getColor(R.color.night_foregroundColor);
        int foregroundBorderColor = 0x32507da1; //res.getColor(R.color.foregroundBorderColor);
        int nightForegroundBorderColor = res.getColor(R.color.night_foregroundBorderColor);
        int colorPrimary = 0xff507da1; //res.getColor(R.color.colorPrimary);
        int nightColorPrimary = res.getColor(R.color.night_colorPrimary);
        int colorPrimaryDark = res.getColor(R.color.colorPrimaryDark);
        int nightColorPrimaryDark = res.getColor(R.color.night_colorPrimaryDark);

        final ColorDrawable windowDrawable = new ColorDrawable();
        final ColorDrawable actionBarDrawable = new ColorDrawable();
        getWindow().setBackgroundDrawable(windowDrawable);
        getActionBar().setBackgroundDrawable(actionBarDrawable);

        PropertyValuesHolder textColorP =
                generateProperty("textColor", isDay, textColor, nightTextColor);
        PropertyValuesHolder chartBgP = generateProperty("chartBg", isDay, chartBg, nightChartBg);
        PropertyValuesHolder windowBgP =
                generateProperty("windowBg", isDay, windowBg, nightWindowBg);
        PropertyValuesHolder axisColorP =
                generateProperty("axisColor", isDay, axisColor, nightAxisColor);
        PropertyValuesHolder axisColorDarkP =
                generateProperty("axisColorDark", isDay, axisColorDark, nightAxisColorDark);
        PropertyValuesHolder vertAxisColorP =
                generateProperty("vertAxisColor", isDay, vertAxisColor, nightVertAxisColor);
        PropertyValuesHolder axisTextColorP =
                generateProperty("axisTextColor", isDay, axisTextColor, nightAxisTextColor);
        PropertyValuesHolder chartLabelBackgroundP =
                generateProperty("chartLabelBackground", isDay, chartLabelBackground,
                        nightChartLabelBackground);
        PropertyValuesHolder chartDateLabelColorP =
                generateProperty("chartDateLabelColor", isDay, chartDateLabelColor,
                        nightChartDateLabelColor);
        PropertyValuesHolder foregroundColorP =
                generateProperty("foregroundColor", isDay, foregroundColor, nightForegroundColor);
        PropertyValuesHolder foregroundBorderColorP =
                generateProperty("foregroundBorderColor", isDay, foregroundBorderColor,
                        nightForegroundBorderColor);
        PropertyValuesHolder colorPrimaryP =
                generateProperty("colorPrimary", isDay, colorPrimary, nightColorPrimary);
        PropertyValuesHolder colorPrimaryDarkP =
                generateProperty("colorPrimaryDark", isDay, colorPrimaryDark,
                        nightColorPrimaryDark);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        }

        final ValueAnimator anim =
                ValueAnimator.ofPropertyValuesHolder(textColorP, chartBgP, windowBgP, axisColorP,
                        axisColorDarkP, vertAxisColorP, axisTextColorP, chartLabelBackgroundP,
                        chartDateLabelColorP, foregroundColorP, foregroundBorderColorP,
                        colorPrimaryP, colorPrimaryDarkP);
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                int textColor = (int) animation.getAnimatedValue("textColor");
                int chartBg = (int) animation.getAnimatedValue("chartBg");
                int windowBg = (int) animation.getAnimatedValue("windowBg");
                int axisColor = (int) animation.getAnimatedValue("axisColor");
                int axisColorDark = (int) animation.getAnimatedValue("axisColorDark");
                int vertAxisColor = (int) animation.getAnimatedValue("vertAxisColor");
                int axisTextColor = (int) animation.getAnimatedValue("axisTextColor");
                int chartLabelBackground = (int) animation.getAnimatedValue("chartLabelBackground");
                int chartDateLabelColor = (int) animation.getAnimatedValue("chartDateLabelColor");
                int foregroundColor = (int) animation.getAnimatedValue("foregroundColor");
                int foregroundBorderColor =
                        (int) animation.getAnimatedValue("foregroundBorderColor");
                int colorPrimary = (int) animation.getAnimatedValue("colorPrimary");
                int colorPrimaryDark = (int) animation.getAnimatedValue("colorPrimaryDark");

                for (LinearLayout root : roots) {
                    root.setBackgroundColor(chartBg);
                }
                for (LineView lineView : lineViews) {
                    lineView.setAxisColor(axisColor);
                    lineView.setAxisColorDark(axisColorDark);
                    lineView.setVertAxisColor(vertAxisColor);
                    lineView.setAxisTextColor(axisTextColor);
                    lineView.setChartLabelColor(chartLabelBackground);
                    lineView.setChartDateLabelColor(chartDateLabelColor);
                    lineView.setChartBackgroundColor(chartBg);
                    lineView.invalidate();
                }

                for (BigLineView bigLineView : bigLineViews) {
                    bigLineView.setChartForegroundColor(foregroundColor);
                    bigLineView.setChartForegroundBorderColor(foregroundBorderColor);
                    bigLineView.invalidate();
                }

                for (TextView textView : textViews) {
                    textView.setTextColor(textColor);
                }

                for (CheckBox checkBox : checkBoxes) {
                    checkBox.setBgColor(chartBg);
                }

                for (View divider : dividers) {
                    divider.setBackgroundColor(windowBg);
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    getWindow().setStatusBarColor(colorPrimaryDark);
                }

                windowDrawable.setColor(windowBg);
                actionBarDrawable.setColor(colorPrimary);
            }
        });

        if (!animate) anim.setCurrentPlayTime(300);
        anim.setDuration(300);
        anim.start();
    }

    private PropertyValuesHolder generateProperty(String name, boolean isDay, int dayColor,
            int nightColor) {
        return PropertyValuesHolder.ofObject(name, ArgbEvaluator.getInstance(),
                isDay ? nightColor : dayColor, isDay ? dayColor : nightColor);
    }

    @Override
    protected void onDestroy() {
        for (BigLineView bigLineView : bigLineViews) {
            bigLineView.clearListeners();
        }
        super.onDestroy();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        boolean isPortrait = newConfig.orientation == Configuration.ORIENTATION_PORTRAIT
                || newConfig.screenHeightDp > newConfig.screenWidthDp;

        for (LineView lineView : lineViews) {
            ViewGroup.LayoutParams layoutParams = lineView.getLayoutParams();
            layoutParams.height = Utils.dpToPx(isPortrait ? 400 : 200);
            lineView.setLayoutParams(layoutParams);
        }
    }
}
