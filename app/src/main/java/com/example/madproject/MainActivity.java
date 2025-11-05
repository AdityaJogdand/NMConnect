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

        // Check Session before showing UI
        SessionManager session = new SessionManager(this);

        if (session.isLoggedIn()) {
            String role = session.getRole();
            String sapId = session.getSapId();
            String name = session.getName();
            String rollNo = session.getRollNo();
            String branch = session.getBranch();
            String semester = session.getSemester();
            String email = session.getEmail();

            Log.d(TAG, "User already logged in - Role: " + role);

            Intent intent;

            // ✅ FIX: Added faculty redirection
            if ("Faculty".equalsIgnoreCase(role) || "Teacher".equalsIgnoreCase(role)) {
                Log.d(TAG, "Redirecting to FacultyHomePage");
                intent = new Intent(this, FacultyHomePage.class);
            } else if ("CR".equalsIgnoreCase(role)) {
                Log.d(TAG, "Redirecting to CrHomePage");
                intent = new Intent(this, CrHomePage.class);
            } else {
                Log.d(TAG, "Redirecting to StudentHomePage");
                intent = new Intent(this, StudentHomePage.class);
            }

            // Pass session data
            intent.putExtra("sapId", sapId);
            intent.putExtra("role", role);
            intent.putExtra("name", name);
            intent.putExtra("rollNo", rollNo);
            intent.putExtra("branch", branch);
            intent.putExtra("semester", semester);
            intent.putExtra("email", email);

            // Clear back stack and start fresh
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
            return;
        }

        // Not logged in → show main page
        setContentView(R.layout.activity_main);

        Button btn = findViewById(R.id.getstartedbtn);


        btn.setOnClickListener(v -> {
            Intent i = new Intent(MainActivity.this, login.class);
            startActivity(i);
        });


    }
}
