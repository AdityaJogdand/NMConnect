package com.example.madproject;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

public class FacultyProfile extends AppCompatActivity {

    TextView tvFacultyId, tvName, tvEmail, tvPhone, tvDepartment, initials;
    Button btnLogout, btnEditProfile;
    View profileCircle;

    private FirebaseFirestore db;
    private FacultySessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_faculty_profile);

        db = FirebaseFirestore.getInstance();
        sessionManager = new FacultySessionManager(this);

        initializeViews();
        loadProfileData();
        setupClickListeners();
    }

    private void initializeViews() {
        tvFacultyId = findViewById(R.id.tvFacultyId);
        tvName = findViewById(R.id.tvName);
        tvEmail = findViewById(R.id.tvEmail);
        tvPhone = findViewById(R.id.tvPhone);
        tvDepartment = findViewById(R.id.tvDepartment);
        initials = findViewById(R.id.initials);
        profileCircle = findViewById(R.id.profileCircle);
        btnLogout = findViewById(R.id.btnLogout);
        btnEditProfile = findViewById(R.id.btnEditProfile);
    }

    private void loadProfileData() {
        String facultyId = sessionManager.getFacultyId();
        String name = sessionManager.getName();
        String email = sessionManager.getEmail();
        String phone = sessionManager.getPhone();
        String department = sessionManager.getDepartment();

        tvFacultyId.setText(facultyId != null ? facultyId : "N/A");
        tvName.setText(name != null ? name : "Faculty Member");
        tvEmail.setText(email != null ? email : "Not provided");
        tvPhone.setText(phone != null ? phone : "Not provided");
        tvDepartment.setText(department != null ? department : "Not provided");

        // Set initials
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

    private void setupClickListeners() {
        btnLogout.setOnClickListener(v -> showLogoutDialog());

        btnEditProfile.setOnClickListener(v -> {
            Toast.makeText(this, "Edit profile feature coming soon", Toast.LENGTH_SHORT).show();
        });

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Yes", (dialog, which) -> performLogout())
                .setNegativeButton("No", null)
                .show();
    }

    private void performLogout() {
        sessionManager.logout();

        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadProfileData();
    }
}