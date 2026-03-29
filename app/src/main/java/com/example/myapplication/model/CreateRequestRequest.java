package com.example.myapplication.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class CreateRequestRequest {

    @SerializedName("title")
    private String title;

    @SerializedName("description")
    private String description;

    @SerializedName("type")
    private String type;

    @SerializedName("details")
    private List<RequestDetail> details;

    @SerializedName("fileUrl")
    private String fileUrl;

    @SerializedName("fileName")
    private String fileName;

    // Constructor 2 tham số (tiêu đề + mô tả)
    public CreateRequestRequest(String title, String description) {
        this.title = title;
        this.description = description;
    }

    // Constructor 4 tham số (dùng bởi RequestActivity)
    public CreateRequestRequest(String title, RequestType type, List<RequestDetail> details, String description) {
        this.title = title;
        this.type = type != null ? type.name() : null;
        this.details = details;
        this.description = description;
    }

    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getType() { return type; }
    public List<RequestDetail> getDetails() { return details; }
    public String getFileUrl() { return fileUrl; }
    public String getFileName() { return fileName; }

    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setType(String type) { this.type = type; }
    public void setDetails(List<RequestDetail> details) { this.details = details; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }
    public void setFileName(String fileName) { this.fileName = fileName; }
}
