package com.example.myapplication.model;

import com.google.gson.annotations.SerializedName;

public class CreateRequestRequest {

    @SerializedName("title")
    private final String title;

    @SerializedName("description")
    private final String description;

    public CreateRequestRequest(String title, String description) {
        this.title = title;
        this.description = description;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }
}

