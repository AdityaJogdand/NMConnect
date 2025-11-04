package com.example.madproject;

public class FacultySubject {
    private String subjectId;
    private String subjectName;
    private String subjectCode;
    private String branch;
    private String semester;
    private boolean isActive;
    private boolean isSelected;

    public FacultySubject() {
        // Default constructor required for Firestore
    }

    public FacultySubject(String subjectId, String subjectName, String subjectCode,
                          String branch, String semester, boolean isActive) {
        this.subjectId = subjectId;
        this.subjectName = subjectName;
        this.subjectCode = subjectCode;
        this.branch = branch;
        this.semester = semester;
        this.isActive = isActive;
        this.isSelected = false;
    }

    // Getters
    public String getSubjectId() {
        return subjectId;
    }

    public String getSubjectName() {
        return subjectName;
    }

    public String getSubjectCode() {
        return subjectCode;
    }

    public String getBranch() {
        return branch;
    }

    public String getSemester() {
        return semester;
    }

    public boolean isActive() {
        return isActive;
    }

    public boolean isSelected() {
        return isSelected;
    }

    // Setters
    public void setSubjectId(String subjectId) {
        this.subjectId = subjectId;
    }

    public void setSubjectName(String subjectName) {
        this.subjectName = subjectName;
    }

    public void setSubjectCode(String subjectCode) {
        this.subjectCode = subjectCode;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public void setSemester(String semester) {
        this.semester = semester;
    }

    public void setIsActive(boolean isActive) {
        this.isActive = isActive;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }

    @Override
    public String toString() {
        return subjectName + " (" + subjectCode + ") - " + branch + " " + semester;
    }
}