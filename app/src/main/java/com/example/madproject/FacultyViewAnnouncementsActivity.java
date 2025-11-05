package com.example.madproject;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class FacultyViewAnnouncementsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TextView tvEmpty;
    private FacultyAnnouncementAdapter adapter;
    private List<FacultyAnnouncement> announcementList = new ArrayList<>();

    private FirebaseFirestore db;
    private SessionManager session;
    private String facultyId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_faculty_view_announcements);

        db = FirebaseFirestore.getInstance();
        session = new SessionManager(this);
        facultyId = session.getSapId();

        recyclerView = findViewById(R.id.rvFacultyAnnouncements);
        tvEmpty = findViewById(R.id.tvEmptyAnnouncements);
        ImageView ivBack = findViewById(R.id.ivBack);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new FacultyAnnouncementAdapter(announcementList, this::onAnnouncementClick, this::onDeleteClick);
        recyclerView.setAdapter(adapter);

        ivBack.setOnClickListener(v -> finish());

        loadFacultyAnnouncements();
    }

    private void loadFacultyAnnouncements() {
        db.collection("Faculty")
                .document(facultyId)
                .collection("faculty_announcement")
                .orderBy("date", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Toast.makeText(this, "Error loading announcements", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (snapshots != null) {
                        announcementList.clear();
                        for (DocumentSnapshot doc : snapshots) {
                            FacultyAnnouncement announcement = parseFacultyAnnouncement(doc);
                            if (announcement != null) {
                                announcementList.add(announcement);
                            }
                        }
                        updateUI();
                    }
                });
    }

    private FacultyAnnouncement parseFacultyAnnouncement(DocumentSnapshot doc) {
        Map<String, Object> data = doc.getData();
        if (data == null) return null;

        FacultyAnnouncement announcement = new FacultyAnnouncement();
        announcement.setId(doc.getId());

        if (data.containsKey("title")) announcement.setTitle((String) data.get("title"));
        if (data.containsKey("message")) announcement.setMessage((String) data.get("message"));
        if (data.containsKey("priority")) announcement.setPriority((String) data.get("priority"));
        if (data.containsKey("department")) announcement.setDepartment((String) data.get("department"));
        if (data.containsKey("semester")) announcement.setSemester((String) data.get("semester"));
        if (data.containsKey("date")) announcement.setDate((Timestamp) data.get("date"));
        if (data.containsKey("expires_at")) announcement.setExpiresAt((Timestamp) data.get("expires_at"));
        if (data.containsKey("is_active")) {
            Boolean isActive = (Boolean) data.get("is_active");
            announcement.setActive(isActive != null ? isActive : false);
        }
        if (data.containsKey("subjects")) {
            announcement.setSubjects((List<String>) data.get("subjects"));
        }

        return announcement;
    }

    private void updateUI() {
        if (announcementList.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            adapter.notifyDataSetChanged();
        }
    }

    private void onAnnouncementClick(FacultyAnnouncement announcement) {
        // Show full details dialog
        showAnnouncementDetails(announcement);
    }

    private void onDeleteClick(FacultyAnnouncement announcement) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Announcement")
                .setMessage("Are you sure you want to delete this announcement? This will remove it from students' view as well.")
                .setPositiveButton("Delete", (dialog, which) -> deleteAnnouncement(announcement))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteAnnouncement(FacultyAnnouncement announcement) {
        // Delete from faculty subcollection
        db.collection("Faculty")
                .document(facultyId)
                .collection("faculty_announcement")
                .document(announcement.getId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    // Also deactivate in main collection
                    String docPath = announcement.getDepartment() + " " + announcement.getSemester();
                    db.collection("Announcements")
                            .document(docPath)
                            .collection("posts")
                            .document(announcement.getId())
                            .update("is_active", false)
                            .addOnSuccessListener(aVoid2 ->
                                    Toast.makeText(this, "Announcement deleted", Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "Deleted from your records", Toast.LENGTH_SHORT).show());
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to delete: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void showAnnouncementDetails(FacultyAnnouncement announcement) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_announcement_details, null);

        TextView tvTitle = dialogView.findViewById(R.id.tvDialogTitle);
        TextView tvMessage = dialogView.findViewById(R.id.tvDialogMessage);
        TextView tvDepartment = dialogView.findViewById(R.id.tvDialogDepartment);
        TextView tvSemester = dialogView.findViewById(R.id.tvDialogSemester);
        TextView tvPostedDate = dialogView.findViewById(R.id.tvDialogPostedDate);
        TextView tvExpiryDate = dialogView.findViewById(R.id.tvDialogExpiryDate);
        TextView tvSubjects = dialogView.findViewById(R.id.tvDialogSubjects);
        Chip chipPriority = dialogView.findViewById(R.id.chipDialogPriority);
        Chip chipStatus = dialogView.findViewById(R.id.chipDialogStatus);

        tvTitle.setText(announcement.getTitle());
        tvMessage.setText(announcement.getMessage());
        tvDepartment.setText("Department: " + announcement.getDepartment());
        tvSemester.setText("Semester: " + announcement.getSemester());

        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());
        tvPostedDate.setText("Posted: " + sdf.format(announcement.getDate().toDate()));
        tvExpiryDate.setText("Expires: " + sdf.format(announcement.getExpiresAt().toDate()));

        if (announcement.getSubjects() != null && !announcement.getSubjects().isEmpty()) {
            tvSubjects.setText("Subjects: " + String.join(", ", announcement.getSubjects()));
        } else {
            tvSubjects.setVisibility(View.GONE);
        }

        chipPriority.setText(announcement.getPriority());
        setPriorityColor(chipPriority, announcement.getPriority());

        chipStatus.setText(announcement.isActive() ? "Active" : "Expired");
        chipStatus.setChipBackgroundColorResource(announcement.isActive() ?
                android.R.color.holo_green_light : android.R.color.darker_gray);

        new AlertDialog.Builder(this)
                .setView(dialogView)
                .setPositiveButton("Close", null)
                .show();
    }

    private void setPriorityColor(Chip chip, String priority) {
        if ("Urgent".equals(priority)) {
            chip.setChipBackgroundColorResource(android.R.color.holo_red_light);
        } else if ("Important".equals(priority)) {
            chip.setChipBackgroundColorResource(android.R.color.holo_orange_light);
        } else {
            chip.setChipBackgroundColorResource(android.R.color.holo_blue_light);
        }
    }

    // Adapter
    static class FacultyAnnouncementAdapter extends RecyclerView.Adapter<FacultyAnnouncementAdapter.ViewHolder> {
        private final List<FacultyAnnouncement> announcements;
        private final OnAnnouncementClickListener clickListener;
        private final OnDeleteClickListener deleteListener;

        interface OnAnnouncementClickListener {
            void onClick(FacultyAnnouncement announcement);
        }

        interface OnDeleteClickListener {
            void onDelete(FacultyAnnouncement announcement);
        }

        public FacultyAnnouncementAdapter(List<FacultyAnnouncement> announcements,
                                          OnAnnouncementClickListener clickListener,
                                          OnDeleteClickListener deleteListener) {
            this.announcements = announcements;
            this.clickListener = clickListener;
            this.deleteListener = deleteListener;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_faculty_announcement, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            FacultyAnnouncement announcement = announcements.get(position);

            holder.tvTitle.setText(announcement.getTitle());
            holder.tvMessage.setText(announcement.getMessage());
            holder.tvDeptSem.setText(announcement.getDepartment() + " - Sem " + announcement.getSemester());

            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
            holder.tvDate.setText(sdf.format(announcement.getDate().toDate()));

            holder.chipPriority.setText(announcement.getPriority());
            setPriorityColor(holder.chipPriority, announcement.getPriority());

            holder.chipStatus.setText(announcement.isActive() ? "Active" : "Expired");
            holder.chipStatus.setChipBackgroundColorResource(announcement.isActive() ?
                    android.R.color.holo_green_light : android.R.color.darker_gray);

            holder.itemView.setOnClickListener(v -> clickListener.onClick(announcement));
            holder.ivDelete.setOnClickListener(v -> deleteListener.onDelete(announcement));
        }

        @Override
        public int getItemCount() {
            return announcements.size();
        }

        private void setPriorityColor(Chip chip, String priority) {
            if ("Urgent".equals(priority)) {
                chip.setChipBackgroundColorResource(android.R.color.holo_red_light);
            } else if ("Important".equals(priority)) {
                chip.setChipBackgroundColorResource(android.R.color.holo_orange_light);
            } else {
                chip.setChipBackgroundColorResource(android.R.color.holo_blue_light);
            }
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvTitle, tvMessage, tvDeptSem, tvDate;
            Chip chipPriority, chipStatus;
            ImageView ivDelete;

            public ViewHolder(View itemView) {
                super(itemView);
                tvTitle = itemView.findViewById(R.id.tvAnnouncementTitle);
                tvMessage = itemView.findViewById(R.id.tvAnnouncementMessage);
                tvDeptSem = itemView.findViewById(R.id.tvDeptSem);
                tvDate = itemView.findViewById(R.id.tvAnnouncementDate);
                chipPriority = itemView.findViewById(R.id.chipPriority);
                chipStatus = itemView.findViewById(R.id.chipStatus);
                ivDelete = itemView.findViewById(R.id.ivDelete);
            }
        }
    }

    // Model class
    static class FacultyAnnouncement {
        private String id;
        private String title;
        private String message;
        private String priority;
        private String department;
        private String semester;
        private Timestamp date;
        private Timestamp expiresAt;
        private boolean isActive;
        private List<String> subjects;

        // Getters and Setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public String getPriority() { return priority; }
        public void setPriority(String priority) { this.priority = priority; }

        public String getDepartment() { return department; }
        public void setDepartment(String department) { this.department = department; }

        public String getSemester() { return semester; }
        public void setSemester(String semester) { this.semester = semester; }

        public Timestamp getDate() { return date; }
        public void setDate(Timestamp date) { this.date = date; }

        public Timestamp getExpiresAt() { return expiresAt; }
        public void setExpiresAt(Timestamp expiresAt) { this.expiresAt = expiresAt; }

        public boolean isActive() { return isActive; }
        public void setActive(boolean active) { isActive = active; }

        public List<String> getSubjects() { return subjects; }
        public void setSubjects(List<String> subjects) { this.subjects = subjects; }
    }
}