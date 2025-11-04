package com.example.madproject;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.concurrent.atomic.AtomicBoolean;

public class SubjectDetailsActivity extends AppCompatActivity {

    private TextView tvSubjectName, tvSubjectCode, tvBranch, tvSemester;
    private TextView tvFacultyName, tvFacultyDept, tvFacultyEmail, tvFacultyPhone;
    private TextView tvAttendance, tvLabExam, tvMidTerm1, tvMidTerm2;
    private TextView tvPresentation, tvViva, tvTotalMarks, tvPercentage;
    private ImageView btnBack;
    private View facultySection, marksSection;
    private TextView tvNoData;

    private FirebaseFirestore db;
    private String subjectName, subjectCode, facultySapId, facultyName, studentSapId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_subject_details);

        db = FirebaseFirestore.getInstance();

        initializeViews();
        getIntentData();

        if (facultySapId != null && !facultySapId.isEmpty()) {
            loadFacultyDetails();
        } else {
            fetchFacultySapIdFromSubject();
        }

        btnBack.setOnClickListener(v -> finish());
    }

    private void initializeViews() {
        btnBack = findViewById(R.id.btnBack);
        tvSubjectName = findViewById(R.id.tvSubjectName);
        tvSubjectCode = findViewById(R.id.tvSubjectCode);
        tvBranch = findViewById(R.id.tvBranch);
        tvSemester = findViewById(R.id.tvSemester);

        facultySection = findViewById(R.id.facultySection);
        tvFacultyName = findViewById(R.id.tvFacultyName);
        tvFacultyDept = findViewById(R.id.tvFacultyDept);
        tvFacultyEmail = findViewById(R.id.tvFacultyEmail);
        tvFacultyPhone = findViewById(R.id.tvFacultyPhone);

        marksSection = findViewById(R.id.marksSection);
        tvAttendance = findViewById(R.id.tvAttendance);
        tvLabExam = findViewById(R.id.tvLabExam);
        tvMidTerm1 = findViewById(R.id.tvMidTerm1);
        tvMidTerm2 = findViewById(R.id.tvMidTerm2);
        tvPresentation = findViewById(R.id.tvPresentation);
        tvViva = findViewById(R.id.tvViva);
        tvTotalMarks = findViewById(R.id.tvTotalMarks);
        tvPercentage = findViewById(R.id.tvPercentage);

        tvNoData = findViewById(R.id.tvNoData);
    }

    private void getIntentData() {
        SessionManager session = new SessionManager(this);
        studentSapId = session.getSapId();

        subjectName = getIntent().getStringExtra("subject_name");
        subjectCode = getIntent().getStringExtra("subject_code");
        facultySapId = getIntent().getStringExtra("faculty_sap_id");
        facultyName = getIntent().getStringExtra("faculty_name");
        String branch = getIntent().getStringExtra("branch");
        String semester = getIntent().getStringExtra("semester");

        tvSubjectName.setText(subjectName != null ? subjectName : "N/A");
        tvSubjectCode.setText(subjectCode != null ? subjectCode : "N/A");
        tvBranch.setText(branch != null ? branch : "N/A");
        tvSemester.setText(semester != null ? semester : "N/A");

        String attendance = getIntent().getStringExtra("attendance");
        if (attendance != null) {
            displayPassedMarks();
        } else {
            loadInternalMarks(studentSapId, subjectName);
        }
    }

    private void displayPassedMarks() {
        String attendance = getIntent().getStringExtra("attendance");
        String labExam = getIntent().getStringExtra("lab_exam");
        String midTerm1 = getIntent().getStringExtra("mid_term1");
        String midTerm2 = getIntent().getStringExtra("mid_term2");
        String presentation = getIntent().getStringExtra("presentation");
        String viva = getIntent().getStringExtra("viva");
        String totalMarks = getIntent().getStringExtra("total_marks");
        double percentage = getIntent().getDoubleExtra("percentage", 0.0);

        // ✅ Show attendance directly, not as marks
        tvAttendance.setText(attendance != null ? attendance : "N/A");
        tvLabExam.setText(formatMarkValue(labExam));
        tvMidTerm1.setText(formatMarkValue(midTerm1));
        tvMidTerm2.setText(formatMarkValue(midTerm2));
        tvPresentation.setText(formatMarkValue(presentation));
        tvViva.setText(formatMarkValue(viva));
        tvTotalMarks.setText(formatMarkValue(totalMarks) + "/50");
        tvPercentage.setText(String.format("%.2f%%", percentage));

        marksSection.setVisibility(View.VISIBLE);
        tvNoData.setVisibility(View.GONE);

        if (facultyName != null && !facultyName.isEmpty()) {
            tvFacultyName.setText(facultyName);
            facultySection.setVisibility(View.VISIBLE);
        }
    }

    private void fetchFacultySapIdFromSubject() {
        SessionManager session = new SessionManager(this);
        String branch = session.getBranch();
        String semester = session.getSemester();

        if (subjectName == null || branch == null || semester == null) return;

        AtomicBoolean facultyFound = new AtomicBoolean(false);

        db.collection("Faculty")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        facultySection.setVisibility(View.GONE);
                        return;
                    }

                    int totalFaculties = querySnapshot.size();
                    int[] checkedCount = {0};

                    for (DocumentSnapshot facultyDoc : querySnapshot.getDocuments()) {
                        String facultyId = facultyDoc.getId();
                        db.collection("Faculty")
                                .document(facultyId)
                                .collection("Subjects")
                                .document(subjectName)
                                .get()
                                .addOnSuccessListener(subjectDoc -> {
                                    checkedCount[0]++;
                                    if (subjectDoc.exists()) {
                                        String docBranch = subjectDoc.getString("branch");
                                        String docSemester = subjectDoc.getString("semester");
                                        String docSubjectCode = subjectDoc.getString("subject_code");

                                        boolean branchMatch = branch.equals(docBranch);
                                        boolean semesterMatch = semester.equals(docSemester);
                                        boolean codeMatch = (subjectCode == null || subjectCode.equals(docSubjectCode));

                                        if (branchMatch && semesterMatch && codeMatch && !facultyFound.get()) {
                                            if (facultyFound.compareAndSet(false, true)) {
                                                facultySapId = facultyId;
                                                loadFacultyDetails();
                                            }
                                        }
                                    }

                                    if (checkedCount[0] == totalFaculties && !facultyFound.get()) {
                                        facultySection.setVisibility(View.GONE);
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    checkedCount[0]++;
                                    if (checkedCount[0] == totalFaculties && !facultyFound.get()) {
                                        facultySection.setVisibility(View.GONE);
                                    }
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error finding faculty", Toast.LENGTH_SHORT).show();
                    facultySection.setVisibility(View.GONE);
                });
    }

    private void loadFacultyDetails() {
        if (facultySapId == null || facultySapId.isEmpty()) {
            facultySection.setVisibility(View.GONE);
            return;
        }

        db.collection("Faculty")
                .document(facultySapId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String name = documentSnapshot.getString("Name");
                        if (name == null) name = documentSnapshot.getString("name");

                        String dept = documentSnapshot.getString("department");
                        if (dept == null) dept = documentSnapshot.getString("Department");

                        String email = documentSnapshot.getString("email");
                        if (email == null) email = documentSnapshot.getString("Email");

                        String phone = documentSnapshot.getString("phone");
                        if (phone == null) phone = documentSnapshot.getString("Phone");

                        tvFacultyName.setText(name != null ? name : "N/A");
                        tvFacultyDept.setText(dept != null ? dept : "N/A");
                        tvFacultyEmail.setText(email != null ? email : "N/A");
                        tvFacultyPhone.setText(phone != null ? phone : "N/A");

                        facultySection.setVisibility(View.VISIBLE);
                    } else {
                        facultySection.setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading faculty details", Toast.LENGTH_SHORT).show();
                    facultySection.setVisibility(View.GONE);
                });
    }

    private void loadInternalMarks(String sapId, String subjectName) {
        if (sapId == null || subjectName == null) {
            showNoData();
            return;
        }

        db.collection("Student")
                .document(sapId)
                .collection("Internal Component Assessment ")
                .document(subjectName)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        InternalMark mark = documentSnapshot.toObject(InternalMark.class);
                        if (mark != null) {
                            displayMarks(mark);
                            marksSection.setVisibility(View.VISIBLE);
                            tvNoData.setVisibility(View.GONE);
                        } else {
                            showNoData();
                        }
                    } else {
                        showNoData();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading marks", Toast.LENGTH_SHORT).show();
                    showNoData();
                });
    }

    private void displayMarks(InternalMark mark) {
        // ✅ Attendance is shown as-is (not numeric)
        tvAttendance.setText(mark.getAttendance() != null ? mark.getAttendance() : "N/A");
        tvLabExam.setText(formatMarkValue(mark.getLab_Exam()));
        tvMidTerm1.setText(formatMarkValue(mark.getMid_Term1()));
        tvMidTerm2.setText(formatMarkValue(mark.getMid_Term2()));
        tvPresentation.setText(formatMarkValue(mark.getPresentation()));
        tvViva.setText(formatMarkValue(mark.getViva()));

        String total = mark.calculateTotal();
        double percentage = mark.getPercentage();

        tvTotalMarks.setText(formatMarkValue(total) + "/50");
        tvPercentage.setText(String.format("%.2f%%", percentage));
    }

    private String formatMarkValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "0";
        }

        try {
            double doubleValue = Double.parseDouble(value.trim());
            if (doubleValue == (int) doubleValue) {
                return String.valueOf((int) doubleValue);
            } else {
                return String.format("%.1f", doubleValue);
            }
        } catch (NumberFormatException e) {
            return value.trim();
        }
    }

    private void showNoData() {
        marksSection.setVisibility(View.GONE);
        tvNoData.setVisibility(View.VISIBLE);
    }
}
