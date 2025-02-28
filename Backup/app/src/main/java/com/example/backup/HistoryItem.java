package com.example.backup;

import android.os.Parcel;
import android.os.Parcelable;

public class HistoryItem implements Parcelable {
    private int imageResource;
    private String title;
    private String description;

    public HistoryItem(int imageResource, String title, String description) {
        this.imageResource = imageResource;
        this.title = title;
        this.description = description;
    }

    protected HistoryItem(Parcel in) {
        imageResource = in.readInt();
        title = in.readString();
        description = in.readString();
    }

    public static final Creator<HistoryItem> CREATOR = new Creator<HistoryItem>() {
        @Override
        public HistoryItem createFromParcel(Parcel in) {
            return new HistoryItem(in);
        }

        @Override
        public HistoryItem[] newArray(int size) {
            return new HistoryItem[size];
        }
    };

    public int getImageResource() {
        return imageResource;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(imageResource);
        dest.writeString(title);
        dest.writeString(description);
    }
}
