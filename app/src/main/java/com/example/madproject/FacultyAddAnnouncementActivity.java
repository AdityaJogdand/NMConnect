package com.example.madproject;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class FacultyAddAnnouncementActivity extends AppCompatActivity {

    EditText etTitle, etMessage, etExpiryDate;
    RadioGroup rgPriority;
    RadioButton rbHigh, rbNormal, rbLow;
    Button btnPost, btnCancel;
    RecyclerView subjectsRecyclerView;
    FacultyAnnouncementSubjectAdapter subjectAdapter;

    private FirebaseFirestore db;
    private List<FacultySubject> subjectList;
    private Calendar expiryDate;
    private String facultyId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_faculty_add_announcement);

        db = FirebaseFirestore.getInstance();
        subjectList = new ArrayList<>();
        expiryDate = Calendar.getInstance();
        expiryDate.add(Calendar.DAY_OF_MONTH, 7); // Default 7 days

        facultyId = getIntent().getStringExtra("facultyId");

        initializeViews();
        setupViews();
        loadFacultySubjects();

        etExpiryDate.setOnClickListener(v -> showDatePicker());
        btnPost.setOnClickListener(v -> submitAnnouncement());
        btnCancel.setOnClickListener(v -> finish());
    }

    private void initializeViews() {
        etTitle = findViewById(R.id.etTitle);
        etMessage = findViewById(R.id.etMessage);
        etExpiryDate = findViewById(R.id.etExpiryDate);
        rgPriority = findViewById(R.id.rgPriority);
        rbHigh = findViewById(R.id.rbHigh);
        rbNormal = findViewById(R.id.rbNormal);
        rbLow = findViewById(R.id.rbLow);
        btnPost = findViewById(R.id.btnPost);
        btnCancel = findViewById(R.id.btnCancel);
        subjectsRecyclerView = findViewById(R.id.subjectsRecyclerView);
    }

    private void setupViews() {
        updateExpiryDateDisplay();
        rbNormal.setChecked(true);

        subjectsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        subjectAdapter = new FacultyAnnouncementSubjectAdapter(subjectList);
        subjectsRecyclerView.setAdapter(subjectAdapter);
    }

    private void loadFacultySubjects() {
        if (facultyId == null) {
            Toast.makeText(this, "Faculty ID not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        db.collection("Faculty")
                .document(facultyId)
                .collection("Subjects")
                .whereEqualTo("is_active", true)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    subjectList.clear();
                    for (DocumentSnapshot doc : querySnapshot) {
                        FacultySubject subject = new FacultySubject();
                        subject.setSubjectId(doc.getId());
                        subject.setSubjectName(doc.getId());
                        subject.setSubjectCode(doc.getString("subject_code"));
                        subject.setBranch(doc.getString("branch"));
                        subject.setSemester(doc.getString("semester"));
                        subject.setIsActive(true);
                        subject.setSelected(false);
                        subjectList.add(subject);
                    }

                    if (subjectList.isEmpty()) {
                        Toast.makeText(this, "No active subjects found", Toast.LENGTH_LONG).show();
                    }

                    subjectAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading subjects: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void showDatePicker() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    expiryDate.set(year, month, dayOfMonth);
                    updateExpiryDateDisplay();
                },
                expiryDate.get(Calendar.YEAR),
                expiryDate.get(Calendar.MONTH),
                expiryDate.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis());
        datePickerDialog.show();
    }

    private void updateExpiryDateDisplay() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        etExpiryDate.setText(sdf.format(expiryDate.getTime()));
    }

    private void submitAnnouncement() {
        String title = etTitle.getText().toString().trim();
        String message = etMessage.getText().toString().trim();

        String priority = "normal";
        if (rbHigh.isChecked()) {
            priority = "urgent";
        } else if (rbLow.isChecked()) {
            priority = "normal";
        } else if (rbNormal.isChecked()) {
            priority = "important";
        }

        // Validation
        if (title.isEmpty()) {
            etTitle.setError("Title is required");
            etTitle.requestFocus();
            return;
        }

        if (message.isEmpty()) {
            etMessage.setError("Message is required");
            etMessage.requestFocus();
            return;
        }

        List<FacultySubject> selectedSubjects = new ArrayList<>();
        for (FacultySubject subject : subjectList) {
            if (subject.isSelected()) {
                selectedSubjects.add(subject);
            }
        }

        if (selectedSubjects.isEmpty()) {
            Toast.makeText(this, "Please select at least one subject", Toast.LENGTH_SHORT).show();
            return;
        }

        btnPost.setEnabled(false);
        btnPost.setText("Posting...");

        FacultySessionManager session = new FacultySessionManager(this);
        String facultyName = session.getName();

        int[] completed = {0};
        int total = selectedSubjects.size();
        boolean[] hasError = {false};

        for (FacultySubject subject : selectedSubjects) {
            String branchSemesterDoc = subject.getBranch() + " " + subject.getSemester();

            Map<String, Object> announcementData = new HashMap<>();
            announcementData.put("title", title);
            announcementData.put("message", message);
            announcementData.put("priority", priority);
            announcementData.put("posted_by", facultyId);
            announcementData.put("posted_by_name", facultyName != null ? facultyName : "Faculty");
            announcementData.put("posted_by_role", "Faculty");
            announcementData.put("subject_name", subject.getSubjectName());
            announcementData.put("subject_code", subject.getSubjectCode());
            announcementData.put("date", new Timestamp(new Date()));
            announcementData.put("expires_at", new Timestamp(expiryDate.getTime()));
            announcementData.put("is_active", true);

            db.collection("Announcements")
                    .document(branchSemesterDoc)
                    .collection("posts")
                    .add(announcementData)
                    .addOnSuccessListener(documentReference -> {
                        completed[0]++;
                        if (completed[0] == total) {
                            runOnUiThread(() -> {
                                if (hasError[0]) {
                                    Toast.makeText(this, "Announcement posted with some errors",
                                            Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(this, "Announcement posted successfully!",
                                            Toast.LENGTH_SHORT).show();
                                }
                                finish();
                            });
                        }
                    })
                    .addOnFailureListener(e -> {
                        hasError[0] = true;
                        completed[0]++;
                        if (completed[0] == total) {
                            runOnUiThread(() -> {
                                Toast.makeText(this, "Error posting announcement: " + e.getMessage(),
                                        Toast.LENGTH_SHORT).show();
                                btnPost.setEnabled(true);
                                btnPost.setText("Post");
                            });
                        }
                    });
        }
    }
}