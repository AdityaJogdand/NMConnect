package com.example.madproject;

import com.google.firebase.Timestamp;

public class InternalMark {
    private String id;
    private String subject_name;
    private String subject_code;
    private String faculty_sap_id;
    private String faculty_name;
    private String semester;
    private String branch;

    // ICA Components (stored as strings)
    private String Attendance;
    private String Lab_Exam;
    private String Mid_Term1;
    private String Mid_Term2;
    private String Presentation;
    private String Viva;
    private String total_marks;

    // Metadata
    private Timestamp last_updated;
    private String uploaded_by;

    // Empty constructor required for Firestore
    public InternalMark() {
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSubject_name() {
        return subject_name;
    }

    public void setSubject_name(String subject_name) {
        this.subject_name = subject_name;
    }

    public String getSubject_code() {
        return subject_code;
    }

    public void setSubject_code(String subject_code) {
        this.subject_code = subject_code;
    }

    public String getFaculty_sap_id() {
        return faculty_sap_id;
    }

    public void setFaculty_sap_id(String faculty_sap_id) {
        this.faculty_sap_id = faculty_sap_id;
    }

    public String getFaculty_name() {
        return faculty_name;
    }

    public void setFaculty_name(String faculty_name) {
        this.faculty_name = faculty_name;
    }

    public String getSemester() {
        return semester;
    }

    public void setSemester(String semester) {
        this.semester = semester;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public String getAttendance() {
        return Attendance;
    }

    public void setAttendance(String attendance) {
        Attendance = attendance;
    }

    public String getLab_Exam() {
        return Lab_Exam;
    }

    public void setLab_Exam(String lab_Exam) {
        Lab_Exam = lab_Exam;
    }

    public String getMid_Term1() {
        return Mid_Term1;
    }

    public void setMid_Term1(String mid_Term1) {
        Mid_Term1 = mid_Term1;
    }

    public String getMid_Term2() {
        return Mid_Term2;
    }

    public void setMid_Term2(String mid_Term2) {
        Mid_Term2 = mid_Term2;
    }

    public String getPresentation() {
        return Presentation;
    }

    public void setPresentation(String presentation) {
        Presentation = presentation;
    }

    public String getViva() {
        return Viva;
    }

    public void setViva(String viva) {
        Viva = viva;
    }

    public String getTotal_marks() {
        return total_marks;
    }

    public void setTotal_marks(String total_marks) {
        this.total_marks = total_marks;
    }

    public Timestamp getLast_updated() {
        return last_updated;
    }

    public void setLast_updated(Timestamp last_updated) {
        this.last_updated = last_updated;
    }

    public String getUploaded_by() {
        return uploaded_by;
    }

    public void setUploaded_by(String uploaded_by) {
        this.uploaded_by = uploaded_by;
    }

    // ✅ Helper method to get formatted marks string for display
    public String getFormattedMarks() {
        double total = calculateTotalValue();
        return String.format("%.1f/50", total);
    }

    // ✅ Calculate total from individual components dynamically
    private double calculateTotalValue() {
        double total = 0.0;

        total += parseDoubleSafe(Lab_Exam);
        total += parseDoubleSafe(Mid_Term1);
        total += parseDoubleSafe(Mid_Term2);
        total += parseDoubleSafe(Presentation);
        total += parseDoubleSafe(Viva);

        return total;
    }

    // ✅ Returns calculated total as string (for backward compatibility)
    public String calculateTotal() {
        return String.format("%.1f", calculateTotalValue());
    }

    // ✅ Helper for safe parsing
    private double parseDoubleSafe(String value) {
        if (value == null || value.trim().isEmpty()) return 0.0;
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    // ✅ Helper method to calculate percentage
    public double getPercentage() {
        double total = calculateTotalValue();
        return (total / 50.0) * 100.0;
    }
}
