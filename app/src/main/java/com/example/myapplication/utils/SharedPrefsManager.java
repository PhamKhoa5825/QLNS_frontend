package com.example.myapplication.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class SharedPrefsManager {

    private static final String PREF_NAME = "qlns_prefs";
    private static final String KEY_TOKEN = "jwt_token";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_ROLE = "role";
    private static final String KEY_DEPT_ID = "dept_id";

    private static SharedPrefsManager mInstance;
    private static Context mCtx;

    private SharedPrefsManager(Context context) {
        mCtx = context;
    }

    public static synchronized SharedPrefsManager getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new SharedPrefsManager(context);
        }
        return mInstance;
    }

    public void saveUserLogin(String token, Long userId, String username, String role) {
        SharedPreferences sharedPreferences = mCtx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        editor.putString(KEY_TOKEN, token);
        editor.putLong(KEY_USER_ID, userId);
        editor.putString(KEY_USERNAME, username);
        editor.putString(KEY_ROLE, role);
        editor.apply();
    }
    
    // Default dept id for testing, real app sets this during login or fetch user details
    public void setDepartmentId(Long deptId) {
        SharedPreferences sharedPreferences = mCtx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putLong(KEY_DEPT_ID, deptId);
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
        return sharedPreferences.getLong(KEY_DEPT_ID, -1L); // Default return -1L if not set
    }
    
    public String getUsername() {
        SharedPreferences sharedPreferences = mCtx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return sharedPreferences.getString(KEY_USERNAME, "");
    }
    
    public Long getUserId() {
        SharedPreferences sharedPreferences = mCtx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return sharedPreferences.getLong(KEY_USER_ID, -1L);
    }
    
    public String getRole() {
        SharedPreferences sharedPreferences = mCtx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return sharedPreferences.getString(KEY_ROLE, "");
    }

    public void logout() {
        SharedPreferences sharedPreferences = mCtx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();
    }
}
