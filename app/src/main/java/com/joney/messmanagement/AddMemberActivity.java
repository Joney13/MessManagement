package com.joney.messmanagement;

import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class AddMemberActivity extends AppCompatActivity {

    private TextInputEditText etMemberName, etMemberMobile, etMemberEmail, etMemberDescription;
    private Button btnSaveMember;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_member);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        etMemberName = findViewById(R.id.etMemberName);
        etMemberMobile = findViewById(R.id.etMemberMobile);
        etMemberEmail = findViewById(R.id.etMemberEmail);
        etMemberDescription = findViewById(R.id.etMemberDescription);
        btnSaveMember = findViewById(R.id.btnSaveMember);

        btnSaveMember.setOnClickListener(v -> {
            saveMemberData();
        });
    }

    private void saveMemberData() {
        String name = Objects.requireNonNull(etMemberName.getText()).toString().trim();
        String mobile = Objects.requireNonNull(etMemberMobile.getText()).toString().trim();
        String email = Objects.requireNonNull(etMemberEmail.getText()).toString().trim();
        String description = Objects.requireNonNull(etMemberDescription.getText()).toString().trim();

        if (name.isEmpty() || mobile.isEmpty()) {
            Toast.makeText(this, "Name and Mobile number are required", Toast.LENGTH_SHORT).show();
            return;
        }

        if (mAuth.getCurrentUser() == null) return;
        String managerUid = mAuth.getCurrentUser().getUid();

        db.collection("users").document(managerUid).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                String messId = documentSnapshot.getString("messId");

                String memberUserId = "MEM-" + UUID.randomUUID().toString().substring(0, 5).toUpperCase();

                Map<String, Object> member = new HashMap<>();
                member.put("name", name);
                member.put("mobile", mobile);
                member.put("email", email);
                member.put("description", description);
                member.put("messId", messId);
                member.put("role", "member");
                member.put("userID", memberUserId); // <-- এখানে পরিবর্তন করা হয়েছে
                member.put("password", "1234");

                db.collection("users").add(member)
                        .addOnSuccessListener(documentReference -> {
                            Toast.makeText(AddMemberActivity.this, "Member added successfully!", Toast.LENGTH_SHORT).show();
                            finish();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(AddMemberActivity.this, "Error adding member: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
            } else {
                Toast.makeText(this, "Could not find manager details.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}