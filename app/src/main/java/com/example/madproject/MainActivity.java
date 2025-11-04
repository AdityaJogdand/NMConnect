package com.example.madproject;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        // Session check - BEFORE setContentView
        SessionManager session = new SessionManager(this);
        if (session.isLoggedIn()) {
            // Get all session data
            String role = session.getRole();
            String sapId = session.getSapId();
            String name = session.getName();
            String rollNo = session.getRollNo();
            String branch = session.getBranch();
            String semester = session.getSemester();
            String email = session.getEmail();

            Log.d(TAG, "User already logged in - Role: " + role);

            Intent i;
            // Check role and redirect accordingly
            if ("CR".equalsIgnoreCase(role)) {
                Log.d(TAG, "Redirecting to CrHomePage");
                i = new Intent(this, CrHomePage.class);
            } else {
                Log.d(TAG, "Redirecting to StudentHomePage");
                i = new Intent(this, StudentHomePage.class);
            }

            // Pass all user data
            i.putExtra("sapId", sapId);
            i.putExtra("role", role);
            i.putExtra("name", name);
            i.putExtra("rollNo", rollNo);
            i.putExtra("branch", branch);
            i.putExtra("semester", semester);
            i.putExtra("email", email);

            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
            finish();
            return;
        }

        // User not logged in, show main screen
        setContentView(R.layout.activity_main);

        Button btn = findViewById(R.id.getstartedbtn);
        TextView lg = findViewById(R.id.login);

        btn.setOnClickListener(v -> {
            Intent i = new Intent(MainActivity.this, login.class);
            startActivity(i);
        });
    }
}