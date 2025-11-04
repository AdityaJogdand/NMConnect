package com.example.madproject;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class AddAnnouncementActivity extends AppCompatActivity {

    private EditText etTitle, etMessage, etExpiryDate;
    private RadioGroup rgPriority;
    private Button btnPost, btnCancel;
    private FirebaseFirestore db;

    private String branch, semester, sapId;
    private Calendar selectedExpiryDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_announcement);

        db = FirebaseFirestore.getInstance();

        // Get data from intent
        branch = getIntent().getStringExtra("branch");
        semester = getIntent().getStringExtra("semester");
        sapId = getIntent().getStringExtra("sapId");

        // Initialize views
        etTitle = findViewById(R.id.etTitle);
        etMessage = findViewById(R.id.etMessage);
        etExpiryDate = findViewById(R.id.etExpiryDate);
        rgPriority = findViewById(R.id.rgPriority);
        btnPost = findViewById(R.id.btnPost);
        btnCancel = findViewById(R.id.btnCancel);

        // Set default expiry date (7 days from now)
        selectedExpiryDate = Calendar.getInstance();
        selectedExpiryDate.add(Calendar.DAY_OF_MONTH, 7);
        updateExpiryDateDisplay();

        // Expiry date picker
        etExpiryDate.setOnClickListener(v -> showDatePicker());

        // Post button
        btnPost.setOnClickListener(v -> postAnnouncement());

        // Cancel button
        btnCancel.setOnClickListener(v -> finish());
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    selectedExpiryDate.set(year, month, dayOfMonth);
                    updateExpiryDateDisplay();
                },
                selectedExpiryDate.get(Calendar.YEAR),
                selectedExpiryDate.get(Calendar.MONTH),
                selectedExpiryDate.get(Calendar.DAY_OF_MONTH)
        );

        // Set minimum date to today
        datePickerDialog.getDatePicker().setMinDate(calendar.getTimeInMillis());
        datePickerDialog.show();
    }

    private void updateExpiryDateDisplay() {
        String dateStr = String.format("%02d/%02d/%d",
                selectedExpiryDate.get(Calendar.DAY_OF_MONTH),
                selectedExpiryDate.get(Calendar.MONTH) + 1,
                selectedExpiryDate.get(Calendar.YEAR)
        );
        etExpiryDate.setText(dateStr);
    }

    private void postAnnouncement() {
        String title = etTitle.getText().toString().trim();
        String message = etMessage.getText().toString().trim();

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

        // Get priority
        String priority = "Normal";
        int selectedId = rgPriority.getCheckedRadioButtonId();
        if (selectedId == R.id.rbHigh) {
            priority = "High";
        } else if (selectedId == R.id.rbLow) {
            priority = "Low";
        }

        // Get CR name from session
        SessionManager session = new SessionManager(this);
        String crName = session.getName();

        // Create announcement data
        Map<String, Object> announcement = new HashMap<>();
        announcement.put("title", title);
        announcement.put("message", message);
        announcement.put("posted_by", sapId);
        announcement.put("posted_by_name", crName != null ? crName : "CR");
        announcement.put("posted_by_role", "CR");
        announcement.put("priority", priority);
        announcement.put("date", Timestamp.now());
        announcement.put("expires_at", new Timestamp(selectedExpiryDate.getTime()));
        announcement.put("is_active", true);

        // Post to Firestore
        String branchSemesterDoc = branch + " " + semester;

        btnPost.setEnabled(false);
        btnPost.setText("Posting...");

        db.collection("Announcements")
                .document(branchSemesterDoc)
                .collection("posts")
                .add(announcement)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Announcement posted successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to post announcement: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    btnPost.setEnabled(true);
                    btnPost.setText("Post Announcement");
                });
    }
}