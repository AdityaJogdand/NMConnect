package com.example.madproject;

import com.google.firebase.Timestamp;

public class Announcement {
    private String id;
    private String title;
    private String message;
    private Timestamp date;
    private Timestamp expires_at;
    private boolean is_active;
    private String posted_by;
    private String posted_by_name;
    private String posted_by_role;
    private String priority;

    public Announcement() {
        // Empty constructor needed for Firestore
    }

    public Announcement(String id, String title, String message, Timestamp date,
                        Timestamp expires_at, boolean is_active, String posted_by,
                        String posted_by_name, String posted_by_role, String priority) {
        this.id = id;
        this.title = title;
        this.message = message;
        this.date = date;
        this.expires_at = expires_at;
        this.is_active = is_active;
        this.posted_by = posted_by;
        this.posted_by_name = posted_by_name;
        this.posted_by_role = posted_by_role;
        this.priority = priority;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Timestamp getDate() { return date; }
    public void setDate(Timestamp date) { this.date = date; }

    public Timestamp getExpires_at() { return expires_at; }
    public void setExpires_at(Timestamp expires_at) { this.expires_at = expires_at; }

    public boolean isIs_active() { return is_active; }
    public void setIs_active(boolean is_active) { this.is_active = is_active; }

    public String getPosted_by() { return posted_by; }
    public void setPosted_by(String posted_by) { this.posted_by = posted_by; }

    public String getPosted_by_name() { return posted_by_name; }
    public void setPosted_by_name(String posted_by_name) { this.posted_by_name = posted_by_name; }

    public String getPosted_by_role() { return posted_by_role; }
    public void setPosted_by_role(String posted_by_role) { this.posted_by_role = posted_by_role; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
}