package com.example.madproject;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SnapHelper;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class FacultyHomePage extends AppCompatActivity {

    TextView facultyName, facultyId, department, initials, tvMonthYear, tvNoAnnouncements, facultyBadge;
    View profile;
    CardView addAnnouncementCard;
    RecyclerView calendarRecyclerView, announcementsRecyclerView;
    CalendarAdapter calendarAdapter;
    AnnouncementAdapter announcementAdapter;

    private FirebaseFirestore db;
    private List<Announcement> announcementList;
    private AnnouncementCacheManager cacheManager;
    private List<ListenerRegistration> announcementListeners;
    private Set<String> uniqueAnnouncementIds;
    private String facultyIdValue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_faculty_home_page);

        db = FirebaseFirestore.getInstance();
        announcementList = new ArrayList<>();
        announcementListeners = new ArrayList<>();
        uniqueAnnouncementIds = new HashSet<>();
        cacheManager = new AnnouncementCacheManager(this);

        initializeViews();
        loadSessionData();
        setupCalendar();
        setupAnnouncements();

        FacultySessionManager session = new FacultySessionManager(this);
        facultyIdValue = session.getFacultyId();

        loadFacultyAnnouncements();

        profile.setOnClickListener(v -> startActivity(new Intent(this, FacultyProfile.class)));

        // Add announcement button
        addAnnouncementCard.setOnClickListener(v -> {
            Intent intent = new Intent(this, FacultyAddAnnouncementActivity.class);
            intent.putExtra("facultyId", facultyIdValue);
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
        facultyBadge = findViewById(R.id.facultyBadge);
        profile = findViewById(R.id.profile);
        addAnnouncementCard = findViewById(R.id.addAnnouncementCard);
        calendarRecyclerView = findViewById(R.id.calendarRecyclerView);
        tvMonthYear = findViewById(R.id.tvMonthYear);
        announcementsRecyclerView = findViewById(R.id.announcementsRecyclerView);
        tvNoAnnouncements = findViewById(R.id.tvNoAnnouncements);
    }

    private void loadSessionData() {
        FacultySessionManager session = new FacultySessionManager(this);
        String fId = session.getFacultyId();
        String name = session.getName();
        String dept = session.getDepartment();

        facultyId.setText(fId != null ? fId : "N/A");
        facultyName.setText(name != null ? name : "Faculty");
        department.setText(dept != null ? dept : "N/A");

        // Show Faculty badge
        if (facultyBadge != null) {
            facultyBadge.setVisibility(View.VISIBLE);
            facultyBadge.setText("Faculty");
        }

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

    private void setupCalendar() {
        Calendar today = Calendar.getInstance();
        SimpleDateFormat monthYearFormat = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        tvMonthYear.setText(monthYearFormat.format(today.getTime()));

        List<Calendar> dates = CalendarHelper.generateCalendarDates(21);
        calendarRecyclerView.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        );
        calendarAdapter = new CalendarAdapter(dates);
        calendarRecyclerView.setAdapter(calendarAdapter);
        calendarRecyclerView.scrollToPosition(10);
    }

    private void setupAnnouncements() {
        announcementsRecyclerView.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        );

        announcementAdapter = new AnnouncementAdapter(announcementList);
        announcementsRecyclerView.setAdapter(announcementAdapter);
        announcementsRecyclerView.setHasFixedSize(true);

        SnapHelper snapHelper = new PagerSnapHelper();
        snapHelper.attachToRecyclerView(announcementsRecyclerView);
    }

    private void loadFacultyAnnouncements() {
        if (facultyIdValue == null) return;

        // Load cached announcements first
        List<Announcement> cached = cacheManager.getCachedAnnouncements("faculty", facultyIdValue);
        if (cached != null && !cached.isEmpty()) {
            announcementList.clear();
            uniqueAnnouncementIds.clear();
            for (Announcement a : cached) {
                if (uniqueAnnouncementIds.add(a.getId())) {
                    announcementList.add(a);
                }
            }
            updateAnnouncementUI();
        }

        // Load faculty's own subjects and their announcements
        db.collection("Faculty")
                .document(facultyIdValue)
                .collection("Subjects")
                .whereEqualTo("is_active", true)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (DocumentSnapshot doc : querySnapshot) {
                        String branch = doc.getString("branch");
                        String semester = doc.getString("semester");

                        if (branch != null && semester != null) {
                            setupAnnouncementListener(branch, semester);
                        }
                    }

                    // Also load university announcements
                    loadUniversityAnnouncements();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading subjects", Toast.LENGTH_SHORT).show();
                });
    }

    private void setupAnnouncementListener(String branch, String semester) {
        String branchSemesterDoc = branch + " " + semester;

        ListenerRegistration listener = db.collection("Announcements")
                .document(branchSemesterDoc)
                .collection("posts")
                .whereEqualTo("is_active", true)
                .orderBy("date", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        return;
                    }

                    if (snapshots == null) return;

                    for (DocumentChange change : snapshots.getDocumentChanges()) {
                        DocumentSnapshot doc = change.getDocument();
                        Announcement announcement = parseAnnouncementFromDynamicFields(doc);

                        if (announcement == null) continue;

                        switch (change.getType()) {
                            case ADDED:
                            case MODIFIED:
                                if (uniqueAnnouncementIds.add(announcement.getId())) {
                                    announcementList.add(0, announcement);
                                } else {
                                    // Update existing announcement
                                    for (int i = 0; i < announcementList.size(); i++) {
                                        if (announcementList.get(i).getId().equals(announcement.getId())) {
                                            announcementList.set(i, announcement);
                                            break;
                                        }
                                    }
                                }
                                break;
                            case REMOVED:
                                announcementList.removeIf(a -> a.getId().equals(announcement.getId()));
                                uniqueAnnouncementIds.remove(announcement.getId());
                                break;
                        }
                    }

                    // Sort by date
                    announcementList.sort((a, b) -> {
                        if (a.getDate() == null) return 1;
                        if (b.getDate() == null) return -1;
                        return b.getDate().compareTo(a.getDate());
                    });

                    cacheManager.saveAnnouncements(announcementList, "faculty", facultyIdValue);
                    updateAnnouncementUI();
                });

        announcementListeners.add(listener);
    }

    private void loadUniversityAnnouncements() {
        ListenerRegistration listener = db.collection("Announcements")
                .document("University")
                .collection("posts")
                .whereEqualTo("is_active", true)
                .orderBy("date", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) return;

                    if (snapshots == null) return;

                    for (DocumentChange change : snapshots.getDocumentChanges()) {
                        DocumentSnapshot doc = change.getDocument();
                        Announcement announcement = parseAnnouncementFromDynamicFields(doc);

                        if (announcement == null) continue;

                        switch (change.getType()) {
                            case ADDED:
                            case MODIFIED:
                                if (uniqueAnnouncementIds.add(announcement.getId())) {
                                    announcementList.add(0, announcement);
                                } else {
                                    // Update existing announcement
                                    for (int i = 0; i < announcementList.size(); i++) {
                                        if (announcementList.get(i).getId().equals(announcement.getId())) {
                                            announcementList.set(i, announcement);
                                            break;
                                        }
                                    }
                                }
                                break;
                            case REMOVED:
                                announcementList.removeIf(a -> a.getId().equals(announcement.getId()));
                                uniqueAnnouncementIds.remove(announcement.getId());
                                break;
                        }
                    }

                    // Sort by date
                    announcementList.sort((a, b) -> {
                        if (a.getDate() == null) return 1;
                        if (b.getDate() == null) return -1;
                        return b.getDate().compareTo(a.getDate());
                    });

                    cacheManager.saveAnnouncements(announcementList, "faculty", facultyIdValue);
                    updateAnnouncementUI();
                });

        announcementListeners.add(listener);
    }

    private Announcement parseAnnouncementFromDynamicFields(DocumentSnapshot doc) {
        Map<String, Object> data = doc.getData();
        if (data == null || data.isEmpty()) return null;

        Announcement a = new Announcement();
        a.setId(doc.getId());

        if (data.containsKey("title")) a.setTitle((String) data.get("title"));
        if (data.containsKey("message")) a.setMessage((String) data.get("message"));
        if (data.containsKey("posted_by")) a.setPosted_by((String) data.get("posted_by"));
        if (data.containsKey("posted_by_name")) a.setPosted_by_name((String) data.get("posted_by_name"));
        if (data.containsKey("posted_by_role")) a.setPosted_by_role((String) data.get("posted_by_role"));
        if (data.containsKey("priority")) a.setPriority((String) data.get("priority"));
        if (data.containsKey("date")) a.setDate((Timestamp) data.get("date"));
        if (data.containsKey("expires_at")) a.setExpires_at((Timestamp) data.get("expires_at"));
        if (data.containsKey("is_active")) {
            Boolean isActive = (Boolean) data.get("is_active");
            a.setIs_active(isActive != null ? isActive : false);
        }

        // Check if announcement has expired
        Timestamp expiresAt = a.getExpires_at();
        if (expiresAt != null && expiresAt.toDate().before(new java.util.Date())) {
            return null;
        }

        return a;
    }

    private void updateAnnouncementUI() {
        runOnUiThread(() -> {
            if (announcementList.isEmpty()) {
                tvNoAnnouncements.setVisibility(View.VISIBLE);
                announcementsRecyclerView.setVisibility(View.GONE);
            } else {
                tvNoAnnouncements.setVisibility(View.GONE);
                announcementsRecyclerView.setVisibility(View.VISIBLE);
                announcementAdapter.updateList(new ArrayList<>(announcementList));
                announcementsRecyclerView.post(announcementsRecyclerView::requestLayout);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        for (ListenerRegistration listener : announcementListeners) {
            if (listener != null) listener.remove();
        }
        announcementListeners.clear();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh announcements when coming back from FacultyAddAnnouncementActivity
        announcementList.clear();
        uniqueAnnouncementIds.clear();
        for (ListenerRegistration listener : announcementListeners) {
            if (listener != null) listener.remove();
        }
        announcementListeners.clear();
        loadFacultyAnnouncements();
    }
}