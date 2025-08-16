package com.joney.messmanagement;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MyProfileActivity extends AppCompatActivity {

    private static final String TAG = "MyProfileActivity";

    // UI Components
    private TextView tvProfileName, tvProfileTotalDeposit, tvProfileTotalMeal,
            tvProfileGuestMeal, tvProfileTotalCost, tvProfileBalance;
    private TextInputEditText etMessageToAdmin;
    private MaterialButton btnSendMessage;
    private CardView cardMealHistory, cardMealOff, cardChatAdmin, cardSettings;
    private Toolbar toolbar;

    // Firebase
    private FirebaseFirestore db;

    // Data
    private String memberDocId;
    private String memberName;
    private String messId;
    private double totalDeposit = 0.0;
    private double totalMeals = 0.0;
    private double guestMeals = 0.0;
    private double totalCost = 0.0;
    private double mealRate = 0.0;

    // Listeners for real-time updates
    private List<ListenerRegistration> listeners = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_profile);

        initializeViews();
        initializeFirebase();
        getMemberData();
        setupClickListeners();
    }

    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar_my_profile);
        setSupportActionBar(toolbar);

        // Enable back button
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Profile TextViews
        tvProfileName = findViewById(R.id.tvProfileName);
        tvProfileTotalDeposit = findViewById(R.id.tvProfileTotalDeposit);
        tvProfileTotalMeal = findViewById(R.id.tvProfileTotalMeal);
        tvProfileGuestMeal = findViewById(R.id.tvProfileGuestMeal);
        tvProfileTotalCost = findViewById(R.id.tvProfileTotalCost);
        tvProfileBalance = findViewById(R.id.tvProfileBalance);

        // Message components
        etMessageToAdmin = findViewById(R.id.etMessageToAdmin);
        btnSendMessage = findViewById(R.id.btnSendMessage);

        // CardViews for quick actions
        cardMealHistory = findViewById(R.id.cardMealHistory);
        cardMealOff = findViewById(R.id.cardMealOff);
        cardChatAdmin = findViewById(R.id.cardChatAdmin);
        cardSettings = findViewById(R.id.cardSettings);
    }

    private void initializeFirebase() {
        db = FirebaseFirestore.getInstance();
    }

    private void getMemberData() {
        memberDocId = getIntent().getStringExtra("MEMBER_DOC_ID");

        if (TextUtils.isEmpty(memberDocId)) {
            Log.e(TAG, "Member ID not found");
            Toast.makeText(this, "Error: Member ID not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadMemberProfile();
    }

    private void setupClickListeners() {
        // Send message button
        btnSendMessage.setOnClickListener(v -> sendMessageToAdmin());

        // Quick action cards
        cardMealHistory.setOnClickListener(v -> {
            Intent intent = new Intent(this, MealHistoryActivity.class);
            intent.putExtra("MEMBER_DOC_ID", memberDocId);
            startActivity(intent);
        });

        cardMealOff.setOnClickListener(v -> {
            Intent intent = new Intent(this, MealOffActivity.class);
            intent.putExtra("MEMBER_DOC_ID", memberDocId);
            startActivity(intent);
        });

        cardChatAdmin.setOnClickListener(v -> {
            Intent intent = new Intent(this, ChatActivity.class);
            intent.putExtra("USER_TYPE", "MEMBER");
            intent.putExtra("MEMBER_DOC_ID", memberDocId);
            intent.putExtra("MEMBER_NAME", memberName);
            intent.putExtra("MESS_ID", messId);
            startActivity(intent);
        });

        cardSettings.setOnClickListener(v -> {
            Intent intent = new Intent(this, SettingsActivity.class);
            intent.putExtra("MEMBER_DOC_ID", memberDocId);
            startActivity(intent);
        });

        cardMealHistory.setOnClickListener(v -> {
            Intent intent = new Intent(this, MemberMealHistoryActivity.class);
            intent.putExtra("MEMBER_DOC_ID", memberDocId);
            startActivity(intent);
        });

        // Toolbar back button
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void loadMemberProfile() {
        db.collection("users").document(memberDocId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        memberName = documentSnapshot.getString("name");
                        messId = documentSnapshot.getString("messId");

                        if (!TextUtils.isEmpty(memberName)) {
                            tvProfileName.setText(memberName);
                        }

                        if (!TextUtils.isEmpty(messId)) {
                            // Load initial data and setup real-time listeners
                            calculateAndDisplaySummary();
                            setupRealTimeListeners();
                        } else {
                            Toast.makeText(this, "Mess ID not found", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, "Member not found", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading member profile", e);
                    Toast.makeText(this, "Error loading profile", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void calculateAndDisplaySummary() {
        if (TextUtils.isEmpty(memberDocId) || TextUtils.isEmpty(messId)) {
            return;
        }

        // সব ডেটা আনার জন্য আলাদা আলাদা টাস্ক তৈরি করা হচ্ছে
        Task<QuerySnapshot> myDepositsTask = db.collection("deposits")
                .whereEqualTo("memberId", memberDocId).get();
        Task<QuerySnapshot> myMealsTask = db.collection("meals")
                .whereEqualTo("memberId", memberDocId).get();
        Task<QuerySnapshot> allCostsTask = db.collection("costs")
                .whereEqualTo("messId", messId).get();
        Task<QuerySnapshot> allMealsTask = db.collection("meals")
                .whereEqualTo("messId", messId).get();

        // সব টাস্ক একসাথে সফল হওয়ার জন্য অপেক্ষা করা হচ্ছে
        Tasks.whenAllSuccess(myDepositsTask, myMealsTask, allCostsTask, allMealsTask)
                .addOnSuccessListener(list -> {
                    try {
                        QuerySnapshot myDepositsResult = (QuerySnapshot) list.get(0);
                        QuerySnapshot myMealsResult = (QuerySnapshot) list.get(1);
                        QuerySnapshot allCostsResult = (QuerySnapshot) list.get(2);
                        QuerySnapshot allMealsResult = (QuerySnapshot) list.get(3);

                        // Calculate totals
                        calculateTotals(myDepositsResult, myMealsResult, allCostsResult, allMealsResult);

                        // Update UI
                        updateUI();

                    } catch (Exception e) {
                        Log.e(TAG, "Error processing data", e);
                        Toast.makeText(this, "Error calculating summary", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching data", e);
                    Toast.makeText(this, "Error loading data", Toast.LENGTH_SHORT).show();
                });
    }

    private void calculateTotals(QuerySnapshot myDepositsResult, QuerySnapshot myMealsResult,
                                 QuerySnapshot allCostsResult, QuerySnapshot allMealsResult) {

        // Calculate total deposit
        totalDeposit = 0.0;
        for (QueryDocumentSnapshot doc : myDepositsResult) {
            Double amount = doc.getDouble("amount");
            if (amount != null) {
                totalDeposit += amount;
            }
        }

        // Calculate total meals and guest meals
        totalMeals = 0.0;
        guestMeals = 0.0;
        for (QueryDocumentSnapshot doc : myMealsResult) {
            Double meals = doc.getDouble("totalMeal");
            if (meals != null) {
                totalMeals += meals;
            }

            // Calculate guest meals
            Double guestBreakfast = doc.getDouble("guestBreakfast");
            Double guestLunch = doc.getDouble("guestLunch");
            Double guestDinner = doc.getDouble("guestDinner");

            if (guestBreakfast != null) guestMeals += guestBreakfast;
            if (guestLunch != null) guestMeals += guestLunch;
            if (guestDinner != null) guestMeals += guestDinner;
        }

        // Calculate mess total cost
        double messTotalCost = 0.0;
        for (QueryDocumentSnapshot doc : allCostsResult) {
            Double amount = doc.getDouble("amount");
            if (amount != null) {
                messTotalCost += amount;
            }
        }

        // Calculate mess total meals
        double messTotalMeals = 0.0;
        for (QueryDocumentSnapshot doc : allMealsResult) {
            Double meals = doc.getDouble("totalMeal");
            if (meals != null) {
                messTotalMeals += meals;
            }
        }

        // Calculate meal rate and my total cost
        mealRate = (messTotalMeals > 0) ? (messTotalCost / messTotalMeals) : 0.0;
        totalCost = totalMeals * mealRate;
    }

    private void updateUI() {
        runOnUiThread(() -> {
            // Update deposit
            tvProfileTotalDeposit.setText(String.format(Locale.US, "BDT %.2f", totalDeposit));

            // Update meals (excluding guest meals from total)
            tvProfileTotalMeal.setText(String.format(Locale.US, "%.1f", totalMeals - guestMeals));
            tvProfileGuestMeal.setText(String.format(Locale.US, "%.1f", guestMeals));

            // Update cost
            tvProfileTotalCost.setText(String.format(Locale.US, "BDT %.2f", totalCost));

            // Update balance with color coding
            double balance = totalDeposit - totalCost;
            tvProfileBalance.setText(String.format(Locale.US, "BDT %.2f", balance));

            // Change color based on balance
            if (balance >= 0) {
                tvProfileBalance.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
            } else {
                tvProfileBalance.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            }
        });
    }

    private void setupRealTimeListeners() {
        // Listen to deposit changes
        ListenerRegistration depositListener = db.collection("deposits")
                .whereEqualTo("memberId", memberDocId)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error listening to deposits", error);
                        return;
                    }
                    // Recalculate when deposits change
                    calculateAndDisplaySummary();
                });

        // Listen to meal changes
        ListenerRegistration mealListener = db.collection("meals")
                .whereEqualTo("memberId", memberDocId)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error listening to meals", error);
                        return;
                    }
                    // Recalculate when meals change
                    calculateAndDisplaySummary();
                });

        // Listen to cost changes (affects meal rate)
        ListenerRegistration costListener = db.collection("costs")
                .whereEqualTo("messId", messId)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error listening to costs", error);
                        return;
                    }
                    // Recalculate when costs change
                    calculateAndDisplaySummary();
                });

        listeners.add(depositListener);
        listeners.add(mealListener);
        listeners.add(costListener);
    }

    private void sendMessageToAdmin() {
        String message = etMessageToAdmin.getText().toString().trim();

        if (TextUtils.isEmpty(message)) {
            etMessageToAdmin.setError("Message cannot be empty");
            return;
        }

        if (TextUtils.isEmpty(memberName) || TextUtils.isEmpty(messId)) {
            Toast.makeText(this, "Member data not loaded", Toast.LENGTH_SHORT).show();
            return;
        }

        // Disable button to prevent multiple sends
        btnSendMessage.setEnabled(false);
        btnSendMessage.setText("Sending...");

        Map<String, Object> messageData = new HashMap<>();
        messageData.put("messId", messId);
        messageData.put("memberId", memberDocId);
        messageData.put("memberName", memberName);
        messageData.put("message", message);
        messageData.put("timestamp", Timestamp.now());
        messageData.put("isResolved", false);
        messageData.put("type", "member_request");

        db.collection("requests")
                .add(messageData)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Message sent to admin successfully", Toast.LENGTH_SHORT).show();
                    etMessageToAdmin.setText("");

                    // Re-enable button
                    btnSendMessage.setEnabled(true);
                    btnSendMessage.setText("Send Message");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error sending message", e);
                    Toast.makeText(this, "Failed to send message", Toast.LENGTH_SHORT).show();

                    // Re-enable button
                    btnSendMessage.setEnabled(true);
                    btnSendMessage.setText("Send Message");
                });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Clean up all listeners to prevent memory leaks
        for (ListenerRegistration listener : listeners) {
            if (listener != null) {
                try {
                    listener.remove();
                } catch (Exception e) {
                    Log.w(TAG, "Error removing listener", e);
                }
            }
        }
        listeners.clear();

        Log.d(TAG, "All listeners cleaned up");
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh data when returning to activity
        if (!TextUtils.isEmpty(memberDocId) && !TextUtils.isEmpty(messId)) {
            calculateAndDisplaySummary();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Optional: You can pause some operations here if needed
    }

    // Helper method to safely run UI operations
    private void safeRunOnUiThread(Runnable action) {
        if (!isFinishing() && !isDestroyed()) {
            runOnUiThread(action);
        }
    }

    // Method to refresh all data manually
    private void refreshData() {
        if (!TextUtils.isEmpty(memberDocId) && !TextUtils.isEmpty(messId)) {
            calculateAndDisplaySummary();
        }
    }

    // Method to handle network errors gracefully
    private void handleNetworkError(Exception e) {
        Log.e(TAG, "Network error", e);
        safeRunOnUiThread(() -> {
            Toast.makeText(this, "Network error. Please check your connection.", Toast.LENGTH_SHORT).show();
        });
    }

    // Method to validate data before processing
    private boolean validateData() {
        if (TextUtils.isEmpty(memberDocId)) {
            Log.e(TAG, "Member ID is null");
            return false;
        }

        if (TextUtils.isEmpty(messId)) {
            Log.e(TAG, "Mess ID is null");
            return false;
        }

        return true;
    }

    // Method to format currency with proper locale
    private String formatCurrency(double amount) {
        return String.format(Locale.US, "BDT %.2f", amount);
    }

    // Method to format meal count
    private String formatMealCount(double meals) {
        return String.format(Locale.US, "%.1f", meals);
    }

    // Method to show loading state
    private void showLoadingState(boolean isLoading) {
        safeRunOnUiThread(() -> {
            if (isLoading) {
                // You can add a progress bar here if needed
                btnSendMessage.setEnabled(false);
            } else {
                btnSendMessage.setEnabled(true);
            }
        });
    }

    // Method to handle empty states
    private void handleEmptyState(String dataType) {
        Log.w(TAG, "No data found for: " + dataType);
        safeRunOnUiThread(() -> {
            Toast.makeText(this, "No " + dataType + " data found", Toast.LENGTH_SHORT).show();
        });
    }

    // Method to update balance color based on amount
    private void updateBalanceColor(double balance) {
        safeRunOnUiThread(() -> {
            if (balance >= 0) {
                tvProfileBalance.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
            } else {
                tvProfileBalance.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            }
        });
    }

    // Method to check if activity is safe for UI operations
    private boolean isActivitySafe() {
        return !isFinishing() && !isDestroyed();
    }

    // Method to log user actions for debugging
    private void logUserAction(String action) {
        Log.d(TAG, "User action: " + action + " by member: " + memberName);
    }
}