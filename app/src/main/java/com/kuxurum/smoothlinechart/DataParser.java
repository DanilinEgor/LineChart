package com.kuxurum.smoothlinechart;

import android.content.Context;
import android.graphics.Color;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

class DataParser {
    static Data[] parse(Context context) throws IOException, JSONException {
        long start = System.currentTimeMillis();

        InputStream stream = context.getResources().getAssets().open("chart_data.json");
        int size = stream.available();
        byte[] buffer = new byte[size];
        stream.read(buffer);
        stream.close();
        String json = new String(buffer);
        JSONArray array = new JSONArray(json);

        Data[] res = new Data[array.length()];
        for (int i = 0; i < array.length(); i++) {
            Wrapper wrapper = new Wrapper();

            JSONObject root = array.getJSONObject(i);
            JSONArray columns = root.getJSONArray("columns");
            for (int j = 0; j < columns.length(); j++) {
                Wrapper.WrapperColumn column = new Wrapper.WrapperColumn();

                JSONArray a = columns.getJSONArray(j);
                column.label = a.getString(0);
                column.values = new long[a.length() - 1];
                for (int k = 1; k < a.length(); k++) {
                    column.values[k - 1] = a.getLong(k);
                }
                wrapper.columns.add(column);
            }

            Data data = new Data();
            data.columns = new Data.Column[columns.length()];

            JSONObject names = root.getJSONObject("names");
            JSONObject colors = root.getJSONObject("colors");
            JSONObject types = root.getJSONObject("types");

            for (int j = 0; j < wrapper.columns.size(); j++) {
                Wrapper.WrapperColumn column = wrapper.columns.get(j);
                if (types.getString(column.label).equals("x") && j != 0) {
                    Collections.swap(wrapper.columns, 0, j);
                    break;
                }
            }

            for (int j = 0; j < wrapper.columns.size(); j++) {
                Data.Column dataCol = new Data.Column();

                Wrapper.WrapperColumn column = wrapper.columns.get(j);
                dataCol.value = column.values;
                dataCol.name = names.optString(column.label);
                dataCol.color = Color.parseColor(colors.optString(column.label, "#000000"));

                data.columns[j] = dataCol;
            }

            res[i] = data;
        }

        long end = System.currentTimeMillis();
        //Log.v("Parser", "parser time=" + (end - start) + "ms");
        return res;
    }

    private static class Wrapper {
        List<WrapperColumn> columns = new ArrayList<>();

        static class WrapperColumn {
            String label;
            long[] values;
        }
    }
}
