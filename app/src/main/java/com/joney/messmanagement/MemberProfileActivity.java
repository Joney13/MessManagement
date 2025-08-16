package com.joney.messmanagement;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.firestore.FirebaseFirestore;

public class MemberProfileActivity extends AppCompatActivity {

    private TextView tvProfileName, tvProfileDescription, tvProfileUserId, tvProfileMobile, tvProfileEmail;
    private Button btnSendPrivateMessage;
    private FirebaseFirestore db;
    private String memberDocId;
    private String memberName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_member_profile);

        db = FirebaseFirestore.getInstance();
        memberDocId = getIntent().getStringExtra("MEMBER_DOC_ID");

        tvProfileName = findViewById(R.id.tvProfileName);
        tvProfileDescription = findViewById(R.id.tvProfileDescription);
        tvProfileUserId = findViewById(R.id.tvProfileUserId);
        tvProfileMobile = findViewById(R.id.tvProfileMobile);
        tvProfileEmail = findViewById(R.id.tvProfileEmail);
        btnSendPrivateMessage = findViewById(R.id.btnSendPrivateMessage);

        loadMemberProfile();

        btnSendPrivateMessage.setOnClickListener(v -> {
            if (memberDocId != null && memberName != null) {
                Intent intent = new Intent(this, ChatActivity.class);
                intent.putExtra("chatType", "private");
                intent.putExtra("receiverId", memberDocId);
                intent.putExtra("receiverName", memberName);
                startActivity(intent);
            } else {
                Toast.makeText(this, "Cannot start chat. User data is missing.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadMemberProfile() {
        if (memberDocId == null) {
            Toast.makeText(this, "Could not load member profile.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        db.collection("users").document(memberDocId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Member member = documentSnapshot.toObject(Member.class);
                        if (member != null) {
                            memberName = member.getName();
                            tvProfileName.setText(member.getName());
                            tvProfileDescription.setText(member.getDescription());
                            // সঠিক মেথড getUserID() ব্যবহার করা হয়েছে
                            tvProfileUserId.setText("User ID: " + member.getUserID());
                            tvProfileMobile.setText("Mobile: " + member.getMobile());
                            tvProfileEmail.setText("Email: " + member.getEmail());
                        }
                    }
                });
    }
}