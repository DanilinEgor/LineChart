package com.kuxurum.smoothlinechart;

public class Data implements Cloneable {
    Column[] columns;
    boolean percentage;
    boolean stacked;
    boolean yScaled;
    boolean hasBars;

    public static class Column {
        String name;
        long[] value;
        int color;

        public Column() {
        }
    }

    public Data() {
    }

    @Override
    public Data clone() {
        try {
            return (Data) super.clone();
        } catch (Exception e) {
            return null;
        }
    }
}
