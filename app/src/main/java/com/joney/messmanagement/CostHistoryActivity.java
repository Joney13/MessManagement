package com.joney.messmanagement;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import java.util.Objects;

public class CostHistoryActivity extends AppCompatActivity {

    private RecyclerView costHistoryRecyclerView;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private CostAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cost_history);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        costHistoryRecyclerView = findViewById(R.id.costHistoryRecyclerView);
        costHistoryRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        FloatingActionButton fabAddCost = findViewById(R.id.fab_add_cost);
        fabAddCost.setOnClickListener(v -> {
            startActivity(new Intent(this, CostActivity.class));
        });
    }

    private void setupAndStartAdapter() {
        String adminUid = Objects.requireNonNull(mAuth.getCurrentUser()).getUid();
        db.collection("users").document(adminUid).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                String currentMessId = documentSnapshot.getString("messId");
                if (currentMessId != null) {
                    Query query = db.collection("costs")
                            .whereEqualTo("messId", currentMessId)
                            .orderBy("costDate", Query.Direction.DESCENDING);

                    FirestoreRecyclerOptions<Cost> options = new FirestoreRecyclerOptions.Builder<Cost>()
                            .setQuery(query, Cost.class)
                            .build();

                    adapter = new CostAdapter(options);
                    costHistoryRecyclerView.setAdapter(adapter);
                    adapter.startListening();
                }
            } else {
                Toast.makeText(this, "Could not find admin details.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        // প্রতিবার অ্যাক্টিভিটি চালু হলে নতুন করে অ্যাডাপ্টার সেটআপ করা হচ্ছে
        setupAndStartAdapter();
    }

    @Override
    protected void onStop() {
        super.onStop();
        // নিশ্চিত করা হচ্ছে যে অ্যাডাপ্টারটি stop হবে
        if (adapter != null) {
            adapter.stopListening();
        }
    }
}