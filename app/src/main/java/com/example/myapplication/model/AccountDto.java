package com.example.myapplication.model;

import com.google.gson.annotations.SerializedName;

public class AccountDto {

    public static class UpdateRoleRequest {
        @SerializedName("role")
        public String role;

        public UpdateRoleRequest(String role) {
            this.role = role;
        }
    }
}
