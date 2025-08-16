package com.joney.messmanagement;

import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

public class AdminMealOffRequestsActivity extends AppCompatActivity {

    private RecyclerView rvAdminMealOffRequests;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private AdminMealOffAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_meal_off_requests);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        rvAdminMealOffRequests = findViewById(R.id.rvAdminMealOffRequests);

        setupRecyclerView();
    }

    private void setupRecyclerView() {
        String adminUid = mAuth.getCurrentUser().getUid();
        db.collection("users").document(adminUid).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                String messId = documentSnapshot.getString("messId");
                if (messId != null) {
                    Query query = db.collection("meal_off_requests")
                            .whereEqualTo("messId", messId)
                            .orderBy("startDate", Query.Direction.DESCENDING);

                    FirestoreRecyclerOptions<MealOffRequest> options = new FirestoreRecyclerOptions.Builder<MealOffRequest>()
                            .setQuery(query, MealOffRequest.class)
                            .setLifecycleOwner(this)
                            .build();

                    adapter = new AdminMealOffAdapter(options);
                    rvAdminMealOffRequests.setLayoutManager(new LinearLayoutManager(this));
                    rvAdminMealOffRequests.setAdapter(adapter);
                }
            }
        });
    }
}