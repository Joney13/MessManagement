package com.joney.messmanagement;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MemberDashboardActivity extends AppCompatActivity {

    private static final String TAG = "MemberDashboard";
    private static final int CHAT_REQUEST_CODE = 1001;

    // UI Components
    private TextView tvWelcomeMember, tvTotalMembers, tvTotalDeposit, tvTotalCost,
            tvRemainingBalance, tvTotalMeals, tvMealRate, tvMemberMenuBanner,
            tvMemberNoticeBoard, tvChatBadgeMember;

    // Firebase
    private FirebaseFirestore db;

    // Data
    private String memberDocId;
    private String messId;
    private String memberName;

    // Firestore Listeners (for proper cleanup)
    private List<ListenerRegistration> listeners = new ArrayList<>();
    private ListenerRegistration currentMessageListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_member_dashboard);

        // Initialize views and data
        initializeViews();
        initializeData();

        // Validate member data
        if (!validateMemberData()) {
            return;
        }

        // Load member and mess data
        loadMemberAndMessData();

        // Setup click listeners
        setupClickListeners();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Chat থেকে ফিরে এলে timestamp update করুন
        if (requestCode == CHAT_REQUEST_CODE) {
            updateLastReadTimestamp();
        }
    }

    private void initializeViews() {
        Toolbar toolbar = findViewById(R.id.toolbar_member_dashboard);
        setSupportActionBar(toolbar);

        // Initialize TextViews
        tvWelcomeMember = findViewById(R.id.tvWelcomeMember);
        tvTotalMembers = findViewById(R.id.tvTotalMembers);
        tvTotalDeposit = findViewById(R.id.tvTotalDeposit);
        tvTotalCost = findViewById(R.id.tvTotalCost);
        tvRemainingBalance = findViewById(R.id.tvRemainingBalance);
        tvTotalMeals = findViewById(R.id.tvTotalMeals);
        tvMealRate = findViewById(R.id.tvMealRate);
        tvMemberMenuBanner = findViewById(R.id.tvMemberMenuBanner);
        tvMemberNoticeBoard = findViewById(R.id.tvMemberNoticeBoard);
        tvChatBadgeMember = findViewById(R.id.tvChatBadgeMember);
    }

    private void initializeData() {
        db = FirebaseFirestore.getInstance();
        memberDocId = getIntent().getStringExtra("MEMBER_DOC_ID");
    }

    private boolean validateMemberData() {
        if (TextUtils.isEmpty(memberDocId)) {
            Log.e(TAG, "Member ID not found in intent");
            Toast.makeText(this, "Error: Member ID not found", Toast.LENGTH_LONG).show();
            finish();
            return false;
        }
        return true;
    }

    private void setupClickListeners() {
        Button btnMyProfile = findViewById(R.id.btnMyProfile);
        Button btnMealOff = findViewById(R.id.btnMealOff);
        FloatingActionButton fabChatMember = findViewById(R.id.fab_chat_member);

        btnMyProfile.setOnClickListener(v -> {
            Intent intent = new Intent(this, MyProfileActivity.class);
            intent.putExtra("MEMBER_DOC_ID", memberDocId);
            startActivity(intent);
        });

        btnMealOff.setOnClickListener(v -> {
            Intent intent = new Intent(this, MealOffActivity.class);
            intent.putExtra("MEMBER_DOC_ID", memberDocId);
            startActivity(intent);
        });

        fabChatMember.setOnClickListener(v -> openChat());
    }

    private void openChat() {
        if (TextUtils.isEmpty(memberName) || TextUtils.isEmpty(messId)) {
            Toast.makeText(this, "Please wait for data to load", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra("USER_TYPE", "MEMBER");
        intent.putExtra("MEMBER_DOC_ID", memberDocId);
        intent.putExtra("MEMBER_NAME", memberName);
        intent.putExtra("MESS_ID", messId);

        // startActivityForResult ব্যবহার করুন যাতে chat থেকে ফিরে এলে জানতে পারেন
        startActivityForResult(intent, CHAT_REQUEST_CODE);
    }

    private void updateLastReadTimestamp() {
        if (!TextUtils.isEmpty(memberDocId)) {
            db.collection("users").document(memberDocId)
                    .update("lastReadTimestamp", Timestamp.now())
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Last read timestamp updated successfully");
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to update last read timestamp", e);
                    });
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.member_dashboard_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.menu_logout_member) {
            logout();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void logout() {
        Intent intent = new Intent(this, WelcomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void loadMemberAndMessData() {
        db.collection("users").document(memberDocId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        messId = documentSnapshot.getString("messId");
                        memberName = documentSnapshot.getString("name");

                        // Validate required data
                        if (TextUtils.isEmpty(messId) || TextUtils.isEmpty(memberName)) {
                            Log.e(TAG, "Missing member data: messId or name is null");
                            Toast.makeText(this, "Member data incomplete", Toast.LENGTH_SHORT).show();
                            finish();
                            return;
                        }

                        // Update UI
                        tvWelcomeMember.setText("Welcome, " + memberName);
                        if (getSupportActionBar() != null) {
                            getSupportActionBar().setTitle("Mess Dashboard");
                        }

                        // Load mess-related data
                        setupDataListeners();
                    } else {
                        Log.e(TAG, "Member document does not exist");
                        Toast.makeText(this, "Member not found", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading member data", e);
                    Toast.makeText(this, "Error loading member data", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void setupDataListeners() {
        // Setup all data listeners
        setupUnreadMessageListener();
        setupTodaysMenuListener();
        setupLatestNoticeListener();
        setupMessDataListeners();
    }

    private void setupUnreadMessageListener() {
        if (TextUtils.isEmpty(messId) || TextUtils.isEmpty(memberDocId)) {
            Log.w(TAG, "Cannot setup unread message listener: missing data");
            return;
        }

        Log.d(TAG, "Setting up unread message listener for member: " + memberDocId);

        // Real-time listener for user document to get updated lastReadTimestamp
        ListenerRegistration userListener = db.collection("users").document(memberDocId)
                .addSnapshotListener((userDoc, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error listening to user document", error);
                        return;
                    }

                    if (userDoc == null || !userDoc.exists()) {
                        Log.w(TAG, "User document not found");
                        return;
                    }

                    Timestamp lastRead = userDoc.getTimestamp("lastReadTimestamp");
                    if (lastRead == null) {
                        lastRead = new Timestamp(0, 0);
                    }

                    Log.d(TAG, "Member lastReadTimestamp: " + lastRead.toDate());

                    // Remove previous message listener if exists
                    removeMessageListener();

                    // Setup message listener with updated timestamp
                    setupMessageListener(lastRead);
                });

        listeners.add(userListener);
    }

    private void removeMessageListener() {
        if (currentMessageListener != null) {
            currentMessageListener.remove();
            currentMessageListener = null;
            Log.d(TAG, "Previous message listener removed");
        }
    }

    private void setupMessageListener(Timestamp lastRead) {
        Log.d(TAG, "Setting up message listener with timestamp: " + lastRead.toDate());

        currentMessageListener = db.collection("messages")
                .whereEqualTo("messId", messId)
                .whereGreaterThan("timestamp", lastRead)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error listening to messages", error);
                        return;
                    }

                    safeRunOnUiThread(() -> updateUnreadMessageBadge(snapshots));
                });

        // Don't add to listeners list as we manage it separately
    }

    private void updateUnreadMessageBadge(com.google.firebase.firestore.QuerySnapshot snapshots) {
        if (snapshots == null || snapshots.isEmpty()) {
            tvChatBadgeMember.setVisibility(View.GONE);
            Log.d(TAG, "No unread messages, hiding badge");
            return;
        }

        int unreadCount = 0;
        // Count messages not sent by current member
        for (QueryDocumentSnapshot doc : snapshots) {
            String senderId = doc.getString("senderId");
            if (!TextUtils.isEmpty(senderId) && !senderId.equals(memberDocId)) {
                unreadCount++;
            }
        }

        Log.d(TAG, "Unread count: " + unreadCount);

        if (unreadCount > 0) {
            tvChatBadgeMember.setVisibility(View.VISIBLE);
            tvChatBadgeMember.setText(String.valueOf(unreadCount));
            Log.d(TAG, "Showing badge with count: " + unreadCount);
        } else {
            tvChatBadgeMember.setVisibility(View.GONE);
            Log.d(TAG, "No unread messages from others, hiding badge");
        }
    }

    private void setupTodaysMenuListener() {
        if (TextUtils.isEmpty(messId)) return;

        SimpleDateFormat docIdFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String todayDateString = docIdFormat.format(new Date());
        String documentId = todayDateString + "_" + messId;

        ListenerRegistration listener = db.collection("daily_menus").document(documentId)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error loading menu", error);
                        safeRunOnUiThread(() -> tvMemberMenuBanner.setText("Could not load menu"));
                        return;
                    }

                    String menuText = buildMenuText(snapshot);
                    safeRunOnUiThread(() -> tvMemberMenuBanner.setText(menuText));
                });

        listeners.add(listener);
    }

    private String buildMenuText(com.google.firebase.firestore.DocumentSnapshot snapshot) {
        if (snapshot == null || !snapshot.exists()) {
            return "Menu not set for today";
        }

        String breakfast = snapshot.getString("breakfastMenu");
        String lunch = snapshot.getString("lunchMenu");
        String dinner = snapshot.getString("dinnerMenu");

        StringBuilder menuText = new StringBuilder();

        if (!TextUtils.isEmpty(breakfast)) {
            menuText.append("Breakfast: ").append(breakfast).append("\n");
        }
        if (!TextUtils.isEmpty(lunch)) {
            menuText.append("Lunch: ").append(lunch).append("\n");
        }
        if (!TextUtils.isEmpty(dinner)) {
            menuText.append("Dinner: ").append(dinner);
        }

        return menuText.length() > 0 ? menuText.toString().trim() : "Menu not set for today";
    }

    private void setupLatestNoticeListener() {
        if (TextUtils.isEmpty(messId)) return;

        ListenerRegistration listener = db.collection("notices")
                .whereEqualTo("messId", messId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(1)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error loading notices", error);
                        safeRunOnUiThread(() -> tvMemberNoticeBoard.setText("Could not load notice"));
                        return;
                    }

                    String noticeText = "No new notices";
                    if (snapshots != null && !snapshots.isEmpty()) {
                        String latestNotice = snapshots.getDocuments().get(0).getString("noticeText");
                        if (!TextUtils.isEmpty(latestNotice)) {
                            noticeText = latestNotice;
                        }
                    }

                    final String finalNoticeText = noticeText;
                    safeRunOnUiThread(() -> tvMemberNoticeBoard.setText(finalNoticeText));
                });

        listeners.add(listener);
    }

    private void setupMessDataListeners() {
        setupMemberCountListener();
        setupDepositListener();
        setupCostListener();
        setupMealListener();
    }

    private void setupMemberCountListener() {
        ListenerRegistration listener = db.collection("users")
                .whereEqualTo("messId", messId)
                .whereEqualTo("role", "member")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error loading member count", error);
                        return;
                    }

                    int memberCount = (value != null) ? value.size() : 0;
                    safeRunOnUiThread(() -> tvTotalMembers.setText(String.valueOf(memberCount)));
                });

        listeners.add(listener);
    }

    private void setupDepositListener() {
        ListenerRegistration listener = db.collection("deposits")
                .whereEqualTo("messId", messId)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error loading deposits", error);
                        return;
                    }

                    double totalDeposit = calculateTotal(value, "amount");
                    safeRunOnUiThread(() -> {
                        tvTotalDeposit.setText(String.format(Locale.US, "BDT %.2f", totalDeposit));
                        updateBalanceAndMealRate();
                    });
                });

        listeners.add(listener);
    }

    private void setupCostListener() {
        ListenerRegistration listener = db.collection("costs")
                .whereEqualTo("messId", messId)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error loading costs", error);
                        return;
                    }

                    double totalCost = calculateTotal(value, "amount");
                    safeRunOnUiThread(() -> {
                        tvTotalCost.setText(String.format(Locale.US, "BDT %.2f", totalCost));
                        updateBalanceAndMealRate();
                    });
                });

        listeners.add(listener);
    }

    private void setupMealListener() {
        ListenerRegistration listener = db.collection("meals")
                .whereEqualTo("messId", messId)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error loading meals", error);
                        return;
                    }

                    double totalMeals = calculateTotal(value, "totalMeal");
                    safeRunOnUiThread(() -> {
                        tvTotalMeals.setText(String.format(Locale.US, "%.1f", totalMeals));
                        updateBalanceAndMealRate();
                    });
                });

        listeners.add(listener);
    }

    private double calculateTotal(com.google.firebase.firestore.QuerySnapshot snapshots, String fieldName) {
        double total = 0;
        if (snapshots != null) {
            for (QueryDocumentSnapshot doc : snapshots) {
                Double amount = doc.getDouble(fieldName);
                if (amount != null) {
                    total += amount;
                }
            }
        }
        return total;
    }

    private void updateBalanceAndMealRate() {
        try {
            // Parse values safely
            double totalDeposit = parseAmountFromText(tvTotalDeposit.getText().toString());
            double totalCost = parseAmountFromText(tvTotalCost.getText().toString());
            double totalMeals = parseAmountFromText(tvTotalMeals.getText().toString());

            // Calculate remaining balance
            double remainingBalance = totalDeposit - totalCost;
            tvRemainingBalance.setText(String.format(Locale.US, "BDT %.2f", remainingBalance));

            // Calculate meal rate
            double mealRate = (totalMeals > 0) ? (totalCost / totalMeals) : 0;
            tvMealRate.setText(String.format(Locale.US, "BDT %.2f", mealRate));

        } catch (Exception e) {
            Log.e(TAG, "Error calculating balance and meal rate", e);
        }
    }

    private double parseAmountFromText(String text) {
        if (TextUtils.isEmpty(text)) return 0;

        try {
            // Remove currency symbols and non-numeric characters except decimal point
            String cleanText = text.replaceAll("[^\\d.]", "");
            return Double.parseDouble(cleanText);
        } catch (NumberFormatException e) {
            Log.w(TAG, "Failed to parse amount from text: " + text);
            return 0;
        }
    }

    // Helper method to safely run UI operations on main thread
    private void safeRunOnUiThread(Runnable action) {
        if (isFinishing() || isDestroyed()) {
            return; // Don't run if activity is finishing
        }

        if (Thread.currentThread() == getMainLooper().getThread()) {
            action.run();
        } else {
            runOnUiThread(action);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Remove message listener separately
        removeMessageListener();

        // Clean up all other listeners
        for (ListenerRegistration listener : listeners) {
            if (listener != null) {
                listener.remove();
            }
        }
        listeners.clear();
        Log.d(TAG, "All listeners cleaned up");
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Optional: You can also remove listeners on pause and re-add on resume
        // for better performance if needed
    }
}