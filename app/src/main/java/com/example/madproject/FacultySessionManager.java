package com.example.madproject;

import android.content.Context;
import android.content.SharedPreferences;

public class FacultySessionManager {
    private static final String PREF_NAME = "FacultySession";
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    private static final String KEY_FACULTY_ID = "facultyId";
    private static final String KEY_NAME = "name";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_PHONE = "phone";
    private static final String KEY_DEPARTMENT = "department";
    private static final String KEY_ROLE = "role";

    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
    private Context context;

    public FacultySessionManager(Context context) {
        this.context = context;
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
    }

    /**
     * Create faculty login session
     */
    public void createLoginSession(String facultyId, String name, String email,
                                   String phone, String department, String role) {
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putString(KEY_FACULTY_ID, facultyId);
        editor.putString(KEY_NAME, name);
        editor.putString(KEY_EMAIL, email);
        editor.putString(KEY_PHONE, phone);
        editor.putString(KEY_DEPARTMENT, department);
        editor.putString(KEY_ROLE, role);
        editor.apply();
    }

    /**
     * Check if faculty is logged in
     */
    public boolean isLoggedIn() {
        return sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    /**
     * Get faculty ID
     */
    public String getFacultyId() {
        return sharedPreferences.getString(KEY_FACULTY_ID, null);
    }

    /**
     * Get faculty name
     */
    public String getName() {
        return sharedPreferences.getString(KEY_NAME, null);
    }

    /**
     * Get faculty email
     */
    public String getEmail() {
        return sharedPreferences.getString(KEY_EMAIL, null);
    }

    /**
     * Get faculty phone
     */
    public String getPhone() {
        return sharedPreferences.getString(KEY_PHONE, null);
    }

    /**
     * Get faculty department
     */
    public String getDepartment() {
        return sharedPreferences.getString(KEY_DEPARTMENT, null);
    }

    /**
     * Get faculty role
     */
    public String getRole() {
        return sharedPreferences.getString(KEY_ROLE, "Faculty");
    }

    /**
     * Update faculty name
     */
    public void updateName(String name) {
        editor.putString(KEY_NAME, name);
        editor.apply();
    }

    /**
     * Update faculty email
     */
    public void updateEmail(String email) {
        editor.putString(KEY_EMAIL, email);
        editor.apply();
    }

    /**
     * Update faculty phone
     */
    public void updatePhone(String phone) {
        editor.putString(KEY_PHONE, phone);
        editor.apply();
    }

    /**
     * Update faculty department
     */
    public void updateDepartment(String department) {
        editor.putString(KEY_DEPARTMENT, department);
        editor.apply();
    }

    /**
     * Clear session and logout
     */
    public void logout() {
        editor.clear();
        editor.apply();
    }

    /**
     * Check if this is the first login
     */
    public boolean isFirstLogin() {
        return sharedPreferences.getBoolean("first_login", true);
    }

    /**
     * Set first login flag to false
     */
    public void setFirstLoginComplete() {
        editor.putBoolean("first_login", false);
        editor.apply();
    }
}