package com.example.myapplication.model;

import com.google.gson.annotations.SerializedName;

public class UpdateTaskStatusRequest {
    @SerializedName("status")
    private String status;
    @SerializedName("note")
    private String note;

    public UpdateTaskStatusRequest(String status, String note) {
        this.status = status;
        this.note = note;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
