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

    /**
     * Creates a new login session for any user type.
     * The same method is used for Student, CR, and Faculty users.
     */
    public void createLoginSession(String sapId, String role, String rollNo,
                                   String branch, String name, String email, String semester) {
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

    // -------------------------------
    // ✅ Session Update Methods
    // -------------------------------

    public void updateField(String key, String value) {
        editor.putString(key, value);
        editor.apply();
    }

    public void updateName(String newName) {
        updateField(KEY_NAME, newName);
    }

    public void updateBranch(String newBranch) {
        updateField(KEY_BRANCH, newBranch);
    }

    public void updateEmail(String newEmail) {
        updateField(KEY_EMAIL, newEmail);
    }

    // -------------------------------
    // ✅ Getters
    // -------------------------------

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

    // -------------------------------
    // ✅ Helpers
    // -------------------------------

    /**
     * Returns a normalized user type.
     * Possible values: "Faculty", "CR", "Student", or "Unknown"
     */
    public String getUserType() {
        String role = getRole();
        if (role == null) return "Unknown";

        if (role.equalsIgnoreCase("faculty") || role.equalsIgnoreCase("teacher"))
            return "Faculty";
        if (role.equalsIgnoreCase("cr"))
            return "CR";
        if (role.equalsIgnoreCase("student"))
            return "Student";

        return "Unknown";
    }

    // -------------------------------
    // ✅ Logout / Clear Session
    // -------------------------------

    public void clearSession() {
        editor.clear();
        editor.apply();
    }
}
