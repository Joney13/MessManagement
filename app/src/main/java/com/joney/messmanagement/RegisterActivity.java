package com.joney.messmanagement;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class RegisterActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // ভেরিয়েবলগুলো লোকালভাবে ডিক্লেয়ার করা হলো
        TextInputEditText editTextMessName = findViewById(R.id.editTextMessName);
        TextInputEditText editTextManagerName = findViewById(R.id.editTextManagerName);
        TextInputEditText editTextEmail = findViewById(R.id.editTextEmail);
        TextInputEditText editTextPassword = findViewById(R.id.editTextPassword);
        Button registerButton = findViewById(R.id.registerButton);

        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        registerButton.setOnClickListener(v -> {
            // NullPointerException এড়ানোর জন্য Objects.requireNonNull ব্যবহার
            String messName = Objects.requireNonNull(editTextMessName.getText()).toString().trim();
            String managerName = Objects.requireNonNull(editTextManagerName.getText()).toString().trim();
            String email = Objects.requireNonNull(editTextEmail.getText()).toString().trim();
            String password = Objects.requireNonNull(editTextPassword.getText()).toString().trim();

            if (messName.isEmpty() || managerName.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            FirebaseUser firebaseUser = mAuth.getCurrentUser();
                            // NullPointerException এড়ানোর জন্য null চেক
                            if (firebaseUser != null) {
                                String managerUid = firebaseUser.getUid();
                                createMessAndManager(db, messName, managerName, email, managerUid);
                            } else {
                                Toast.makeText(RegisterActivity.this, "Registration failed: Could not get user.", Toast.LENGTH_LONG).show();
                            }
                        } else {
                            // NullPointerException এড়ানোর জন্য null চেক
                            String errorMessage = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                            Toast.makeText(RegisterActivity.this, "Registration Failed: " + errorMessage, Toast.LENGTH_LONG).show();
                        }
                    });
        });
    }

    private void createMessAndManager(FirebaseFirestore db, String messName, String managerName, String email, String managerUid) {
        DocumentReference messRef = db.collection("messes").document();
        String messId = messRef.getId();

        Map<String, Object> messData = new HashMap<>();
        messData.put("messName", messName);
        messData.put("managerUid", managerUid);

        messRef.set(messData).addOnSuccessListener(aVoid -> {
            Map<String, Object> managerData = new HashMap<>();
            managerData.put("name", managerName);
            managerData.put("email", email);
            managerData.put("role", "admin");
            managerData.put("messId", messId);

            db.collection("users").document(managerUid).set(managerData)
                    .addOnSuccessListener(aVoid1 -> {
                        Toast.makeText(RegisterActivity.this, "Mess registered successfully", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(RegisterActivity.this, AdminDashboardActivity.class);
                        // নতুন টাস্ক শুরু করে আগের সব টাস্ক ক্লিয়ার করা
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    });
        }).addOnFailureListener(e -> {
            Toast.makeText(RegisterActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }
}