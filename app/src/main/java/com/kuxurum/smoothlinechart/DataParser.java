package com.kuxurum.smoothlinechart;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.Types;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import okio.BufferedSource;
import okio.Okio;

class DataParser {
    static Data[] parse(Context context) throws IOException {
        long start = System.currentTimeMillis();
        BufferedSource buffer = Okio.buffer(
                Okio.source(context.getResources().getAssets().open("chart_data.json")));
        Moshi moshi = new Moshi.Builder().build();
        Type type = Types.newParameterizedType(List.class, Wrapper.class);
        JsonAdapter<List<Wrapper>> adapter = moshi.adapter(type);
        List<Wrapper> wrappers = adapter.fromJson(buffer);
        Log.v("Parser", "parser1 time=" + (System.currentTimeMillis() - start) + "ms");
        Data[] res = new Data[wrappers.size()];
        for (int k = 0; k < wrappers.size(); k++) {
            Wrapper wrapper = wrappers.get(k);
            Data data = new Data();
            data.columns = new Data.Column[wrapper.columns.size()];
            for (int i = 0; i < wrapper.columns.size(); i++) {
                Data.Column column = new Data.Column();
                List<Object> y = wrapper.columns.get(i);
                long[] value = new long[y.size() - 1];
                for (int j = 0; j < value.length; j++) {
                    value[j] = ((Double) y.get(j + 1)).longValue();
                }
                column.value = value;

                column.name = (String) y.get(0);

                data.columns[i] = column;
            }

            for (int i = 0; i < data.columns.length; i++) {
                Data.Column column = data.columns[i];
                Data.Column tmp;
                if (wrapper.types.get(column.name).equals("x") && i != 0) {
                    tmp = column;
                    data.columns[i] = data.columns[0];
                    data.columns[0] = tmp;
                    break;
                }
            }

            for (int i = 0; i < data.columns.length; i++) {
                Data.Column column = data.columns[i];
                String label = column.name;
                column.name = wrapper.names.get(label);

                String color = wrapper.colors.get(label);
                if (color == null) color = "#000000";
                column.color = Color.parseColor(color);
            }

            res[k] = data;
        }

        long end = System.currentTimeMillis();
        Log.v("Parser", "parser2 time=" + (end - start) + "ms");
        return res;
    }

    static class Wrapper {
        List<List<Object>> columns;
        Map<String, String> types;
        Map<String, String> names;
        Map<String, String> colors;
    }
}
