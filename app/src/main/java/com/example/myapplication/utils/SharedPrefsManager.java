package com.example.myapplication.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class SharedPrefsManager {
    private static final String PREF_NAME = "qlns_pref";

    // Keys
    private static final String KEY_TOKEN = "token";
    private static final String KEY_USER_ID = "userId";
    private static final String KEY_EMPLOYEE_ID = "employeeId";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_ROLE = "role";
    private static final String KEY_FULL_NAME = "fullName";
    private static final String KEY_AVATAR_URL = "avatarUrl";
    private static final String KEY_DEPT_ID = "departmentId";
    private static final String KEY_SAVED_USERNAME = "saved_username";

    private static SharedPrefsManager mInstance;
    private final SharedPreferences prefs;

    private SharedPrefsManager(Context context) {
        prefs = context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized SharedPrefsManager getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new SharedPrefsManager(context);
        }
        return mInstance;
    }

    // ── SAVE ──────────────────────────────────────────────────

    public void saveLoginData(String token, Long userId, Long employeeId,
                              String role, String fullName, String avatarUrl) {
        prefs.edit()
                .putString(KEY_TOKEN, token)
                .putLong(KEY_USER_ID, userId != null ? userId : -1)
                .putLong(KEY_EMPLOYEE_ID, employeeId != null ? employeeId : -1)
                .putString(KEY_ROLE, role)
                .putString(KEY_FULL_NAME, fullName)
                .putString(KEY_AVATAR_URL, avatarUrl)
                .apply();
    }

    public void saveDepartmentId(Long deptId) {
        prefs.edit().putLong(KEY_DEPT_ID, deptId).apply();
    }

    public void saveFullName(String fullName) {
        prefs.edit().putString(KEY_FULL_NAME, fullName).apply();
    }

    public void saveRememberUsername(String username) {
        prefs.edit().putString(KEY_SAVED_USERNAME, username).apply();
    }

    public void clearRememberUsername() {
        prefs.edit().remove(KEY_SAVED_USERNAME).apply();
    }

    // ── GET ───────────────────────────────────────────────────

    public String getToken()       { return prefs.getString(KEY_TOKEN, null); }
    public Long getUserId()        { return prefs.getLong(KEY_USER_ID, -1); }
    public Long getEmployeeId()    { return prefs.getLong(KEY_EMPLOYEE_ID, -1); }
    public String getRole()        { return prefs.getString(KEY_ROLE, ""); }
    public String getFullName()    { return prefs.getString(KEY_FULL_NAME, ""); }
    public String getAvatarUrl()   { return prefs.getString(KEY_AVATAR_URL, null); }
    public Long getDepartmentId()  { return prefs.getLong(KEY_DEPT_ID, -1); }
    public String getSavedUsername() { return prefs.getString(KEY_SAVED_USERNAME, ""); }

    // ── CHECK ─────────────────────────────────────────────────

    public boolean isLoggedIn()  { return getToken() != null; }
    public boolean isAdmin()     { return "ADMIN".equals(getRole()); }
    public boolean isManager()   { return "MANAGER".equals(getRole()); }
    public boolean isAdminOrManager() { return isAdmin() || isManager(); }

    // ── LOGOUT ────────────────────────────────────────────────

    public void logout() {
        String savedUsername = getSavedUsername();
        prefs.edit().clear().apply();
        if (savedUsername != null && !savedUsername.isEmpty()) {
            saveRememberUsername(savedUsername);
        }
    }
}