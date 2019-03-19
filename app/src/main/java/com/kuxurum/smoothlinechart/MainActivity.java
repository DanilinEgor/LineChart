package com.kuxurum.smoothlinechart;

import android.app.Activity;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {
    private Data[] datas;
    private List<BigLineView> bigLineViews = new ArrayList<>();
    private List<CheckBox> checkBoxes = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Utils.init(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        float[] to;
        float[] from;
        boolean[] checked;
        if (savedInstanceState == null) {
            try {
                datas = DataParser.parse(this);
            } catch (Exception e) {
                Log.e("LineView", "Exception: ", e);
            }
            from = new float[datas.length];
            to = new float[datas.length];
            for (int i = 0; i < from.length; i++) {
                from[i] = 0.8f;
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
        } else {
            datas = (Data[]) savedInstanceState.getParcelableArray("data");
            from = new float[datas.length];
            to = new float[datas.length];

            for (int i = 0; i < from.length; i++) {
                from[i] = savedInstanceState.getFloat(i + "_fromX");
                to[i] = savedInstanceState.getFloat(i + "_toX");
            }

            int linesCount = 0;
            for (Data data : datas) {
                linesCount += data.columns.length;
            }
            checked = new boolean[linesCount];
            for (int i = 0; i < linesCount; i++) {
                checked[i] = savedInstanceState.getBoolean(i + "_checked");
            }
        }

        int dividerColor = getResources().getColor(R.color.bg_pressed);
        int textColor = getResources().getColor(R.color.textColorPrimary);
        int chartBg = getResources().getColor(R.color.chartBackground);
        ViewGroup mainRoot = findViewById(R.id.layout_root);

        int orientation = getResources().getConfiguration().orientation;
        boolean isPortrait = orientation != Configuration.ORIENTATION_LANDSCAPE;

        int counter = 0;
        for (int j = 0; j < datas.length; j++) {
            Data data = datas[j];
            LinearLayout root = new LinearLayout(this);
            {
                ViewGroup.MarginLayoutParams params =
                        new ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT);
                if (j != 0) {
                    params.topMargin = Utils.dpToPx(24);
                }
                root.setLayoutParams(params);
                root.setOrientation(LinearLayout.VERTICAL);
                root.setBackgroundColor(chartBg);
            }

            final LineView lineView = new LineView(this);
            {
                lineView.setLayoutParams(
                        new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                isPortrait ? Utils.dpToPx(400) : Utils.dpToPx(200)));
                lineView.setData(data);
            }
            root.addView(lineView);

            final BigLineView bigLineView = new BigLineView(this);
            {
                ViewGroup.MarginLayoutParams params =
                        new ViewGroup.MarginLayoutParams(ViewGroup.MarginLayoutParams.MATCH_PARENT,
                                Utils.dpToPx(55));
                params.topMargin = Utils.dpToPx(8);
                params.leftMargin = Utils.dpToPx(24);
                params.rightMargin = Utils.dpToPx(24);
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
                bigLineViews.add(bigLineView);
            }
            root.addView(bigLineView);

            for (int i = 1; i < data.columns.length; i++) {
                Data.Column column = data.columns[i];
                FrameLayout layout = new FrameLayout(this);
                {
                    ViewGroup.MarginLayoutParams params = new ViewGroup.MarginLayoutParams(
                            ViewGroup.MarginLayoutParams.MATCH_PARENT,
                            ViewGroup.MarginLayoutParams.WRAP_CONTENT);
                    if (i == 1) {
                        params.topMargin = Utils.dpToPx(8);
                    }

                    layout.setLayoutParams(params);
                    layout.setPadding(0, Utils.dpToPx(16), 0, Utils.dpToPx(16));
                    layout.setBackgroundResource(R.drawable.selectable_background);
                }

                final CheckBox cb = new CheckBox(this);
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
                checkBoxes.add(cb);

                {
                    TextView tv = new TextView(this);
                    FrameLayout.LayoutParams params =
                            new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                                    ViewGroup.LayoutParams.WRAP_CONTENT);
                    params.leftMargin = Utils.dpToPx(72);
                    params.gravity = Gravity.CENTER_VERTICAL;
                    tv.setLayoutParams(params);
                    tv.setText(column.name);
                    tv.setTextColor(textColor);
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
                    FrameLayout.LayoutParams params =
                            new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                    Utils.dpToPx(1));
                    params.leftMargin = Utils.dpToPx(72);
                    divider.setLayoutParams(params);
                    divider.setBackgroundColor(dividerColor);
                    root.addView(divider);
                }
            }

            mainRoot.addView(root);
        }
    }

    @Override
    protected void onDestroy() {
        for (BigLineView bigLineView : bigLineViews) {
            bigLineView.clearListeners();
        }
        bigLineViews.clear();
        checkBoxes.clear();
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelableArray("data", datas);
        for (int i = 0; i < bigLineViews.size(); i++) {
            outState.putFloat(i + "_fromX", bigLineViews.get(i).getFromX());
            outState.putFloat(i + "_toX", bigLineViews.get(i).getToX());
        }
        for (int i = 0; i < checkBoxes.size(); i++) {
            outState.putBoolean(i + "_checked", checkBoxes.get(i).isChecked());
        }
    }
}
