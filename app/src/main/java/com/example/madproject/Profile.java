package com.example.madproject;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class Profile extends AppCompatActivity {

    TextView profileName, profileSapId, profileRollNo, profileBranch, profileRole, profileInitials, profileEmail, profileSemester;
    Button profileLogoutBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Initialize views
        profileName = findViewById(R.id.profileName);
        profileSapId = findViewById(R.id.profileSapId);
        profileRollNo = findViewById(R.id.profileRollNo);
        profileBranch = findViewById(R.id.profileBranch);
        profileRole = findViewById(R.id.profileRole);
        profileInitials = findViewById(R.id.profileInitials);
        profileEmail = findViewById(R.id.profileEmail);
        profileSemester = findViewById(R.id.profileSemester);
        profileLogoutBtn = findViewById(R.id.profileLogoutBtn);

        // Get data from session
        SessionManager session = new SessionManager(this);
        String name = session.getName();
        String sapId = session.getSapId();
        String rollNo = session.getRollNo();
        String branch = session.getBranch();
        String role = session.getRole();
        String email = session.getEmail();
        String semester = session.getSemester();

        // Set values
        profileName.setText(name != null ? name : "N/A");
        profileSapId.setText(sapId != null ? sapId : "N/A");
        profileRollNo.setText(rollNo != null ? rollNo : "N/A");
        profileBranch.setText(branch != null ? branch : "N/A");
        profileRole.setText(role != null ? role : "N/A");
        profileEmail.setText(email != null ? email : "N/A");
        profileSemester.setText(semester != null ? semester : "N/A");

        // Generate and set initials
        if (name != null && !name.isEmpty()) {
            String[] parts = name.split(" ");
            StringBuilder initials = new StringBuilder();
            for (String part : parts) {
                if (!part.isEmpty()) {
                    initials.append(part.charAt(0));
                }
            }
            profileInitials.setText(initials.toString().toUpperCase());
        } else {
            profileInitials.setText("?");
        }

        // Logout button
        profileLogoutBtn.setOnClickListener(v -> {
            session.clearSession();
            Intent intent = new Intent(Profile.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });
    }
}