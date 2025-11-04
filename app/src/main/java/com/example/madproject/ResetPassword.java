package com.example.madproject;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class ResetPassword extends AppCompatActivity {

    private static final String TAG = "ResetPassword";
    private EditText newPasswordInput, confirmPasswordInput;
    private Button resetButton;
    private FirebaseFirestore db;
    private String sapId, collectionName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Initialize views
        newPasswordInput = findViewById(R.id.newpassword);
        confirmPasswordInput = findViewById(R.id.confirmpassword);
        resetButton = findViewById(R.id.reset);

        // Get extras from intent
        sapId = getIntent().getStringExtra("sapId");
        collectionName = getIntent().getStringExtra("collection");

        Log.d(TAG, "Reset password for SAP ID: " + sapId + " in collection: " + collectionName);

        resetButton.setOnClickListener(v -> resetPassword());
    }

    private void resetPassword() {
        String newPassword = newPasswordInput.getText().toString().trim();
        String confirmPassword = confirmPasswordInput.getText().toString().trim();

        if (newPassword.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (newPassword.length() < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }

        String hashedPassword = hashPassword(newPassword);

        if (hashedPassword == null) {
            Toast.makeText(this, "Error hashing password", Toast.LENGTH_SHORT).show();
            return;
        }

        // Use the sapId as the document ID directly
        Log.d(TAG, "Updating password for document: " + sapId);

        db.collection(collectionName)
                .document(sapId)
                .update("password", hashedPassword, "first_login", false)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Password reset successful for " + sapId);
                    Toast.makeText(this, "Password reset successful!", Toast.LENGTH_LONG).show();

                    // Redirect to login page
                    Intent intent = new Intent(this, login.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to update password", e);
                    Toast.makeText(this, "Failed to update password: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
}