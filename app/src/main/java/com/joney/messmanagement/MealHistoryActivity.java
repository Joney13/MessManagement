package com.joney.messmanagement;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

public class MealHistoryActivity extends AppCompatActivity {

    private static final String TAG = "MealHistoryActivity";

    private RecyclerView mealHistoryRecyclerView;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private MealHistoryAdapter adapter;
    private String currentMessId;
    private Query currentQuery; // Store current query for refresh

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_meal_history);

        initializeViews();
        initializeFirebase();
        setupRecyclerView();
    }

    private void initializeViews() {
        // Setup toolbar if exists
        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setTitle("Meal History");
            }
        }

        mealHistoryRecyclerView = findViewById(R.id.mealHistoryRecyclerView);
    }

    private void initializeFirebase() {
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
    }

    private void setupRecyclerView() {
        // Check if user is authenticated
        if (mAuth.getCurrentUser() == null) {
            Log.e(TAG, "User not authenticated");
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String adminUid = mAuth.getCurrentUser().getUid();

        db.collection("users").document(adminUid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    try {
                        if (documentSnapshot.exists()) {
                            currentMessId = documentSnapshot.getString("messId");

                            if (!TextUtils.isEmpty(currentMessId)) {
                                setupFirestoreRecyclerView();
                            } else {
                                Log.e(TAG, "MessId is null or empty");
                                Toast.makeText(this, "Mess ID not found", Toast.LENGTH_SHORT).show();
                                finish();
                            }
                        } else {
                            Log.e(TAG, "Admin document does not exist");
                            Toast.makeText(this, "Admin details not found", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing admin document", e);
                        Toast.makeText(this, "Error loading admin data", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching admin document", e);
                    Toast.makeText(this, "Error loading admin data", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void setupFirestoreRecyclerView() {
        try {
            // Create query and store it for refresh
            currentQuery = db.collection("meals")
                    .whereEqualTo("messId", currentMessId)
                    .orderBy("date", Query.Direction.DESCENDING);

            // Create FirestoreRecyclerOptions
            FirestoreRecyclerOptions<MealHistoryItem> options =
                    new FirestoreRecyclerOptions.Builder<MealHistoryItem>()
                            .setQuery(currentQuery, MealHistoryItem.class)
                            .setLifecycleOwner(this)
                            .build();

            // Create adapter
            adapter = new MealHistoryAdapter(options);

            // Set delete listener with proper interface implementation
            adapter.setOnMealDeletedListener(new MealHistoryAdapter.OnMealDeletedListener() {
                @Override
                public void onMealDeleted() {
                    Log.d(TAG, "Meal deleted, forcing immediate refresh...");
                    // Force immediate refresh
                    forceRefreshAdapter();
                }

                @Override
                public void onMealDeleteStarted() {
                    Log.d(TAG, "Meal delete started");
                    // Optional: Show loading indicator
                }
            });

            // Setup RecyclerView
            LinearLayoutManager layoutManager = new LinearLayoutManager(this);
            mealHistoryRecyclerView.setLayoutManager(layoutManager);
            mealHistoryRecyclerView.setAdapter(adapter);

            // Disable item animator to prevent crashes
            mealHistoryRecyclerView.setItemAnimator(null);

            Log.d(TAG, "RecyclerView setup completed successfully");

        } catch (Exception e) {
            Log.e(TAG, "Error setting up RecyclerView", e);
            Toast.makeText(this, "Error setting up meal history", Toast.LENGTH_SHORT).show();
        }
    }

    private void forceRefreshAdapter() {
        try {
            if (currentQuery != null && !isFinishing() && !isDestroyed()) {
                runOnUiThread(() -> {
                    try {
                        Log.d(TAG, "Recreating adapter for immediate refresh");

                        // Stop current adapter
                        if (adapter != null) {
                            adapter.stopListening();
                        }

                        // Create new FirestoreRecyclerOptions with same query
                        FirestoreRecyclerOptions<MealHistoryItem> newOptions =
                                new FirestoreRecyclerOptions.Builder<MealHistoryItem>()
                                        .setQuery(currentQuery, MealHistoryItem.class)
                                        .setLifecycleOwner(this)
                                        .build();

                        // Create new adapter
                        adapter = new MealHistoryAdapter(newOptions);

                        // Set delete listener again
                        adapter.setOnMealDeletedListener(new MealHistoryAdapter.OnMealDeletedListener() {
                            @Override
                            public void onMealDeleted() {
                                Log.d(TAG, "Meal deleted, forcing immediate refresh...");
                                forceRefreshAdapter();
                            }

                            @Override
                            public void onMealDeleteStarted() {
                                Log.d(TAG, "Meal delete started");
                            }
                        });

                        // Set new adapter to RecyclerView
                        mealHistoryRecyclerView.setAdapter(adapter);

                        // Start listening
                        adapter.startListening();

                        Log.d(TAG, "Adapter recreated and refresh completed");

                    } catch (Exception e) {
                        Log.e(TAG, "Error recreating adapter", e);
                        Toast.makeText(this, "Error refreshing data", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in forceRefreshAdapter", e);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Adapter will automatically start listening when activity starts
        Log.d(TAG, "Activity started");
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Adapter will automatically stop listening when activity stops
        Log.d(TAG, "Activity stopped");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        try {
            // Stop adapter listening
            if (adapter != null) {
                adapter.stopListening();
            }

            // Clean up RecyclerView
            if (mealHistoryRecyclerView != null) {
                mealHistoryRecyclerView.setAdapter(null);
                Log.d(TAG, "RecyclerView adapter cleared");
            }

            // Clear references
            adapter = null;
            currentQuery = null;

        } catch (Exception e) {
            Log.e(TAG, "Error during cleanup", e);
        }

        Log.d(TAG, "Activity destroyed");
    }

    @Override
    public boolean onSupportNavigateUp() {
        // Handle back button in toolbar
        onBackPressed();
        return true;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Log.d(TAG, "Back button pressed");
    }

    // Helper method to safely run UI operations
    private void safeRunOnUiThread(Runnable action) {
        if (!isFinishing() && !isDestroyed()) {
            runOnUiThread(action);
        }
    }

    // Method to refresh data if needed
    public void refreshData() {
        Log.d(TAG, "Manual refresh requested");
        forceRefreshAdapter();
    }

    // Alternative refresh method using notifyDataSetChanged
    public void simpleRefresh() {
        if (adapter != null) {
            try {
                runOnUiThread(() -> {
                    adapter.notifyDataSetChanged();
                    Log.d(TAG, "Simple refresh completed");
                });
            } catch (Exception e) {
                Log.e(TAG, "Error in simple refresh", e);
            }
        }
    }

    // Method to check if activity is safe for UI operations
    private boolean isActivitySafe() {
        return !isFinishing() && !isDestroyed();
    }
}