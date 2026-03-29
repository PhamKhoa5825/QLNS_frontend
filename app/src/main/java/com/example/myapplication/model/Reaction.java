package com.example.myapplication.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Reaction {

    @SerializedName("emoji")
    private String emoji;

    @SerializedName("count")
    private Integer count;

    @SerializedName("userIds")
    private List<Long> userIds;

    @SerializedName("userNames")
    private List<String> userNames;

    public String getEmoji() {
        return emoji;
    }

    public void setEmoji(String emoji) {
        this.emoji = emoji;
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    public List<Long> getUserIds() {
        return userIds;
    }

    public void setUserIds(List<Long> userIds) {
        this.userIds = userIds;
    }

    public List<String> getUserNames() {
        return userNames;
    }

    public void setUserNames(List<String> userNames) {
        this.userNames = userNames;
    }
}
