package com.kuxurum.smoothlinechart;

import android.os.Parcel;
import android.os.Parcelable;

public class Data implements Parcelable {
    Column[] columns;

    public static class Column implements Parcelable {
        String name;
        long[] value;
        int color;

        public Column() {
        }

        protected Column(Parcel in) {
            name = in.readString();
            value = in.createLongArray();
            color = in.readInt();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(name);
            dest.writeLongArray(value);
            dest.writeInt(color);
        }

        @Override
        public int describeContents() {
            return 0;
        }

        public static final Creator<Column> CREATOR = new Creator<Column>() {
            @Override
            public Column createFromParcel(Parcel in) {
                return new Column(in);
            }

            @Override
            public Column[] newArray(int size) {
                return new Column[size];
            }
        };
    }

    public Data() {
    }

    protected Data(Parcel in) {
        columns = in.createTypedArray(Column.CREATOR);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeTypedArray(columns, flags);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Data> CREATOR = new Creator<Data>() {
        @Override
        public Data createFromParcel(Parcel in) {
            return new Data(in);
        }

        @Override
        public Data[] newArray(int size) {
            return new Data[size];
        }
    };
}
