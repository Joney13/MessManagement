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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.Objects;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText editTextLoginInput, editTextPassword;
    private Button loginButton;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        editTextLoginInput = findViewById(R.id.editTextLoginInput);
        editTextPassword = findViewById(R.id.editTextPassword);
        loginButton = findViewById(R.id.loginButton);

        loginButton.setOnClickListener(v -> {
            String input = Objects.requireNonNull(editTextLoginInput.getText()).toString().trim();
            String password = Objects.requireNonNull(editTextPassword.getText()).toString().trim();

            if (input.isEmpty() || password.isEmpty()) {
                Toast.makeText(LoginActivity.this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            loginUser(input, password);
        });
    }

    private void loginUser(String input, String password) {
        mAuth.signInWithEmailAndPassword(input, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseMessaging.getInstance().getToken().addOnSuccessListener(MyFirebaseMessagingService::sendTokenToFirestore);
                        Toast.makeText(LoginActivity.this, "Admin login successful", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(LoginActivity.this, AdminDashboardActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    } else {
                        loginAsMember(input, password);
                    }
                });
    }

    private void loginAsMember(String userId, String password) {
        db.collection("users")
                .whereEqualTo("userID", userId) // Fixed to userID
                .whereEqualTo("password", password)
                .whereEqualTo("role", "member")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null && !task.getResult().isEmpty()) {
                        Toast.makeText(LoginActivity.this, "Member login successful", Toast.LENGTH_SHORT).show();

                        String memberDocId = task.getResult().getDocuments().get(0).getId();

                        Intent intent = new Intent(LoginActivity.this, MemberDashboardActivity.class);
                        intent.putExtra("MEMBER_DOC_ID", memberDocId);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();

                    } else {
                        Toast.makeText(LoginActivity.this, "Invalid credentials.", Toast.LENGTH_LONG).show();
                    }
                });
    }
}