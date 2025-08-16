package com.joney.messmanagement;

import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class SelectMemberForHistoryActivity extends AppCompatActivity {

    private RecyclerView selectMemberRecyclerView;
    private SelectMemberAdapter adapter;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private List<Member> memberList = new ArrayList<>();
    private List<String> documentIds = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_member_for_history);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        setupRecyclerView();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadMembersData();
    }

    private void setupRecyclerView() {
        selectMemberRecyclerView = findViewById(R.id.selectMemberRecyclerView);
        selectMemberRecyclerView.setHasFixedSize(true);
        selectMemberRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        // একটি খালি তালিকা দিয়ে অ্যাডাপ্টারটি তৈরি করে সেট করা হচ্ছে
        adapter = new SelectMemberAdapter(new ArrayList<>(), new ArrayList<>());
        selectMemberRecyclerView.setAdapter(adapter);
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