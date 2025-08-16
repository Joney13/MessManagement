package com.joney.messmanagement;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class AddNoticeActivity extends AppCompatActivity {

    private EditText etNotice;
    private Button btnPostNotice;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String currentMessId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_notice);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        etNotice = findViewById(R.id.etNotice);
        btnPostNotice = findViewById(R.id.btnPostNotice);

        // Fetch the current messId
        fetchMessId();

        btnPostNotice.setOnClickListener(v -> postNotice());
    }

    private void fetchMessId() {
        String adminUid = Objects.requireNonNull(mAuth.getCurrentUser()).getUid();
        db.collection("users").document(adminUid).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                currentMessId = documentSnapshot.getString("messId");
            } else {
                Toast.makeText(this, "Error: Admin details not found.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void postNotice() {
        String noticeText = etNotice.getText().toString().trim();

        if (noticeText.isEmpty()) {
            etNotice.setError("Notice cannot be empty");
            return;
        }
        if (currentMessId == null) {
            Toast.makeText(this, "Error: Cannot post notice. Mess ID is missing.", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> notice = new HashMap<>();
        notice.put("messId", currentMessId);
        notice.put("noticeText", noticeText);
        notice.put("timestamp", Timestamp.now());

        db.collection("notices").add(notice)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Notice posted successfully!", Toast.LENGTH_SHORT).show();
                    finish(); // Go back to the dashboard
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to post notice: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}