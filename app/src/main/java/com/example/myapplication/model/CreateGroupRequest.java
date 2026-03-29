package com.example.myapplication.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class CreateGroupRequest {

    @SerializedName("name")
    private String name;

    @SerializedName("memberUserIds")
    private List<Long> memberUserIds;

    public CreateGroupRequest(String name, List<Long> memberUserIds) {
        this.name = name;
        this.memberUserIds = memberUserIds;
    }

    public String getName() {
        return name;
    }

    public List<Long> getMemberUserIds() {
        return memberUserIds;
    }
}
