package com.joney.messmanagement;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class MembersActivity extends AppCompatActivity {

    private RecyclerView membersRecyclerView;
    private MemberAdapter adapter;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_members);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        setupRecyclerView(); // অ্যাডাপ্টারটি এখানে একবারই তৈরি হবে

        FloatingActionButton fabAddMember = findViewById(R.id.fab_add_member);
        fabAddMember.setOnClickListener(v -> {
            startActivity(new Intent(MembersActivity.this, AddMemberActivity.class));
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadMembersData(); // প্রতিবার পেজে ফিরে আসলে ডেটা রিফ্রেশ হবে
    }

    private void setupRecyclerView() {
        membersRecyclerView = findViewById(R.id.membersRecyclerView);
        membersRecyclerView.setHasFixedSize(true);
        membersRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        // একটি খালি তালিকা দিয়ে অ্যাডাপ্টারটি তৈরি করে সেট করা হচ্ছে
        adapter = new MemberAdapter(new ArrayList<>(), new ArrayList<>());
        membersRecyclerView.setAdapter(adapter);
    }

    private void loadMembersData() {
        if (mAuth.getCurrentUser() == null) return;

        String adminUid = mAuth.getCurrentUser().getUid();
        db.collection("users").document(adminUid).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                String currentMessId = documentSnapshot.getString("messId");
                if (currentMessId != null) {
                    Query query = db.collection("users")
                            .whereEqualTo("messId", currentMessId)
                            .whereEqualTo("role", "member")
                            .orderBy("name", Query.Direction.ASCENDING);

                    query.get().addOnCompleteListener(task -> {
                        if (task.isSuccessful() && task.getResult() != null) {
                            List<Member> newMemberList = new ArrayList<>();
                            List<String> newDocIds = new ArrayList<>();
                            for (QueryDocumentSnapshot doc : task.getResult()) {
                                newMemberList.add(doc.toObject(Member.class));
                                newDocIds.add(doc.getId());
                            }
                            // নতুন ডেটা দিয়ে অ্যাডাপ্টার আপডেট করা হচ্ছে
                            adapter.updateData(newMemberList, newDocIds);
                        } else {
                            Toast.makeText(this, "Failed to load members.", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            } else {
                Toast.makeText(this, "Admin details not found.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}