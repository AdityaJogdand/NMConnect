package com.example.madproject;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class FacultyHomePage extends AppCompatActivity {

    private static final String TAG = "FacultyHomePage";

    TextView facultyName, facultyId, department, initials;
    View profile;
    CardView addAnnouncementCard, manageSubjectsCard;
    RecyclerView subjectsRecyclerView;
    TextView tvNoSubjects;

    private FirebaseFirestore db;
    private List<FacultySubject> subjectsList;
    private FacultySubjectAdapter subjectAdapter;
    private String sapId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_faculty_home_page);

        db = FirebaseFirestore.getInstance();
        subjectsList = new ArrayList<>();

        initializeViews();
        loadSessionData();
        setupSubjectsRecyclerView();
        loadFacultySubjects();

        profile.setOnClickListener(v -> startActivity(new Intent(this, Profile.class)));

        // Add announcement button
        addAnnouncementCard.setOnClickListener(v -> {
            Intent intent = new Intent(this, FacultyAddAnnouncementActivity.class);
            intent.putExtra("sapId", sapId);
            startActivity(intent);
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                moveTaskToBack(true);
            }
        });
    }

    private void initializeViews() {
        facultyName = findViewById(R.id.facultyName);
        facultyId = findViewById(R.id.facultyId);
        department = findViewById(R.id.department);
        initials = findViewById(R.id.initials);
        profile = findViewById(R.id.profile);
        addAnnouncementCard = findViewById(R.id.addAnnouncementCard);
        subjectsRecyclerView = findViewById(R.id.subjectsRecyclerView);
        tvNoSubjects = findViewById(R.id.tvNoSubjects);
    }

    private void loadSessionData() {
        SessionManager session = new SessionManager(this);
        sapId = session.getSapId();
        String name = session.getName();
        String dept = session.getBranch(); // Using branch field for department

        facultyId.setText(sapId != null ? sapId : "N/A");
        facultyName.setText(name != null ? name : "Faculty");
        department.setText(dept != null ? dept : "N/A");

        if (name != null && !name.trim().isEmpty()) {
            String[] parts = name.split(" ");
            StringBuilder sb = new StringBuilder();
            for (String p : parts) {
                if (!p.isEmpty()) sb.append(p.charAt(0));
            }
            initials.setText(sb.toString().toUpperCase());
        } else {
            initials.setText("F");
        }
    }

    private void setupSubjectsRecyclerView() {
        subjectsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        subjectAdapter = new FacultySubjectAdapter(subjectsList, subject -> {
            // Open subject details with options: Attendance, Marks
            Intent intent = new Intent(this, SubjectDetailsActivity.class);
            intent.putExtra("sapId", sapId);
            intent.putExtra("subjectCode", subject.getSubjectCode());
            intent.putExtra("subjectName", subject.getSubjectName());
            intent.putExtra("branch", subject.getBranch());
            intent.putExtra("semester", subject.getSemester());
            startActivity(intent);
        });
        subjectsRecyclerView.setAdapter(subjectAdapter);
    }

    private void loadFacultySubjects() {
        if (sapId == null) {
            Toast.makeText(this, "Faculty ID not found", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "Loading subjects for faculty: " + sapId);

        db.collection("Faculty")
                .document(sapId)
                .collection("Subjects")
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error loading subjects", error);
                        Toast.makeText(this, "Error loading subjects", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (snapshots == null || snapshots.isEmpty()) {
                        Log.d(TAG, "No subjects found");
                        updateSubjectsUI();
                        return;
                    }

                    subjectsList.clear();
                    for (DocumentSnapshot doc : snapshots) {
                        FacultySubject subject = new FacultySubject();
                        subject.setSubjectCode(doc.getId());
                        subject.setSubjectName(doc.getString("subject_name"));
                        subject.setBranch(doc.getString("branch"));
                        subject.setSemester(doc.getString("semester"));
                        subject.setIsActive(doc.getBoolean("is_active"));

                        Log.d(TAG, "Loaded subject: " + subject.getSubjectName() +
                                " - " + subject.getBranch() + " " + subject.getSemester());

                        subjectsList.add(subject);
                    }

                    updateSubjectsUI();
                });
    }

    private void updateSubjectsUI() {
        runOnUiThread(() -> {
            if (subjectsList.isEmpty()) {
                tvNoSubjects.setVisibility(View.VISIBLE);
                subjectsRecyclerView.setVisibility(View.GONE);
            } else {
                tvNoSubjects.setVisibility(View.GONE);
                subjectsRecyclerView.setVisibility(View.VISIBLE);
                subjectAdapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh data when returning to this activity
        loadFacultySubjects();
    }
}