package com.example.madproject;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class MarkAttendanceActivity extends AppCompatActivity {

    private static final String TAG = "MarkAttendance";

    private RecyclerView recyclerView;
    private LinearLayout tvEmpty;
    private TextView tvSelectedCount;
    private ProgressBar progressBar;
    private MaterialButton btnMarkAttendance;
    private ImageView ivBack;

    private StudentAttendanceAdapter adapter;
    private List<Student> studentList = new ArrayList<>();
    private List<Student> selectedStudents = new ArrayList<>();

    private FirebaseFirestore db;
    private SessionManager session;
    private String subjectName;
    private String branch;
    private String semester;
    private String facultyId;
    private String department;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mark_attendance);

        db = FirebaseFirestore.getInstance();
        session = new SessionManager(this);

        progressBar = findViewById(R.id.progressBar);
        recyclerView = findViewById(R.id.rvStudents);
        tvEmpty = findViewById(R.id.tvEmptyStudents);
        tvSelectedCount = findViewById(R.id.tvSelectedCount);
        btnMarkAttendance = findViewById(R.id.btnMarkAttendance);
        ivBack = findViewById(R.id.ivBack);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new StudentAttendanceAdapter(studentList, this::onStudentSelectionChanged);
        recyclerView.setAdapter(adapter);

        ivBack.setOnClickListener(v -> finish());
        btnMarkAttendance.setOnClickListener(v -> showConfirmationDialog());

        facultyId = session.getSapId();
        subjectName = getIntent().getStringExtra("subjectName");

        Log.d(TAG, "Faculty ID: " + facultyId);
        Log.d(TAG, "Subject Name: " + subjectName);

        if (facultyId == null || facultyId.isEmpty()) {
            Toast.makeText(this, "Faculty ID not found. Please login again.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        fetchFacultyDepartment();
    }

    private void fetchFacultyDepartment() {
        progressBar.setVisibility(View.VISIBLE);
        db.collection("Faculty")
                .document(facultyId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        department = documentSnapshot.getString("department");
                        Log.d(TAG, "Department found: " + department);

                        if (department == null || department.isEmpty()) {
                            Toast.makeText(this, "Faculty department missing in database", Toast.LENGTH_LONG).show();
                            finish();
                            return;
                        }

                        if (subjectName == null) {
                            showSubjectSelectionDialog();
                        } else {
                            fetchSubjectDetails();
                        }
                    } else {
                        Toast.makeText(this, "Faculty record not found", Toast.LENGTH_LONG).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error fetching faculty department: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    finish();
                });
    }

    private void showSubjectSelectionDialog() {
        progressBar.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        tvEmpty.setVisibility(View.GONE);

        db.collection("Faculty")
                .document(facultyId)
                .collection("Subjects")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    progressBar.setVisibility(View.GONE);
                    if (querySnapshot.isEmpty()) {
                        Toast.makeText(this, "No subjects assigned to you", Toast.LENGTH_LONG).show();
                        finish();
                        return;
                    }

                    List<SubjectInfo> subjects = new ArrayList<>();
                    List<String> subjectDisplayNames = new ArrayList<>();

                    for (DocumentSnapshot doc : querySnapshot) {
                        String name = doc.getId();
                        String branchVal = doc.getString("branch");
                        String semesterVal = doc.getString("semester");

                        if (name != null && branchVal != null && semesterVal != null) {
                            subjects.add(new SubjectInfo(name, branchVal, semesterVal));
                            subjectDisplayNames.add(name + " (" + branchVal + " - Sem " + semesterVal + ")");
                        }
                    }

                    if (subjects.isEmpty()) {
                        Toast.makeText(this, "No valid subjects found.", Toast.LENGTH_LONG).show();
                        finish();
                        return;
                    }

                    new AlertDialog.Builder(this)
                            .setTitle("Select Subject for Attendance")
                            .setItems(subjectDisplayNames.toArray(new String[0]), (dialog, which) -> {
                                SubjectInfo selected = subjects.get(which);
                                subjectName = selected.name;
                                branch = selected.branch;
                                semester = selected.semester;
                                loadStudents();
                            })
                            .setCancelable(false)
                            .setNegativeButton("Cancel", (dialog, which) -> finish())
                            .show();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Failed to load subjects: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    finish();
                });
    }

    private void fetchSubjectDetails() {
        progressBar.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        tvEmpty.setVisibility(View.GONE);

        db.collection("Faculty")
                .document(facultyId)
                .collection("Subjects")
                .document(subjectName)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    progressBar.setVisibility(View.GONE);

                    if (documentSnapshot.exists()) {
                        branch = documentSnapshot.getString("branch");
                        semester = documentSnapshot.getString("semester");

                        if (branch != null && semester != null) {
                            loadStudents();
                        } else {
                            Toast.makeText(this, "Incomplete subject details", Toast.LENGTH_LONG).show();
                            finish();
                        }
                    } else {
                        Toast.makeText(this, "Subject not found", Toast.LENGTH_LONG).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Error fetching subject: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    finish();
                });
    }

    private String getStudentPath() {
        return "Subjects/" + department + "/" + subjectName + "/" + branch + " " + semester + "/Students";
    }

    private void loadStudents() {
        progressBar.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        tvEmpty.setVisibility(View.GONE);

        String path = getStudentPath();
        Log.d(TAG, "Loading students from path: " + path);

        db.collection(path)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    progressBar.setVisibility(View.GONE);
                    if (queryDocumentSnapshots.isEmpty()) {
                        tvEmpty.setVisibility(View.VISIBLE);
                        recyclerView.setVisibility(View.GONE);
                        return;
                    }

                    studentList.clear();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Student student = parseStudent(doc);
                        // ✅ Only add students who haven't completed all lectures
                        if (student != null && !student.hasCompletedAllLectures()) {
                            studentList.add(student);
                        }
                    }

                    if (studentList.isEmpty()) {
                        tvEmpty.setVisibility(View.VISIBLE);
                        recyclerView.setVisibility(View.GONE);
                        Toast.makeText(this, "All students have completed their attendance", Toast.LENGTH_SHORT).show();
                    } else {
                        tvEmpty.setVisibility(View.GONE);
                        recyclerView.setVisibility(View.VISIBLE);
                        adapter.notifyDataSetChanged();
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    tvEmpty.setVisibility(View.VISIBLE);
                    Toast.makeText(this, "Error loading students: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private Student parseStudent(DocumentSnapshot doc) {
        try {
            Student student = new Student();
            student.setId(doc.getId());
            student.setName(doc.getString("Name") != null ? doc.getString("Name") : "Unknown");
            student.setRollNo(doc.getString("Roll_no") != null ? doc.getString("Roll_no") : "N/A");

            String attendedStr = doc.getString("Attended_Lectures");
            String totalStr = doc.getString("Total_Lectures");

            int attended = 0;
            int total = 0;

            try {
                if (attendedStr != null) attended = Integer.parseInt(attendedStr);
                if (totalStr != null) total = Integer.parseInt(totalStr);
            } catch (NumberFormatException ignored) {}

            student.setAttendedLectures(attended);
            student.setTotalLectures(total);

            return student;
        } catch (Exception e) {
            Log.e(TAG, "Error parsing student: " + doc.getId(), e);
            return null;
        }
    }

    private void onStudentSelectionChanged(Student student, boolean isSelected) {
        if (isSelected) {
            if (!selectedStudents.contains(student)) selectedStudents.add(student);
        } else {
            selectedStudents.remove(student);
        }
        updateSelectedCount();
    }

    private void updateSelectedCount() {
        int count = selectedStudents.size();
        if (count == 0) {
            tvSelectedCount.setText("Select students to mark attendance");
            btnMarkAttendance.setEnabled(false);
        } else {
            tvSelectedCount.setText(count + " student" + (count > 1 ? "s" : "") + " selected");
            btnMarkAttendance.setEnabled(true);
        }
    }

    private void showConfirmationDialog() {
        int count = selectedStudents.size();
        new AlertDialog.Builder(this)
                .setTitle("Mark Attendance")
                .setMessage("Mark attendance for " + count + " selected student" + (count > 1 ? "s" : "") + "?")
                .setPositiveButton("Confirm", (dialog, which) -> markAttendance())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void markAttendance() {
        progressBar.setVisibility(View.VISIBLE);
        btnMarkAttendance.setEnabled(false);

        String path = getStudentPath();
        Log.d(TAG, "Marking attendance at path: " + path);

        int totalStudents = selectedStudents.size();
        final int[] successCount = {0};
        final int[] failCount = {0};

        for (Student student : selectedStudents) {
            // ✅ Only increment Attended_Lectures, NOT Total_Lectures
            String attendedField = String.valueOf(student.getAttendedLectures() + 1);

            db.collection(path)
                    .document(student.getId())
                    .update("Attended_Lectures", attendedField)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "✅ Attendance updated for: " + student.getId());
                        successCount[0]++;
                        if (successCount[0] + failCount[0] == totalStudents)
                            onAttendanceMarkingComplete(successCount[0], failCount[0]);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "❌ Failed to update attendance for: " + student.getId(), e);
                        failCount[0]++;
                        if (successCount[0] + failCount[0] == totalStudents)
                            onAttendanceMarkingComplete(successCount[0], failCount[0]);
                    });
        }
    }

    private void onAttendanceMarkingComplete(int successCount, int failCount) {
        progressBar.setVisibility(View.GONE);

        String message;
        if (failCount == 0)
            message = "Attendance marked for " + successCount + " student" + (successCount > 1 ? "s" : "");
        else
            message = "Attendance marked for " + successCount + " student(s). " + failCount + " failed.";

        Toast.makeText(this, message, Toast.LENGTH_LONG).show();

        selectedStudents.clear();
        adapter.clearSelection();
        updateSelectedCount();
        loadStudents();
    }

    private static class SubjectInfo {
        String name, branch, semester;
        SubjectInfo(String name, String branch, String semester) {
            this.name = name;
            this.branch = branch;
            this.semester = semester;
        }
    }

    static class Student {
        private String id, name, rollNo;
        private int attendedLectures, totalLectures;
        private boolean isSelected;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getRollNo() { return rollNo; }
        public void setRollNo(String rollNo) { this.rollNo = rollNo; }
        public int getAttendedLectures() { return attendedLectures; }
        public void setAttendedLectures(int attendedLectures) { this.attendedLectures = attendedLectures; }
        public int getTotalLectures() { return totalLectures; }
        public void setTotalLectures(int totalLectures) { this.totalLectures = totalLectures; }
        public boolean isSelected() { return isSelected; }
        public void setSelected(boolean selected) { isSelected = selected; }

        // ✅ Check if student has completed all lectures
        public boolean hasCompletedAllLectures() {
            return totalLectures > 0 && attendedLectures >= totalLectures;
        }

        public double getAttendancePercentage() {
            if (totalLectures == 0) return 0;
            return (attendedLectures * 100.0) / totalLectures;
        }
    }

    static class StudentAttendanceAdapter extends RecyclerView.Adapter<StudentAttendanceAdapter.ViewHolder> {
        private final List<Student> students;
        private final OnStudentSelectionListener listener;

        interface OnStudentSelectionListener {
            void onSelectionChanged(Student student, boolean isSelected);
        }

        public StudentAttendanceAdapter(List<Student> students, OnStudentSelectionListener listener) {
            this.students = students;
            this.listener = listener;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_student_attendance, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            Student student = students.get(position);

            holder.tvName.setText(student.getName());
            holder.tvRollNo.setText("Roll No: " + student.getRollNo());
            holder.tvAttendance.setText(student.getAttendedLectures() + "/" + student.getTotalLectures());
            double percentage = student.getAttendancePercentage();
            holder.tvPercentage.setText(String.format("%.1f%%", percentage));

            if (percentage >= 75) holder.tvPercentage.setTextColor(0xFF4CAF50);
            else if (percentage >= 50) holder.tvPercentage.setTextColor(0xFFFFA726);
            else holder.tvPercentage.setTextColor(0xFFEF5350);

            holder.checkbox.setOnCheckedChangeListener(null);
            holder.checkbox.setChecked(student.isSelected());
            holder.checkbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                student.setSelected(isChecked);
                if (listener != null) listener.onSelectionChanged(student, isChecked);
            });

            holder.itemView.setOnClickListener(v -> holder.checkbox.toggle());
        }

        @Override
        public int getItemCount() {
            return students.size();
        }

        public void clearSelection() {
            for (Student student : students) student.setSelected(false);
            notifyDataSetChanged();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvName, tvRollNo, tvAttendance, tvPercentage;
            CheckBox checkbox;
            ViewHolder(View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tvStudentName);
                tvRollNo = itemView.findViewById(R.id.tvRollNo);
                tvAttendance = itemView.findViewById(R.id.tvAttendance);
                tvPercentage = itemView.findViewById(R.id.tvPercentage);
                checkbox = itemView.findViewById(R.id.checkboxStudent);
            }
        }
    }
}