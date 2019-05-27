package com.kuxurum.smoothlinechart;

import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.flexbox.FlexDirection;
import com.google.android.flexbox.FlexWrap;
import com.google.android.flexbox.FlexboxLayout;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import org.json.JSONException;

public class MainActivity extends AppCompatActivity {
    private Data[] datas;
    private List<LinearLayout> roots = new ArrayList<>();

    private List<PercentageBarLineView> percentageBarLineViews = new ArrayList<>();
    private List<BigPercentageBarLineView> bigPercentageBarLineViews = new ArrayList<>();
    private List<StackedBarLineView> stackedBarLineViews = new ArrayList<>();
    private List<BigStackedBarLineView> bigStackedBarLineViews = new ArrayList<>();
    private List<BarLineView> barLineViews = new ArrayList<>();
    private List<BigBarLineView> bigBarLineViews = new ArrayList<>();
    private List<TwoAxisLineView> twoAxisLineViews = new ArrayList<>();
    private List<BigTwoAxisLineView> bigTwoAxisLineViews = new ArrayList<>();
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
        try {
            datas = DataParser.parse(this, "chart_data.json");
        } catch (Exception e) {
            Log.e("LineView", "Exception: ", e);
        }
        from = new float[datas.length];
        to = new float[datas.length];
        for (int i = 0; i < from.length; i++) {
            from[i] = 0.75f;
            to[i] = 2f;
        }

        int linesCount = 0;
        for (Data data : datas) {
            linesCount += data.columns.length;
        }

        ViewGroup mainRoot = findViewById(R.id.layout_root);

        int orientation = getResources().getConfiguration().orientation;
        boolean isPortrait = orientation != Configuration.ORIENTATION_LANDSCAPE;

        for (int j = 0; j < datas.length; j++) {
            final Data data = datas[j];

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
                root.setPadding(0, 0, 0, Utils.dpToPx(12));
            }

            generateBlock(data, root, isPortrait, from[j], to[j]);

            mainRoot.addView(root);
        }

        TextView textView = new TextView(this);
        textView.setTextSize(16);
        textView.setPadding(Utils.dpToPx(24), Utils.dpToPx(24), Utils.dpToPx(24), Utils.dpToPx(24));
        textView.setTextColor(Color.GRAY);
        textView.setText("I developed this charts for challenge and fun and I like it! "
                + "Honestly, I didn't really care much about performance (also, I think it has many design glitches, but I tried to make it like on screenshots), I wanted to make hard bonus goals as much as possible and make cool animations. "
                + "My focus was to solve tasks from the hardest one to the easiest. So I tried to implement animations from 5th down to 1st. "
                + "But I was really out of time to implement animations and make them smooth and perfect :( All mechanics are ready, but ¯\\_(ツ)_/¯ I'm too slow :D "
                + "Anyway, I think app is far not perfect, but pretty (for me).");
        mainRoot.addView(textView);

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

    private void applyColor(final boolean animate) {
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
        int percVertAxisColor = res.getColor(R.color.percVertAxisColor);
        int nightPercVertAxisColor = res.getColor(R.color.night_percVertAxisColor);
        int axisTextColor = res.getColor(R.color.axisTextColor);
        int nightAxisTextColor = res.getColor(R.color.night_axisTextColor);
        int chartLabelBackground = res.getColor(R.color.chartLabelBackground);
        int nightChartLabelBackground = res.getColor(R.color.night_chartLabelBackground);
        int chartDateLabelColor = res.getColor(R.color.chartDateLabelColor);
        int nightChartDateLabelColor = res.getColor(R.color.night_chartDateLabelColor);
        int foregroundColor = res.getColor(R.color.foregroundColor);
        int nightForegroundColor = res.getColor(R.color.night_foregroundColor);
        int foregroundBorderColor = res.getColor(R.color.foregroundBorderColor);
        int nightForegroundBorderColor = res.getColor(R.color.night_foregroundBorderColor);
        int colorPrimary = res.getColor(R.color.colorPrimary);
        int nightColorPrimary = res.getColor(R.color.night_colorPrimary);
        int colorPrimaryDark = res.getColor(R.color.colorPrimaryDark);
        int nightColorPrimaryDark = res.getColor(R.color.night_colorPrimaryDark);
        int maskColor = res.getColor(R.color.stackedBarMaskColor);
        int nightMaskColor = res.getColor(R.color.night_stackedBarMaskColor);
        int zoomOutColor = res.getColor(R.color.zoomOutColor);
        int nightZoomOutColor = res.getColor(R.color.night_zoomOutColor);
        int titleColor = res.getColor(R.color.titleColor);
        int nightTitleColor = res.getColor(R.color.night_titleColor);

        final ColorDrawable windowDrawable = new ColorDrawable();
        final ColorDrawable actionBarDrawable = new ColorDrawable();
        getWindow().setBackgroundDrawable(windowDrawable);
        getSupportActionBar().setBackgroundDrawable(actionBarDrawable);

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
        PropertyValuesHolder maskColorP =
                generateProperty("maskColor", isDay, maskColor, nightMaskColor);
        PropertyValuesHolder zoomOutColorP =
                generateProperty("zoomOutColor", isDay, zoomOutColor, nightZoomOutColor);
        PropertyValuesHolder titleColorP =
                generateProperty("titleColor", isDay, titleColor, nightTitleColor);
        PropertyValuesHolder percVertAxisColorP =
                generateProperty("percVertAxisColor", isDay, percVertAxisColor,
                        nightPercVertAxisColor);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        }

        final ValueAnimator anim =
                ValueAnimator.ofPropertyValuesHolder(textColorP, chartBgP, windowBgP, axisColorP,
                        axisColorDarkP, vertAxisColorP, axisTextColorP, chartLabelBackgroundP,
                        chartDateLabelColorP, foregroundColorP, foregroundBorderColorP,
                        colorPrimaryP, colorPrimaryDarkP, maskColorP, zoomOutColorP, titleColorP,
                        percVertAxisColorP);
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                int textColor = (int) animation.getAnimatedValue("textColor");
                int chartBg = (int) animation.getAnimatedValue("chartBg");
                int windowBg = (int) animation.getAnimatedValue("windowBg");
                int axisColor = (int) animation.getAnimatedValue("axisColor");
                int axisColorDark = (int) animation.getAnimatedValue("axisColorDark");
                int vertAxisColor = (int) animation.getAnimatedValue("vertAxisColor");
                int percVertAxisColor = (int) animation.getAnimatedValue("percVertAxisColor");
                int axisTextColor = (int) animation.getAnimatedValue("axisTextColor");
                int chartLabelBackground = (int) animation.getAnimatedValue("chartLabelBackground");
                int chartDateLabelColor = (int) animation.getAnimatedValue("chartDateLabelColor");
                int foregroundColor = (int) animation.getAnimatedValue("foregroundColor");
                int foregroundBorderColor =
                        (int) animation.getAnimatedValue("foregroundBorderColor");
                int colorPrimary = (int) animation.getAnimatedValue("colorPrimary");
                int colorPrimaryDark = (int) animation.getAnimatedValue("colorPrimaryDark");
                int maskColor = (int) animation.getAnimatedValue("maskColor");
                int zoomOutColor = (int) animation.getAnimatedValue("zoomOutColor");
                int titleColor = (int) animation.getAnimatedValue("titleColor");

                for (ViewGroup root : roots) {
                    root.setBackgroundColor(chartBg);
                }

                for (LineView lineView : lineViews) {
                    lineView.setChartBackgroundColor(chartBg);
                    lineView.setAxisColor(axisColor);
                    lineView.setAxisColorDark(axisColorDark);
                    lineView.setVertAxisColor(vertAxisColor);
                    lineView.setAxisTextColor(axisTextColor);
                    lineView.setXAxisTextColor(axisTextColor);
                    lineView.setChartLabelColor(chartLabelBackground);
                    lineView.setChartDateLabelColor(chartDateLabelColor);
                    lineView.setChartLabelDataNameColor(chartDateLabelColor);
                    lineView.setMaskColor(maskColor);
                    lineView.setDataLabelArrowColor(Color.parseColor("#D2D5D7"));
                    lineView.setZoomOutColor(zoomOutColor);
                    lineView.setTitleColor(titleColor);
                    lineView.invalidate();
                }

                for (BigLineView bigLineView : bigLineViews) {
                    bigLineView.setChartForegroundColor(foregroundColor);
                    bigLineView.setChartForegroundBorderColor(foregroundBorderColor);
                    bigLineView.invalidate();
                }

                for (TwoAxisLineView lineView : twoAxisLineViews) {
                    lineView.setChartBackgroundColor(chartBg);
                    lineView.setAxisColor(axisColor);
                    lineView.setAxisColorDark(axisColorDark);
                    lineView.setVertAxisColor(vertAxisColor);
                    lineView.setAxisTextColor(axisTextColor);
                    lineView.setXAxisTextColor(axisTextColor);
                    lineView.setChartLabelColor(chartLabelBackground);
                    lineView.setChartDateLabelColor(chartDateLabelColor);
                    lineView.setChartLabelDataNameColor(chartDateLabelColor);
                    lineView.setMaskColor(maskColor);
                    lineView.setDataLabelArrowColor(Color.parseColor("#D2D5D7"));
                    lineView.setZoomOutColor(zoomOutColor);
                    lineView.setTitleColor(titleColor);
                    lineView.invalidate();
                }

                for (BigTwoAxisLineView bigLineView : bigTwoAxisLineViews) {
                    bigLineView.setChartForegroundColor(foregroundColor);
                    bigLineView.setChartForegroundBorderColor(foregroundBorderColor);
                    bigLineView.invalidate();
                }

                for (PercentageBarLineView lineView : percentageBarLineViews) {
                    lineView.setAxisColor(axisColor);
                    lineView.setAxisColorDark(axisColorDark);
                    lineView.setVertAxisColor(percVertAxisColor);
                    lineView.setAxisTextColor(axisTextColor);
                    lineView.setXAxisTextColor(axisTextColor);
                    lineView.setChartLabelColor(chartLabelBackground);
                    lineView.setChartDateLabelColor(chartDateLabelColor);
                    lineView.setChartLabelDataNameColor(chartDateLabelColor);
                    lineView.setMaskColor(maskColor);
                    lineView.setDataLabelArrowColor(Color.parseColor("#D2D5D7"));
                    lineView.setZoomOutColor(zoomOutColor);
                    lineView.setTitleColor(titleColor);
                    lineView.invalidate();
                }

                for (BigPercentageBarLineView bigLineView : bigPercentageBarLineViews) {
                    bigLineView.setChartForegroundColor(foregroundColor);
                    bigLineView.setChartForegroundBorderColor(foregroundBorderColor);
                    bigLineView.invalidate();
                }

                for (StackedBarLineView lineView : stackedBarLineViews) {
                    lineView.setAxisColor(axisColor);
                    lineView.setAxisColorDark(axisColorDark);
                    lineView.setVertAxisColor(vertAxisColor);
                    lineView.setAxisTextColor(axisTextColor);
                    lineView.setXAxisTextColor(axisTextColor);
                    lineView.setChartLabelColor(chartLabelBackground);
                    lineView.setChartDateLabelColor(chartDateLabelColor);
                    lineView.setChartLabelDataNameColor(chartDateLabelColor);
                    lineView.setMaskColor(maskColor);
                    lineView.setDataLabelArrowColor(Color.parseColor("#D2D5D7"));
                    lineView.setZoomOutColor(zoomOutColor);
                    lineView.setTitleColor(titleColor);
                    lineView.invalidate();
                }

                for (BigStackedBarLineView bigLineView : bigStackedBarLineViews) {
                    bigLineView.setChartForegroundColor(foregroundColor);
                    bigLineView.setChartForegroundBorderColor(foregroundBorderColor);
                    bigLineView.invalidate();
                }

                for (BarLineView lineView : barLineViews) {
                    lineView.setChartBackgroundColor(chartBg);
                    lineView.setAxisColor(axisColor);
                    lineView.setAxisColorDark(axisColorDark);
                    lineView.setVertAxisColor(vertAxisColor);
                    lineView.setAxisTextColor(axisTextColor);
                    lineView.setXAxisTextColor(axisTextColor);
                    lineView.setChartLabelColor(chartLabelBackground);
                    lineView.setChartDateLabelColor(chartDateLabelColor);
                    lineView.setChartLabelDataNameColor(chartDateLabelColor);
                    lineView.setMaskColor(maskColor);
                    lineView.setDataLabelArrowColor(Color.parseColor("#D2D5D7"));
                    lineView.setZoomOutColor(zoomOutColor);
                    lineView.setTitleColor(titleColor);
                    lineView.invalidate();
                }

                for (BigBarLineView bigLineView : bigBarLineViews) {
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
        for (BigPercentageBarLineView bigLineView : bigPercentageBarLineViews) {
            bigLineView.clearListeners();
        }
        super.onDestroy();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        boolean isPortrait = newConfig.orientation == Configuration.ORIENTATION_PORTRAIT
                || newConfig.screenHeightDp > newConfig.screenWidthDp;

        for (PercentageBarLineView lineView : percentageBarLineViews) {
            ViewGroup.LayoutParams layoutParams = lineView.getLayoutParams();
            layoutParams.height = Utils.dpToPx(isPortrait ? 400 : 200);
            lineView.setLayoutParams(layoutParams);
        }
    }

    void generateBlock(Data data, ViewGroup root, boolean isPortrait, float from, float to) {
        if (data.stacked && data.percentage) {
            final PercentageBarLineView lineView = new PercentageBarLineView(this);
            percentageBarLineViews.add(lineView);
            {
                lineView.setLayoutParams(
                        new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                isPortrait ? Utils.dpToPx(400) : Utils.dpToPx(200)));
                lineView.setData(data);
                root.addView(lineView);
            }

            final BigPercentageBarLineView bigLineView = new BigPercentageBarLineView(this);
            bigPercentageBarLineViews.add(bigLineView);
            {
                LinearLayout.LayoutParams params =
                        new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                Utils.dpToPx(50));

                params.topMargin = Utils.dpToPx(8);

                bigLineView.setLayoutParams(params);
                bigLineView.setData(data);
                bigLineView.addListener(new BigPercentageBarLineView.MoveListener() {
                    @Override
                    public void onUpdateFrom(float from) {
                        lineView.setFrom(from);
                    }

                    @Override
                    public void onUpdateFromIndex(int from) {
                        lineView.setFromIndex(from);
                    }

                    @Override
                    public void onUpdateTo(float to) {
                        lineView.setTo(to);
                    }

                    @Override
                    public void onUpdateToIndex(int to) {
                        lineView.setToIndex(to);
                    }
                });
                bigLineView.setFrom(from);
                bigLineView.setTo(to);

                lineView.listener = new PercentageBarLineView.Listener() {
                    @Override
                    public void onPressed(long time) {
                        bigLineView.setDetailed(time);
                        lineView.animateToCircle();
                    }

                    @Override
                    public void onZoomOut() {
                        lineView.animateToBar();
                        bigLineView.setWhole();
                    }
                };
            }
            root.addView(bigLineView);

            final FlexboxLayout flexbox = new FlexboxLayout(this);
            {
                LinearLayout.LayoutParams params =
                        new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT);
                params.topMargin = Utils.dpToPx(20);
                params.leftMargin = Utils.dpToPx(24);
                params.rightMargin = Utils.dpToPx(24);
                flexbox.setLayoutParams(params);
                flexbox.setFlexDirection(FlexDirection.ROW);
                flexbox.setFlexWrap(FlexWrap.WRAP);
                root.addView(flexbox);
            }

            for (int i = 1; i < data.columns.length; i++) {
                Data.Column column = data.columns[i];
                final CheckBox cb = new CheckBox(this);
                checkBoxes.add(cb);
                {
                    FlexboxLayout.LayoutParams params =
                            new FlexboxLayout.LayoutParams(FlexboxLayout.LayoutParams.WRAP_CONTENT,
                                    FlexboxLayout.LayoutParams.WRAP_CONTENT);
                    params.leftMargin = params.rightMargin =
                            params.topMargin = params.bottomMargin = Utils.dpToPx(4);
                    cb.setLayoutParams(params);
                    cb.setCheckedColor(column.color);
                    cb.setChecked(true);
                    cb.text = column.name;
                    cb.setPadding(Utils.dpToPx(12), Utils.dpToPx(12), Utils.dpToPx(16),
                            Utils.dpToPx(12));
                    flexbox.addView(cb);
                }

                final int finalI = i;
                cb.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (cb.isChecked()) {
                            boolean allowToUnCheck = false;
                            for (int c = 0; c < flexbox.getChildCount(); c++) {
                                if (c + 1 == finalI) continue;
                                allowToUnCheck = allowToUnCheck || ((CheckBox) flexbox.getChildAt(
                                        c)).isChecked();
                            }
                            if (allowToUnCheck) {
                                cb.setChecked(!cb.isChecked());
                                lineView.setLineEnabled(finalI, cb.isChecked());
                                bigLineView.setLineEnabled(finalI, cb.isChecked());
                            }
                            return;
                        }

                        cb.setChecked(!cb.isChecked());
                        lineView.setLineEnabled(finalI, cb.isChecked());
                        bigLineView.setLineEnabled(finalI, cb.isChecked());
                    }
                });
                cb.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        cb.setChecked(true);
                        lineView.setLineEnabled(finalI, true);
                        bigLineView.setLineEnabled(finalI, true);

                        for (int c = 0; c < flexbox.getChildCount(); c++) {
                            if (c + 1 == finalI) continue;
                            ((CheckBox) flexbox.getChildAt(c)).setChecked(false);
                            lineView.setLineEnabled(c + 1, false);
                            bigLineView.setLineEnabled(c + 1, false);
                        }
                        return true;
                    }
                });
            }
        } else if (data.stacked) {
            final StackedBarLineView lineView = new StackedBarLineView(this);
            stackedBarLineViews.add(lineView);
            {
                lineView.setLayoutParams(
                        new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                isPortrait ? Utils.dpToPx(400) : Utils.dpToPx(200)));
                lineView.setData(data);
                root.addView(lineView);
            }

            final BigStackedBarLineView bigLineView = new BigStackedBarLineView(this);
            bigStackedBarLineViews.add(bigLineView);
            {
                LinearLayout.LayoutParams params =
                        new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                Utils.dpToPx(50));

                params.topMargin = Utils.dpToPx(8);

                bigLineView.setLayoutParams(params);
                bigLineView.setData(data);
                bigLineView.addListener(new BigStackedBarLineView.MoveListener() {
                    @Override
                    public void onUpdateFrom(float from) {
                        lineView.setFrom(from);
                    }

                    @Override
                    public void onUpdateTo(float to) {
                        lineView.setTo(to);
                    }
                });
                bigLineView.setFrom(from);
                bigLineView.setTo(to);

                lineView.listener = new StackedBarLineView.Listener() {
                    @Override
                    public void onPressed(long time) {
                        try {
                            Calendar calendar = Calendar.getInstance();
                            calendar.setTimeInMillis(time);
                            String folder = String.valueOf(calendar.get(Calendar.YEAR))
                                    + "-"
                                    + String.format(Locale.US, "%02d",
                                    calendar.get(Calendar.MONTH) + 1);
                            String day = String.format(Locale.US, "%02d",
                                    calendar.get(Calendar.DAY_OF_MONTH));
                            Data data = DataParser.parse(MainActivity.this,
                                    "3/" + folder + "/" + day + ".json")[0];
                            lineView.setDetailed(data);
                            bigLineView.setDetailed(data);
                        } catch (IOException e) {
                            e.printStackTrace();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onZoomOut() {
                        lineView.setWhole();
                        bigLineView.setWhole();
                    }
                };
            }
            root.addView(bigLineView);

            final FlexboxLayout flexbox = new FlexboxLayout(this);
            {
                LinearLayout.LayoutParams params =
                        new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT);
                params.topMargin = Utils.dpToPx(20);
                params.leftMargin = Utils.dpToPx(24);
                params.rightMargin = Utils.dpToPx(24);
                flexbox.setLayoutParams(params);
                flexbox.setFlexDirection(FlexDirection.ROW);
                flexbox.setFlexWrap(FlexWrap.WRAP);
                root.addView(flexbox);
            }

            for (int i = 1; i < data.columns.length; i++) {
                Data.Column column = data.columns[i];
                final CheckBox cb = new CheckBox(this);
                checkBoxes.add(cb);
                {
                    FlexboxLayout.LayoutParams params =
                            new FlexboxLayout.LayoutParams(FlexboxLayout.LayoutParams.WRAP_CONTENT,
                                    FlexboxLayout.LayoutParams.WRAP_CONTENT);
                    params.leftMargin = params.rightMargin =
                            params.topMargin = params.bottomMargin = Utils.dpToPx(4);
                    cb.setLayoutParams(params);
                    cb.setCheckedColor(column.color);
                    cb.setChecked(true);
                    cb.text = column.name;
                    cb.setPadding(Utils.dpToPx(12), Utils.dpToPx(12), Utils.dpToPx(16),
                            Utils.dpToPx(12));
                    flexbox.addView(cb);
                }

                final int finalI = i;
                cb.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (cb.isChecked()) {
                            boolean allowToUnCheck = false;
                            for (int c = 0; c < flexbox.getChildCount(); c++) {
                                if (c + 1 == finalI) continue;
                                allowToUnCheck = allowToUnCheck || ((CheckBox) flexbox.getChildAt(
                                        c)).isChecked();
                            }
                            if (allowToUnCheck) {
                                cb.setChecked(!cb.isChecked());
                                lineView.setLineEnabled(finalI, cb.isChecked());
                                bigLineView.setLineEnabled(finalI, cb.isChecked());
                            }
                            return;
                        }

                        cb.setChecked(!cb.isChecked());
                        lineView.setLineEnabled(finalI, cb.isChecked());
                        bigLineView.setLineEnabled(finalI, cb.isChecked());
                    }
                });
                cb.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        cb.setChecked(true);
                        lineView.setLineEnabled(finalI, true);
                        bigLineView.setLineEnabled(finalI, true);

                        for (int c = 0; c < flexbox.getChildCount(); c++) {
                            if (c + 1 == finalI) continue;
                            ((CheckBox) flexbox.getChildAt(c)).setChecked(false);
                            lineView.setLineEnabled(c + 1, false);
                            bigLineView.setLineEnabled(c + 1, false);
                        }
                        return true;
                    }
                });
            }
        } else if (data.yScaled) {
            final TwoAxisLineView lineView = new TwoAxisLineView(this);
            twoAxisLineViews.add(lineView);
            {
                lineView.setLayoutParams(
                        new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                isPortrait ? Utils.dpToPx(400) : Utils.dpToPx(200)));
                lineView.setData(data);
                root.addView(lineView);
            }

            final BigTwoAxisLineView bigLineView = new BigTwoAxisLineView(this);
            bigTwoAxisLineViews.add(bigLineView);
            {
                LinearLayout.LayoutParams params =
                        new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                Utils.dpToPx(50));

                params.topMargin = Utils.dpToPx(8);

                bigLineView.setLayoutParams(params);
                bigLineView.setData(data);
                bigLineView.addListener(new BigTwoAxisLineView.MoveListener() {
                    @Override
                    public void onUpdateFrom(float from) {
                        lineView.setFrom(from);
                    }

                    @Override
                    public void onUpdateTo(float to) {
                        lineView.setTo(to);
                    }
                });
                bigLineView.setFrom(from);
                bigLineView.setTo(to);

                //lineView.listener = new LineView.Listener() {
                //    @Override
                //    public void onPressed(long time) {
                //bigLineView.setDetailed(time - 1000 * 60 * 60 * 24 * 3,
                //        time + 1000 * 60 * 60 * 24 * 4);
                //lineView.animateToCircle();
                //}

                //@Override
                //public void onZoomOut() {
                //lineView.animateToBar();
                //bigLineView.setWhole();
                //}
                //};
            }
            root.addView(bigLineView);

            final FlexboxLayout flexbox = new FlexboxLayout(this);
            {
                LinearLayout.LayoutParams params =
                        new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT);
                params.topMargin = Utils.dpToPx(20);
                params.leftMargin = Utils.dpToPx(24);
                params.rightMargin = Utils.dpToPx(24);
                flexbox.setLayoutParams(params);
                flexbox.setFlexDirection(FlexDirection.ROW);
                flexbox.setFlexWrap(FlexWrap.WRAP);
                root.addView(flexbox);
            }

            for (int i = 1; i < data.columns.length; i++) {
                Data.Column column = data.columns[i];
                final CheckBox cb = new CheckBox(this);
                checkBoxes.add(cb);
                {
                    FlexboxLayout.LayoutParams params =
                            new FlexboxLayout.LayoutParams(FlexboxLayout.LayoutParams.WRAP_CONTENT,
                                    FlexboxLayout.LayoutParams.WRAP_CONTENT);
                    params.leftMargin = params.rightMargin =
                            params.topMargin = params.bottomMargin = Utils.dpToPx(4);
                    cb.setLayoutParams(params);
                    cb.setCheckedColor(column.color);
                    cb.setChecked(true);
                    cb.text = column.name;
                    cb.setPadding(Utils.dpToPx(12), Utils.dpToPx(12), Utils.dpToPx(16),
                            Utils.dpToPx(12));
                    flexbox.addView(cb);
                }

                final int finalI = i;
                cb.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (cb.isChecked()) {
                            boolean allowToUnCheck = false;
                            for (int c = 0; c < flexbox.getChildCount(); c++) {
                                if (c + 1 == finalI) continue;
                                allowToUnCheck = allowToUnCheck || ((CheckBox) flexbox.getChildAt(
                                        c)).isChecked();
                            }
                            if (allowToUnCheck) {
                                cb.setChecked(!cb.isChecked());
                                lineView.setLineEnabled(finalI, cb.isChecked());
                                bigLineView.setLineEnabled(finalI, cb.isChecked());
                            }
                            return;
                        }

                        cb.setChecked(!cb.isChecked());
                        lineView.setLineEnabled(finalI, cb.isChecked());
                        bigLineView.setLineEnabled(finalI, cb.isChecked());
                    }
                });
                cb.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        cb.setChecked(true);
                        lineView.setLineEnabled(finalI, true);
                        bigLineView.setLineEnabled(finalI, true);

                        for (int c = 0; c < flexbox.getChildCount(); c++) {
                            if (c + 1 == finalI) continue;
                            ((CheckBox) flexbox.getChildAt(c)).setChecked(false);
                            lineView.setLineEnabled(c + 1, false);
                            bigLineView.setLineEnabled(c + 1, false);
                        }
                        return true;
                    }
                });
            }
        } else if (data.hasBars) {
            final BarLineView lineView = new BarLineView(this);
            barLineViews.add(lineView);
            {
                lineView.setLayoutParams(
                        new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                isPortrait ? Utils.dpToPx(400) : Utils.dpToPx(200)));
                lineView.setData(data);
                root.addView(lineView);
            }

            final BigBarLineView bigLineView = new BigBarLineView(this);
            bigBarLineViews.add(bigLineView);
            {
                LinearLayout.LayoutParams params =
                        new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                Utils.dpToPx(50));

                params.topMargin = Utils.dpToPx(8);

                bigLineView.setLayoutParams(params);
                bigLineView.setData(data);
                bigLineView.addListener(new BigBarLineView.MoveListener() {
                    @Override
                    public void onUpdateFrom(float from) {
                        lineView.setFrom(from);
                    }

                    @Override
                    public void onUpdateTo(float to) {
                        lineView.setTo(to);
                    }
                });
                bigLineView.setFrom(from);
                bigLineView.setTo(to);
            }
            root.addView(bigLineView);

            final FlexboxLayout flexbox = new FlexboxLayout(this);
            {
                LinearLayout.LayoutParams params =
                        new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT);
                params.topMargin = Utils.dpToPx(20);
                params.leftMargin = Utils.dpToPx(24);
                params.rightMargin = Utils.dpToPx(24);
                flexbox.setLayoutParams(params);
                flexbox.setFlexDirection(FlexDirection.ROW);
                flexbox.setFlexWrap(FlexWrap.WRAP);
                root.addView(flexbox);
            }

            lineView.listener = new BarLineView.Listener() {
                @Override
                public void onPressed(long time) {
                    try {
                        Calendar calendar = Calendar.getInstance();
                        calendar.setTimeInMillis(time);
                        String folder =
                                String.valueOf(calendar.get(Calendar.YEAR)) + "-" + String.format(
                                        Locale.US, "%02d", calendar.get(Calendar.MONTH) + 1);
                        String day = String.format(Locale.US, "%02d",
                                calendar.get(Calendar.DAY_OF_MONTH));
                        Data data = DataParser.parse(MainActivity.this,
                                "4/" + folder + "/" + day + ".json")[0];
                        lineView.setDetailed(data);
                        bigLineView.setVisibility(View.GONE);
                        for (int c = 0; c < flexbox.getChildCount(); c++) {
                            CheckBox checkbox = (CheckBox) flexbox.getChildAt(c);
                            checkbox.setVisibility(View.VISIBLE);
                            checkbox.setChecked(true);
                            if (c == 0) {
                                checkbox.text =
                                        new SimpleDateFormat("dd MMMM").format(new Date(time));
                            } else if (c == 1) {
                                checkbox.text = new SimpleDateFormat("dd MMMM").format(
                                        new Date(time - 1000 * 60 * 60 * 24));
                            } else {
                                checkbox.text = new SimpleDateFormat("dd MMMM").format(
                                        new Date(time - 1000 * 60 * 60 * 24 * 7));
                            }
                            checkbox.requestLayout();
                            checkbox.invalidate();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onZoomOut() {
                    lineView.setWhole();
                    bigLineView.setVisibility(View.VISIBLE);
                    for (int c = 0; c < flexbox.getChildCount(); c++) {
                        flexbox.getChildAt(c).setVisibility(View.GONE);
                    }
                }
            };

            for (int i = 0; i < 3; i++) {
                final CheckBox cb = new CheckBox(this);
                checkBoxes.add(cb);
                {
                    FlexboxLayout.LayoutParams params =
                            new FlexboxLayout.LayoutParams(FlexboxLayout.LayoutParams.WRAP_CONTENT,
                                    FlexboxLayout.LayoutParams.WRAP_CONTENT);
                    params.leftMargin = params.rightMargin =
                            params.topMargin = params.bottomMargin = Utils.dpToPx(4);
                    cb.setLayoutParams(params);
                    int color = (i == 0) ? Color.parseColor("#64ADED")
                            : (i == 1 ? Color.parseColor("#558DED") : Color.parseColor("#5CBCDF"));
                    cb.setCheckedColor(color);
                    cb.setChecked(true);
                    cb.setVisibility(View.GONE);
                    cb.setPadding(Utils.dpToPx(12), Utils.dpToPx(12), Utils.dpToPx(16),
                            Utils.dpToPx(12));
                    flexbox.addView(cb);
                }

                final int finalI = i;
                cb.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (cb.isChecked()) {
                            boolean allowToUnCheck = false;
                            for (int c = 0; c < flexbox.getChildCount(); c++) {
                                if (c == finalI) continue;
                                allowToUnCheck = allowToUnCheck || ((CheckBox) flexbox.getChildAt(
                                        c)).isChecked();
                            }
                            if (allowToUnCheck) {
                                cb.setChecked(!cb.isChecked());
                                lineView.setLineEnabled(finalI + 1, cb.isChecked());
                            }
                            return;
                        }

                        cb.setChecked(!cb.isChecked());
                        lineView.setLineEnabled(finalI + 1, cb.isChecked());
                    }
                });
                cb.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        cb.setChecked(true);
                        lineView.setLineEnabled(finalI + 1, true);

                        for (int c = 0; c < flexbox.getChildCount(); c++) {
                            if (c == finalI) continue;
                            ((CheckBox) flexbox.getChildAt(c)).setChecked(false);
                            lineView.setLineEnabled(c + 1, false);
                        }
                        return true;
                    }
                });
            }
        } else {
            final LineView lineView = new LineView(this);
            lineViews.add(lineView);
            {
                lineView.setLayoutParams(
                        new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                isPortrait ? Utils.dpToPx(400) : Utils.dpToPx(200)));
                lineView.setData(data);
                root.addView(lineView);
            }

            final BigLineView bigLineView = new BigLineView(this);
            bigLineViews.add(bigLineView);
            {
                LinearLayout.LayoutParams params =
                        new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                Utils.dpToPx(50));

                params.topMargin = Utils.dpToPx(8);

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
                bigLineView.setFrom(from);
                bigLineView.setTo(to);

                lineView.listener = new LineView.Listener() {
                    @Override
                    public void onPressed(long time) {
                        //try {
                        //    Calendar calendar = Calendar.getInstance();
                        //    calendar.setTimeInMillis(time);
                        //    String folder = String.valueOf(calendar.get(Calendar.YEAR))
                        //            + "-"
                        //            + String.format(Locale.US, "%02d",
                        //            calendar.get(Calendar.MONTH) + 1);
                        //    String day = String.format(Locale.US, "%02d",
                        //            calendar.get(Calendar.DAY_OF_MONTH));
                        //    Data data = DataParser.parse(MainActivity.this,
                        //            "1/" + folder + "/" + day + ".json")[0];
                        //    lineView.setDetailed(data);
                        //    bigLineView.setDetailed(data);
                        //} catch (IOException e) {
                        //    e.printStackTrace();
                        //} catch (JSONException e) {
                        //    e.printStackTrace();
                        //}
                    }

                    @Override
                    public void onZoomOut() {
                        //lineView.setWhole();
                        //bigLineView.setWhole();
                    }
                };
            }
            root.addView(bigLineView);

            final FlexboxLayout flexbox = new FlexboxLayout(this);
            {
                LinearLayout.LayoutParams params =
                        new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT);
                params.topMargin = Utils.dpToPx(20);
                params.leftMargin = Utils.dpToPx(24);
                params.rightMargin = Utils.dpToPx(24);
                flexbox.setLayoutParams(params);
                flexbox.setFlexDirection(FlexDirection.ROW);
                flexbox.setFlexWrap(FlexWrap.WRAP);
                root.addView(flexbox);
            }

            for (int i = 1; i < data.columns.length; i++) {
                Data.Column column = data.columns[i];
                final CheckBox cb = new CheckBox(this);
                checkBoxes.add(cb);
                {
                    FlexboxLayout.LayoutParams params =
                            new FlexboxLayout.LayoutParams(FlexboxLayout.LayoutParams.WRAP_CONTENT,
                                    FlexboxLayout.LayoutParams.WRAP_CONTENT);
                    params.leftMargin = params.rightMargin =
                            params.topMargin = params.bottomMargin = Utils.dpToPx(4);
                    cb.setLayoutParams(params);
                    cb.setCheckedColor(column.color);
                    cb.setChecked(true);
                    cb.text = column.name;
                    cb.setPadding(Utils.dpToPx(12), Utils.dpToPx(12), Utils.dpToPx(16),
                            Utils.dpToPx(12));
                    flexbox.addView(cb);
                }

                final int finalI = i;
                cb.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (cb.isChecked()) {
                            boolean allowToUnCheck = false;
                            for (int c = 0; c < flexbox.getChildCount(); c++) {
                                if (c + 1 == finalI) continue;
                                allowToUnCheck = allowToUnCheck || ((CheckBox) flexbox.getChildAt(
                                        c)).isChecked();
                            }
                            if (allowToUnCheck) {
                                cb.setChecked(!cb.isChecked());
                                lineView.setLineEnabled(finalI, cb.isChecked());
                                bigLineView.setLineEnabled(finalI, cb.isChecked());
                            }
                            return;
                        }

                        cb.setChecked(!cb.isChecked());
                        lineView.setLineEnabled(finalI, cb.isChecked());
                        bigLineView.setLineEnabled(finalI, cb.isChecked());
                    }
                });
                cb.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        cb.setChecked(true);
                        lineView.setLineEnabled(finalI, true);
                        bigLineView.setLineEnabled(finalI, true);

                        for (int c = 0; c < flexbox.getChildCount(); c++) {
                            if (c + 1 == finalI) continue;
                            ((CheckBox) flexbox.getChildAt(c)).setChecked(false);
                            lineView.setLineEnabled(c + 1, false);
                            bigLineView.setLineEnabled(c + 1, false);
                        }
                        return true;
                    }
                });
            }
        }
    }
}
