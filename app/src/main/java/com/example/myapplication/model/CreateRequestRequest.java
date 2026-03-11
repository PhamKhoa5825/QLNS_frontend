package com.example.myapplication.model;

import com.google.gson.annotations.SerializedName;

public class CreateRequestRequest {
    @SerializedName("title")
    private String title;
    @SerializedName("description")
    private String description;
    @SerializedName("fileUrl")
    private String fileUrl;
    @SerializedName("fileName")
    private String fileName;

    public CreateRequestRequest(String title, String description) {
        this.title = title;
        this.description = description;
    }

    // Getters and Setters
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
}
