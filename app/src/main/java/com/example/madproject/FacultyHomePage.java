package com.example.madproject;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SnapHelper;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class FacultyHomePage extends AppCompatActivity {

    TextView facultyName, facultyId, department, initials, tvMonthYear, tvNoAnnouncements, tvViewAllAnnouncements;
    View profile;
    RecyclerView calendarRecyclerView, announcementsRecyclerView;
    ExtendedFloatingActionButton fabAddAnnouncement, fabMarkAttendance;

    CalendarAdapter calendarAdapter;
    AnnouncementAdapter announcementAdapter;
    AnnouncementCacheManager cacheManager;

    SessionManager session;
    SharedPreferences prefs;
    FirebaseFirestore db;

    List<Announcement> announcementList = new ArrayList<>();
    ListenerRegistration deptListener, universityListener;

    private final SharedPreferences.OnSharedPreferenceChangeListener prefListener =
            (sharedPreferences, key) -> refreshUI();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_faculty_home_page);

        // Initialize
        db = FirebaseFirestore.getInstance();
        session = new SessionManager(this);
        prefs = getSharedPreferences("app_session", MODE_PRIVATE);
        cacheManager = new AnnouncementCacheManager(this);

        // Bind Views
        facultyName = findViewById(R.id.studentName);
        facultyId = findViewById(R.id.sapid);
        department = findViewById(R.id.branch);
        initials = findViewById(R.id.initials);
        profile = findViewById(R.id.profile);
        calendarRecyclerView = findViewById(R.id.calendarRecyclerView);
        tvMonthYear = findViewById(R.id.tvMonthYear);
        announcementsRecyclerView = findViewById(R.id.announcementsRecyclerView);
        tvNoAnnouncements = findViewById(R.id.tvNoAnnouncements);
        tvViewAllAnnouncements = findViewById(R.id.tvViewAllAnnouncements);
        fabAddAnnouncement = findViewById(R.id.fabAddAnnouncement);
        fabMarkAttendance = findViewById(R.id.fabMarkAttendance);

        if (prefs != null) prefs.registerOnSharedPreferenceChangeListener(prefListener);

        setupCalendar();
        setupAnnouncements();
        refreshUI();

        // For faculty, department is stored under 'branch' key
        String departmentName = session.getBranch();
        String semester = session.getSemester();

        loadAnnouncementsWithCache(departmentName, semester);

        profile.setOnClickListener(v -> startActivity(new Intent(this, Profile.class)));

        // Faculty adds announcements
        fabAddAnnouncement.setOnClickListener(v -> {
            Intent intent = new Intent(this, FacultyAddAnnouncementActivity.class);
            intent.putExtra("department", departmentName);
            intent.putExtra("semester", semester);
            intent.putExtra("sapId", session.getSapId());
            startActivity(intent);
        });

        // Mark Attendance
        fabMarkAttendance.setOnClickListener(v -> {
            Intent intent = new Intent(this, MarkAttendanceActivity.class);
            intent.putExtra("department", departmentName);
            intent.putExtra("semester", semester);
            startActivity(intent);
        });

        // View all announcements
        tvViewAllAnnouncements.setOnClickListener(v -> {
            Intent intent = new Intent(this, FacultyViewAnnouncementsActivity.class);
            startActivity(intent);
        });

        // Handle back press
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                moveTaskToBack(true);
            }
        });
    }

    private void refreshUI() {
        if (session == null) session = new SessionManager(this);
        String id = session.getSapId();
        String name = session.getName();
        String deptName = session.getBranch();

        if (facultyId != null) facultyId.setText(id != null ? id : "N/A");
        if (facultyName != null) facultyName.setText(name != null ? name : "Faculty Member");
        if (department != null) department.setText(deptName != null ? deptName : "N/A");

        if (initials != null) {
            if (name != null && !name.trim().isEmpty()) {
                String[] parts = name.trim().split(" ");
                StringBuilder sb = new StringBuilder();
                for (String p : parts)
                    if (!p.isEmpty()) sb.append(p.charAt(0));
                initials.setText(sb.toString().toUpperCase());
            } else initials.setText("?");
        }
    }

    private void setupCalendar() {
        if (tvMonthYear != null) {
            Calendar today = Calendar.getInstance();
            SimpleDateFormat monthYearFormat = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
            tvMonthYear.setText(monthYearFormat.format(today.getTime()));
        }

        if (calendarRecyclerView != null) {
            List<Calendar> dates = CalendarHelper.generateCalendarDates(21);
            calendarRecyclerView.setLayoutManager(
                    new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
            );
            calendarAdapter = new CalendarAdapter(dates);
            calendarRecyclerView.setAdapter(calendarAdapter);
            calendarRecyclerView.scrollToPosition(10);
        }
    }

    private void setupAnnouncements() {
        if (announcementsRecyclerView == null) return;

        announcementsRecyclerView.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        );
        announcementAdapter = new AnnouncementAdapter(announcementList);
        announcementsRecyclerView.setAdapter(announcementAdapter);
        announcementsRecyclerView.setHasFixedSize(true);

        SnapHelper snapHelper = new PagerSnapHelper();
        snapHelper.attachToRecyclerView(announcementsRecyclerView);
    }

    private void loadAnnouncementsWithCache(String deptName, String semester) {
        if (deptName == null || semester == null) return;

        if (cacheManager.hasCachedData(deptName, semester)) {
            List<Announcement> cached = cacheManager.getCachedAnnouncements(deptName, semester);
            announcementList.clear();
            announcementList.addAll(cached);
            updateAnnouncementUI();
        }

        setupAnnouncementListeners(deptName, semester);
    }

    private void setupAnnouncementListeners(String deptName, String semester) {
        String deptSemDoc = deptName + " " + semester;

        if (deptListener != null) deptListener.remove();
        if (universityListener != null) universityListener.remove();

        deptListener = db.collection("Announcements")
                .document(deptSemDoc)
                .collection("posts")
                .whereEqualTo("is_active", true)
                .orderBy("date", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Toast.makeText(this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (snapshots == null) return;

                    announcementList.clear();
                    for (DocumentSnapshot doc : snapshots) {
                        Announcement announcement = parseAnnouncement(doc);
                        if (announcement != null) announcementList.add(announcement);
                    }

                    loadUniversityAnnouncementsRealtime(deptName, semester);
                });
    }

    private void loadUniversityAnnouncementsRealtime(String deptName, String semester) {
        universityListener = db.collection("Announcements")
                .document("University")
                .collection("posts")
                .whereEqualTo("is_active", true)
                .orderBy("date", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) return;

                    if (snapshots != null) {
                        for (DocumentSnapshot doc : snapshots) {
                            Announcement announcement = parseAnnouncement(doc);
                            if (announcement != null) announcementList.add(announcement);
                        }
                    }

                    cacheManager.saveAnnouncements(announcementList, deptName, semester);
                    updateAnnouncementUI();
                });
    }

    private Announcement parseAnnouncement(DocumentSnapshot doc) {
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

        Timestamp expiresAt = a.getExpires_at();
        if (expiresAt != null && expiresAt.toDate().before(new java.util.Date())) {
            return null;
        }

        return a;
    }

    private void updateAnnouncementUI() {
        runOnUiThread(() -> {
            if (tvNoAnnouncements == null || announcementsRecyclerView == null || announcementAdapter == null) return;

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
    protected void onResume() {
        super.onResume();
        refreshUI();
        loadAnnouncementsWithCache(session.getBranch(), session.getSemester());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (prefs != null) prefs.unregisterOnSharedPreferenceChangeListener(prefListener);
        if (deptListener != null) deptListener.remove();
        if (universityListener != null) universityListener.remove();
    }
}
