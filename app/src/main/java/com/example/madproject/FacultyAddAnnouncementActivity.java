package com.example.madproject;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;

import java.text.SimpleDateFormat;
import java.util.*;

public class FacultyAddAnnouncementActivity extends AppCompatActivity {

    private EditText etTitle, etMessage, etExpiryDate;
    private Spinner spinnerDepartment, spinnerSemester;
    private RadioGroup rgPriority;
    private Button btnPost, btnCancel;
    private RecyclerView subjectsRecyclerView;

    private FirebaseFirestore db;
    private SessionManager session;
    private String facultyId, facultyName;
    private List<String> facultySubjects = new ArrayList<>();
    private SubjectAdapter subjectAdapter;
    private Calendar selectedDate = Calendar.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_faculty_add_announcement2);

        db = FirebaseFirestore.getInstance();
        session = new SessionManager(this);

        facultyId = session.getSapId();
        facultyName = session.getName();

        etTitle = findViewById(R.id.etTitle);
        etMessage = findViewById(R.id.etMessage);
        etExpiryDate = findViewById(R.id.etExpiryDate);
        spinnerDepartment = findViewById(R.id.spinnerDepartment);
        spinnerSemester = findViewById(R.id.spinnerSemester);
        rgPriority = findViewById(R.id.rgPriority);
        btnPost = findViewById(R.id.btnPost);
        btnCancel = findViewById(R.id.btnCancel);
        subjectsRecyclerView = findViewById(R.id.subjectsRecyclerView);

        setupDepartmentSpinner();
        setupSemesterSpinner();
        setupDatePicker();
        setupSubjectsRecycler();
        setupButtons();

        loadFacultySubjects();
    }

    private void setupDepartmentSpinner() {
        String[] departments = {"Btech(AIDS)", "Btech(CS)", "Btech(IT)", "MBA", "MCA"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, departments);
        spinnerDepartment.setAdapter(adapter);
    }

    private void setupSemesterSpinner() {
        String[] semesters = {"I", "II", "III", "IV", "V", "VI", "VII", "VIII"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, semesters);
        spinnerSemester.setAdapter(adapter);
    }

    private void setupDatePicker() {
        etExpiryDate.setOnClickListener(v -> {
            int year = selectedDate.get(Calendar.YEAR);
            int month = selectedDate.get(Calendar.MONTH);
            int day = selectedDate.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePicker = new DatePickerDialog(
                    FacultyAddAnnouncementActivity.this,
                    (view, year1, month1, dayOfMonth) -> {
                        selectedDate.set(year1, month1, dayOfMonth);
                        selectedDate.set(Calendar.HOUR_OF_DAY, 23);
                        selectedDate.set(Calendar.MINUTE, 59);
                        selectedDate.set(Calendar.SECOND, 59);
                        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
                        etExpiryDate.setText(sdf.format(selectedDate.getTime()));
                    },
                    year, month, day
            );
            datePicker.getDatePicker().setMinDate(System.currentTimeMillis());
            datePicker.show();
        });
    }

    private void setupSubjectsRecycler() {
        subjectsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        subjectAdapter = new SubjectAdapter(facultySubjects);
        subjectsRecyclerView.setAdapter(subjectAdapter);
    }

    private void setupButtons() {
        btnCancel.setOnClickListener(v -> finish());

        btnPost.setOnClickListener(v -> {
            String title = etTitle.getText().toString().trim();
            String message = etMessage.getText().toString().trim();
            String department = spinnerDepartment.getSelectedItem().toString();
            String semester = spinnerSemester.getSelectedItem().toString();
            String expiryDate = etExpiryDate.getText().toString().trim();
            List<String> selectedSubjects = subjectAdapter.getSelectedSubjects();

            if (title.isEmpty()) {
                Toast.makeText(this, "Please enter a title", Toast.LENGTH_SHORT).show();
                return;
            }
            if (message.isEmpty()) {
                Toast.makeText(this, "Please enter a message", Toast.LENGTH_SHORT).show();
                return;
            }
            if (expiryDate.isEmpty()) {
                Toast.makeText(this, "Please select expiry date", Toast.LENGTH_SHORT).show();
                return;
            }
            if (selectedSubjects.isEmpty()) {
                Toast.makeText(this, "Please select at least one subject", Toast.LENGTH_SHORT).show();
                return;
            }

            // NEW VALIDATION: Check if faculty teaches the selected department and semester
            if (!facultyTeachesSelectedBranchSem(department, semester, selectedSubjects)) {
                Toast.makeText(this, "You don't teach " + department + " Semester " + semester + ". Please select the correct department and semester.", Toast.LENGTH_LONG).show();
                return;
            }

            String priority = getPriorityFromRadioGroup();
            postAnnouncement(title, message, department, semester, expiryDate, priority, selectedSubjects);
        });
    }

    private String getPriorityFromRadioGroup() {
        int selectedId = rgPriority.getCheckedRadioButtonId();
        if (selectedId == R.id.rbHigh) return "Urgent";
        else if (selectedId == R.id.rbNormal) return "Important";
        else return "Normal";
    }

    private void loadFacultySubjects() {
        db.collection("Faculty")
                .document(facultyId)
                .collection("Subjects")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    facultySubjects.clear();
                    for (DocumentSnapshot doc : querySnapshot) {
                        String subjectName = doc.getId();
                        String branch = doc.getString("branch");
                        String semester = doc.getString("semester");
                        if (branch != null && semester != null) {
                            facultySubjects.add(subjectName + " (" + branch + " - Sem " + semester + ")");
                        }
                    }
                    subjectAdapter.notifyDataSetChanged();

                    if (facultySubjects.isEmpty()) {
                        Toast.makeText(this, "No subjects assigned to you", Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load subjects: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // NEW METHOD: Validate if faculty teaches the selected branch-semester combination
    private boolean facultyTeachesSelectedBranchSem(String selectedDepartment, String selectedSemester, List<String> selectedSubjects) {
        // Check if any of the selected subjects match the department and semester
        for (String subject : selectedSubjects) {
            // Extract branch and semester from subject string format: "SubjectName (Branch - Sem X)"
            if (subject.contains(selectedDepartment) && subject.contains("Sem " + selectedSemester)) {
                return true;
            }
        }
        return false;
    }

    private void postAnnouncement(String title, String message, String department,
                                  String semester, String expiryDate, String priority,
                                  List<String> subjects) {

        btnPost.setEnabled(false);
        btnPost.setText("Posting...");

        // Create the document path for main announcements
        String documentPath = department + " " + semester;
        Timestamp currentTime = Timestamp.now();
        Timestamp expiryTimestamp = new Timestamp(selectedDate.getTime());

        // Create announcement data
        Map<String, Object> announcement = new HashMap<>();
        announcement.put("title", title);
        announcement.put("message", message);
        announcement.put("priority", priority);
        announcement.put("posted_by", facultyId);
        announcement.put("posted_by_name", facultyName != null ? facultyName : "Faculty");
        announcement.put("posted_by_role", "Faculty");
        announcement.put("date", currentTime);
        announcement.put("expires_at", expiryTimestamp);
        announcement.put("is_active", true);
        announcement.put("subjects", subjects);
        announcement.put("department", department);
        announcement.put("semester", semester);

        // Post to main Announcements collection
        db.collection("Announcements")
                .document(documentPath)
                .collection("posts")
                .add(announcement)
                .addOnSuccessListener(documentReference -> {
                    String announcementId = documentReference.getId();
                    announcement.put("announcement_id", announcementId);

                    // Also save to Faculty's subcollection
                    saveFacultyAnnouncement(announcement, announcementId);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to post: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    btnPost.setEnabled(true);
                    btnPost.setText("Post");
                });
    }

    private void saveFacultyAnnouncement(Map<String, Object> announcement, String announcementId) {
        // Save to Faculty's subcollection: /Faculty/{facultyId}/faculty_announcement/{announcementId}
        db.collection("Faculty")
                .document(facultyId)
                .collection("faculty_announcement")
                .document(announcementId)
                .set(announcement)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Announcement posted successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Posted but failed to save to faculty records", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    // RecyclerView Adapter for Subjects
    static class SubjectAdapter extends RecyclerView.Adapter<SubjectAdapter.SubjectViewHolder> {
        private final List<String> subjects;
        private final Set<String> selectedSubjects = new HashSet<>();

        public SubjectAdapter(List<String> subjects) {
            this.subjects = subjects;
        }

        @Override
        public SubjectViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_subject_checkbox, parent, false);
            return new SubjectViewHolder(view);
        }

        @Override
        public void onBindViewHolder(SubjectViewHolder holder, int position) {
            String subject = subjects.get(position);
            holder.checkBox.setText(subject);
            holder.checkBox.setOnCheckedChangeListener(null);
            holder.checkBox.setChecked(selectedSubjects.contains(subject));
            holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) selectedSubjects.add(subject);
                else selectedSubjects.remove(subject);
            });
        }

        @Override
        public int getItemCount() {
            return subjects.size();
        }

        public List<String> getSelectedSubjects() {
            return new ArrayList<>(selectedSubjects);
        }

        static class SubjectViewHolder extends RecyclerView.ViewHolder {
            CheckBox checkBox;
            public SubjectViewHolder(View itemView) {
                super(itemView);
                checkBox = itemView.findViewById(R.id.checkboxSubject);
            }
        }
    }
}