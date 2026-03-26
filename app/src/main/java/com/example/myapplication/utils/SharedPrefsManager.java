package com.example.myapplication.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class SharedPrefsManager {

    private static final String PREF_NAME = "qlns_pref";
    private static final String KEY_TOKEN = "token";
    private static final String KEY_USER_ID = "user_id"; // canonical key for user id
    private static final String KEY_USER_ID_LEGACY = "userId"; // keep compatibility with old writes
    private static final String KEY_USERNAME = "username";
    private static final String KEY_ROLE = "role";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_DEPT_ID = "departmentId";
    private static final String KEY_EMPLOYEE_ID = "employeeId";

    private static SharedPrefsManager mInstance;
    private static Context mCtx;

    private SharedPrefsManager(Context context) {
        mCtx = context.getApplicationContext();
    }

    public static synchronized SharedPrefsManager getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new SharedPrefsManager(context);
        }
        return mInstance;
    }

    public void saveUserLogin(String token, Long userId, String username, String email, String role, Long departmentId, Long employeeId) {
        SharedPreferences sharedPreferences = mCtx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        editor.putString(KEY_TOKEN, token);
        // store both canonical and legacy keys so old screens continue to work
        editor.putLong(KEY_USER_ID, userId);
        editor.putLong(KEY_USER_ID_LEGACY, userId);
        editor.putString(KEY_USERNAME, username);
        editor.putString(KEY_EMAIL, email);
        editor.putString(KEY_ROLE, role);
        if (departmentId != null) editor.putLong(KEY_DEPT_ID, departmentId);
        if (employeeId != null) editor.putLong(KEY_EMPLOYEE_ID, employeeId);
        editor.apply();
    }

    public boolean isLoggedIn() {
        SharedPreferences sharedPreferences = mCtx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return sharedPreferences.getString(KEY_TOKEN, null) != null;
    }

    public String getToken() {
        SharedPreferences sharedPreferences = mCtx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return sharedPreferences.getString(KEY_TOKEN, null);
    }
    
    public Long getDepartmentId() {
        SharedPreferences sharedPreferences = mCtx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return sharedPreferences.getLong(KEY_DEPT_ID, -1L);
    }
    
    public String getUsername() {
        SharedPreferences sharedPreferences = mCtx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return sharedPreferences.getString(KEY_USERNAME, "");
    }
    
    public Long getUserId() {
        SharedPreferences sharedPreferences = mCtx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        long userId = sharedPreferences.getLong(KEY_USER_ID, -1L);
        if (userId <= 0) {
            userId = sharedPreferences.getLong(KEY_USER_ID_LEGACY, -1L);
        }
        return userId;
    }
    
    public String getRole() {
        SharedPreferences sharedPreferences = mCtx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return sharedPreferences.getString(KEY_ROLE, "");
    }

    public Long getEmployeeId() {
        SharedPreferences sharedPreferences = mCtx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return sharedPreferences.getLong(KEY_EMPLOYEE_ID, -1L);
    }

    public void logout() {
        SharedPreferences sharedPreferences = mCtx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();
    }
}
