package com.example.madproject;

import android.content.Intent;
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
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class StudentHomePage extends AppCompatActivity {

    TextView studentName, sapid, rollno, branch, initials, tvMonthYear, tvNoAnnouncements, tvNoMarks;
    View profile;
    RecyclerView calendarRecyclerView, announcementsRecyclerView, internalMarksRecyclerView;
    CalendarAdapter calendarAdapter;
    AnnouncementAdapter announcementAdapter;
    InternalMarkAdapter internalMarkAdapter;

    private FirebaseFirestore db;
    private List<Announcement> announcementList;
    private Map<String, InternalMark> marksMap;
    private AnnouncementCacheManager cacheManager;
    private MarksCacheManager marksCacheManager;
    private ListenerRegistration branchListener;
    private ListenerRegistration universityListener;
    private ListenerRegistration marksListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_studenthomepage);

        db = FirebaseFirestore.getInstance();
        announcementList = new ArrayList<>();
        marksMap = new HashMap<>();
        cacheManager = new AnnouncementCacheManager(this);
        marksCacheManager = MarksCacheManager.getInstance(this);

        studentName = findViewById(R.id.studentName);
        sapid = findViewById(R.id.sapid);
        rollno = findViewById(R.id.rollno);
        branch = findViewById(R.id.branch);
        initials = findViewById(R.id.initials);
        profile = findViewById(R.id.profile);
        calendarRecyclerView = findViewById(R.id.calendarRecyclerView);
        tvMonthYear = findViewById(R.id.tvMonthYear);
        announcementsRecyclerView = findViewById(R.id.announcementsRecyclerView);
        tvNoAnnouncements = findViewById(R.id.tvNoAnnouncements);
        internalMarksRecyclerView = findViewById(R.id.internalMarksRecyclerView);
        tvNoMarks = findViewById(R.id.tvNoMarks);

        SessionManager session = new SessionManager(this);
        String sapId = session.getSapId();
        String name = session.getName();
        String rollNo = session.getRollNo();
        String branchName = session.getBranch();
        String semester = session.getSemester();

        sapid.setText(sapId != null ? sapId : "N/A");
        studentName.setText(name != null ? name : "Student");
        rollno.setText(rollNo != null ? rollNo : "N/A");
        branch.setText(branchName != null ? branchName : "N/A");

        if (name != null && !name.trim().isEmpty()) {
            String[] parts = name.split(" ");
            StringBuilder sb = new StringBuilder();
            for (String p : parts) if (!p.isEmpty()) sb.append(p.charAt(0));
            initials.setText(sb.toString().toUpperCase());
        } else initials.setText("?");

        setupCalendar();
        setupAnnouncements();
        setupInternalMarks();
        loadAnnouncementsWithCache(branchName, semester);
        loadInternalMarks(sapId, branchName, semester);

        profile.setOnClickListener(v -> startActivity(new Intent(this, Profile.class)));

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                moveTaskToBack(true);
            }
        });
    }

    private void setupCalendar() {
        Calendar today = Calendar.getInstance();
        SimpleDateFormat monthYearFormat = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        tvMonthYear.setText(monthYearFormat.format(today.getTime()));

        List<Calendar> dates = CalendarHelper.generateCalendarDates(21);
        calendarRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
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

    private void setupInternalMarks() {
        internalMarksRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        internalMarkAdapter = new InternalMarkAdapter(new ArrayList<>());
        internalMarksRecyclerView.setAdapter(internalMarkAdapter);
    }

    private void loadInternalMarks(String sapId, String branchName, String semester) {
        if (sapId == null) return;

        List<InternalMark> cachedMarks = marksCacheManager.getCachedMarksList(sapId);
        if (cachedMarks != null && !cachedMarks.isEmpty()) {
            marksMap.clear();
            for (InternalMark mark : cachedMarks) {
                marksMap.put(mark.getId(), mark);
            }
            updateMarksUI();
        }

        marksListener = db.collection("Student")
                .document(sapId)
                .collection("Internal Component Assessment ")
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Toast.makeText(this, "Error loading marks", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (snapshots == null) return;

                    for (DocumentChange change : snapshots.getDocumentChanges()) {
                        DocumentSnapshot doc = change.getDocument();
                        InternalMark mark = doc.toObject(InternalMark.class);

                        if (mark == null) continue;

                        mark.setId(doc.getId());
                        if (mark.getSubject_name() == null || mark.getSubject_name().isEmpty()) {
                            mark.setSubject_name(doc.getId());
                        }

                        switch (change.getType()) {
                            case ADDED:
                            case MODIFIED:
                                marksMap.put(mark.getId(), mark);
                                break;
                            case REMOVED:
                                marksMap.remove(mark.getId());
                                break;
                        }
                    }

                    List<InternalMark> sortedMarks = new ArrayList<>(marksMap.values());
                    sortedMarks.sort((a, b) -> a.getSubject_name().compareToIgnoreCase(b.getSubject_name()));

                    marksCacheManager.saveMarksList(sortedMarks, sapId);
                    updateMarksUI();
                });
    }

    private void updateMarksUI() {
        runOnUiThread(() -> {
            List<InternalMark> marksList = new ArrayList<>(marksMap.values());
            marksList.sort((a, b) -> a.getSubject_name().compareToIgnoreCase(b.getSubject_name()));

            if (marksList.isEmpty()) {
                tvNoMarks.setVisibility(View.VISIBLE);
                internalMarksRecyclerView.setVisibility(View.GONE);
            } else {
                tvNoMarks.setVisibility(View.GONE);
                internalMarksRecyclerView.setVisibility(View.VISIBLE);
                internalMarkAdapter.updateData(marksList);
            }
        });
    }

    private void loadAnnouncementsWithCache(String branchName, String semester) {
        if (branchName == null || semester == null) return;

        if (cacheManager.hasCachedData(branchName, semester)) {
            List<Announcement> cached = cacheManager.getCachedAnnouncements(branchName, semester);
            announcementList.clear();
            announcementList.addAll(cached);
            updateAnnouncementUI();
        }

        setupAnnouncementListeners(branchName, semester);
    }

    private void setupAnnouncementListeners(String branchName, String semester) {
        String branchSemesterDoc = branchName + " " + semester;

        branchListener = db.collection("Announcements")
                .document(branchSemesterDoc)
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
                        Announcement announcement = parseAnnouncementFromDynamicFields(doc);
                        if (announcement != null) announcementList.add(announcement);
                    }

                    loadUniversityAnnouncementsRealtime(branchName, semester);
                });
    }

    private void loadUniversityAnnouncementsRealtime(String branchName, String semester) {
        universityListener = db.collection("Announcements")
                .document("University")
                .collection("posts")
                .whereEqualTo("is_active", true)
                .orderBy("date", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) return;

                    if (snapshots == null) {
                        cacheManager.saveAnnouncements(announcementList, branchName, semester);
                        updateAnnouncementUI();
                        return;
                    }

                    for (DocumentSnapshot doc : snapshots) {
                        Announcement announcement = parseAnnouncementFromDynamicFields(doc);
                        if (announcement != null) announcementList.add(announcement);
                    }

                    cacheManager.saveAnnouncements(announcementList, branchName, semester);
                    updateAnnouncementUI();
                });
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

        Timestamp expiresAt = a.getExpires_at();
        if (expiresAt != null && expiresAt.toDate().before(new java.util.Date())) {
            return null;
        }

        return a;
    }

    private void updateAnnouncementUI() {
        if (announcementList.isEmpty()) {
            tvNoAnnouncements.setVisibility(View.VISIBLE);
            announcementsRecyclerView.setVisibility(View.GONE);
        } else {
            tvNoAnnouncements.setVisibility(View.GONE);
            announcementsRecyclerView.setVisibility(View.VISIBLE);
            announcementAdapter.updateList(new ArrayList<>(announcementList));
            announcementsRecyclerView.post(announcementsRecyclerView::requestLayout);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (branchListener != null) branchListener.remove();
        if (universityListener != null) universityListener.remove();
        if (marksListener != null) marksListener.remove();
    }
}