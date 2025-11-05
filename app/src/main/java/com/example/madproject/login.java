package com.example.madproject;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.card.MaterialCardView;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class login extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    private MaterialCardView cardStudent, cardCR, cardTeacher;
    private Button loginBtn;
    private EditText emailInput, passwordInput;
    private String selectedRole = null;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        db = FirebaseFirestore.getInstance();

        // Initialize views
        cardStudent = findViewById(R.id.cardStudent);
        cardCR = findViewById(R.id.cardCR);
        cardTeacher = findViewById(R.id.cardTeacher);
        loginBtn = findViewById(R.id.signupBtn);
        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);

        // Select role
        cardStudent.setOnClickListener(v -> selectRole("Student", cardStudent));
        cardCR.setOnClickListener(v -> selectRole("CR", cardCR));
        cardTeacher.setOnClickListener(v -> selectRole("Teacher", cardTeacher));

        // Login click
        loginBtn.setOnClickListener(v -> {
            String sapId = emailInput.getText().toString().trim();
            String password = passwordInput.getText().toString().trim();

            if (selectedRole == null) {
                Toast.makeText(this, "Please select a role to continue", Toast.LENGTH_SHORT).show();
                return;
            }

            if (sapId.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            Log.d(TAG, "Login attempt - SAP ID: " + sapId + ", Selected Role: " + selectedRole);
            loginUser(sapId, password);
        });
    }

    private void loginUser(String sapId, String password) {
        String hashedPassword = hashPassword(password);

        // Determine collection based on selected role
        String collectionName = selectedRole.equals("Teacher") ? "Faculty" : "Student";

        Log.d(TAG, "Checking collection: " + collectionName);

        db.collection(collectionName)
                .document(sapId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult().exists()) {
                        DocumentSnapshot doc = task.getResult();

                        String storedPassword = doc.getString("password");
                        Boolean firstLogin = doc.getBoolean("first_login");

                        // Get user data common fields
                        String name = doc.getString("Name");
                        String email = doc.getString("email");
                        String userRole = doc.getString("role"); // Get actual role from database

                        // Fields that differ between Student and Faculty
                        String rollNo;
                        String branchOrDepartment;
                        String semester;

                        // === FIX: CONDITIONAL DATA RETRIEVAL ===
                        if (collectionName.equals("Faculty")) {
                            // Retrieve department for Faculty
                            branchOrDepartment = doc.getString("department");
                            rollNo = null; // Roll No is typically not applicable or unused for Faculty
                            semester = null; // Semester is typically not applicable for Faculty
                        } else {
                            // Retrieve Branch, Roll No, and Semester for Student/CR
                            rollNo = doc.getString("Roll No");
                            branchOrDepartment = doc.getString("Branch");
                            semester = doc.getString("Semester");
                        }
                        // ======================================

                        // Log all fetched data
                        Log.d(TAG, "Document found for SAP ID: " + sapId);
                        Log.d(TAG, "Name: " + name);
                        Log.d(TAG, "Roll No: " + rollNo);
                        Log.d(TAG, "Branch/Department: " + branchOrDepartment); // Log corrected variable
                        Log.d(TAG, "Semester: " + semester);
                        Log.d(TAG, "Email: " + email);
                        Log.d(TAG, "User Role from DB: " + userRole);
                        Log.d(TAG, "Selected Role: " + selectedRole);
                        Log.d(TAG, "First Login: " + firstLogin);

                        if (firstLogin != null && firstLogin) {
                            // First-time login check (plain password)
                            if (storedPassword.equals(password)) {
                                Log.d(TAG, "First login - password correct, updating...");
                                db.collection(collectionName)
                                        .document(sapId)
                                        .update("password", hashedPassword, "first_login", false);

                                navigateToReset(sapId, collectionName);
                            } else {
                                Log.e(TAG, "First login - incorrect password");
                                Toast.makeText(this, "Incorrect password", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            // Regular login check (hashed password)
                            if (storedPassword != null && storedPassword.equals(hashedPassword)) {
                                Log.d(TAG, "Password verified successfully");

                                // Validate role selection for students only
                                if (!collectionName.equals("Faculty")) {
                                    // For students, check if role matches
                                    if ("CR".equalsIgnoreCase(userRole) && !selectedRole.equals("CR")) {
                                        Log.w(TAG, "User is CR but selected Student card");
                                        Toast.makeText(this, "You are a CR. Please select the CR card.", Toast.LENGTH_SHORT).show();
                                        return;
                                    } else if (!"CR".equalsIgnoreCase(userRole) && selectedRole.equals("CR")) {
                                        Log.w(TAG, "User is not CR but selected CR card");
                                        Toast.makeText(this, "You are not a CR. Please select the Student card.", Toast.LENGTH_SHORT).show();
                                        return;
                                    }
                                }

                                Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show();

                                // Save complete user session
                                SessionManager session = new SessionManager(this);
                                String sessionRole = collectionName.equals("Faculty") ? "Faculty" : (userRole != null ? userRole : "Student");

                                // Pass the corrected branch/department variable to the session
                                session.createLoginSession(sapId, sessionRole, rollNo, branchOrDepartment, name, email, semester);

                                // Redirect based on collection and role
                                Intent intent;
                                try {
                                    if (collectionName.equals("Faculty")) {
                                        // Faculty login - redirect to FacultyHomePage
                                        Log.d(TAG, "Redirecting to FacultyHomePage");
                                        intent = new Intent(this, FacultyHomePage.class);
                                    } else if ("CR".equalsIgnoreCase(userRole) && "CR".equals(selectedRole)) {
                                        // User is CR and selected CR card - go to CR HomePage
                                        Log.d(TAG, "Redirecting to CrHomePage");
                                        intent = new Intent(this, CrHomePage.class);
                                    } else {
                                        // Regular student - go to Student HomePage
                                        Log.d(TAG, "Redirecting to StudentHomePage");
                                        intent = new Intent(this, StudentHomePage.class);
                                    }

                                    // Pass all user data
                                    intent.putExtra("sapId", sapId);
                                    intent.putExtra("role", sessionRole);
                                    intent.putExtra("name", name);
                                    intent.putExtra("rollNo", rollNo);
                                    intent.putExtra("branch", branchOrDepartment); // Use corrected variable
                                    intent.putExtra("semester", semester);
                                    intent.putExtra("email", email);

                                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                                    startActivity(intent);
                                    finishAffinity();
                                } catch (Exception e) {
                                    Log.e(TAG, "Error starting activity", e);
                                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                }
                            } else {
                                Log.e(TAG, "Invalid credentials - password mismatch");
                                Toast.makeText(this, "Invalid credentials", Toast.LENGTH_SHORT).show();
                            }
                        }
                    } else {
                        Log.e(TAG, "User not found in " + collectionName + " collection");
                        Toast.makeText(this, "User not found in " + collectionName, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            Log.e(TAG, "Error hashing password", e);
            e.printStackTrace();
            return null;
        }
    }

    private void navigateToReset(String sapId, String collectionName) {
        Log.d(TAG, "Navigating to ResetPassword screen");
        Toast.makeText(this, "First-time login. Please reset your password.", Toast.LENGTH_LONG).show();
        Intent intent = new Intent(this, ResetPassword.class);
        intent.putExtra("sapId", sapId);
        intent.putExtra("collection", collectionName);
        startActivity(intent);
    }

    private void selectRole(String role, MaterialCardView selectedCard) {
        selectedRole = role;
        Log.d(TAG, "Role selected: " + role);
        MaterialCardView[] cards = {cardStudent, cardCR, cardTeacher};

        for (MaterialCardView card : cards) {
            if (card == selectedCard) {
                card.setStrokeColor(Color.parseColor("#D73A31"));
                card.setStrokeWidth(5);
            } else {
                card.setStrokeWidth(0);
            }
        }
    }
}