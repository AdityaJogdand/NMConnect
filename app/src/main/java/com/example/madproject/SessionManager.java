package com.example.madproject;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {
    private static final String PREF_NAME = "app_session";
    private static final String KEY_LOGGED_IN = "is_logged_in";
    private static final String KEY_SAPID = "sap_id";
    private static final String KEY_ROLE = "role";
    private static final String KEY_ROLLNO = "roll_no";
    private static final String KEY_BRANCH = "branch";
    private static final String KEY_NAME = "name";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_SEMESTER = "semester";

    private final SharedPreferences prefs;
    private final SharedPreferences.Editor editor;

    public SessionManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = prefs.edit();
    }

    // Create login session with all user data
    public void createLoginSession(String sapId, String role, String rollNo, String branch, String name, String email, String semester) {
        editor.putBoolean(KEY_LOGGED_IN, true);
        editor.putString(KEY_SAPID, sapId);
        editor.putString(KEY_ROLE, role);
        editor.putString(KEY_ROLLNO, rollNo);
        editor.putString(KEY_BRANCH, branch);
        editor.putString(KEY_NAME, name);
        editor.putString(KEY_EMAIL, email);
        editor.putString(KEY_SEMESTER, semester);
        editor.apply();
    }

    // Getters
    public boolean isLoggedIn() {
        return prefs.getBoolean(KEY_LOGGED_IN, false);
    }

    public String getSapId() {
        return prefs.getString(KEY_SAPID, null);
    }

    public String getRole() {
        return prefs.getString(KEY_ROLE, null);
    }

    public String getRollNo() {
        return prefs.getString(KEY_ROLLNO, null);
    }

    public String getBranch() {
        return prefs.getString(KEY_BRANCH, null);
    }

    public String getName() {
        return prefs.getString(KEY_NAME, null);
    }

    public String getEmail() {
        return prefs.getString(KEY_EMAIL, null);
    }

    public String getSemester() {
        return prefs.getString(KEY_SEMESTER, null);
    }

    // Clear session data
    public void clearSession() {
        editor.clear();
        editor.apply();
    }
}