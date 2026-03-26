package com.example.myapplication.model;

import com.google.gson.annotations.SerializedName;

public class VoteRequest {
    @SerializedName("optionIndex")
    private int optionIndex;

    public VoteRequest(int optionIndex) {
        this.optionIndex = optionIndex;
    }

    public int getOptionIndex() {
        return optionIndex;
    }
}

