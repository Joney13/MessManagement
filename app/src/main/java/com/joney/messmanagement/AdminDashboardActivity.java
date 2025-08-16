package com.joney.messmanagement;

import android.animation.ValueAnimator;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class AdminDashboardActivity extends AppCompatActivity {

    private static final String TAG = "AdminDashboard";
    private static final int CHAT_REQUEST_CODE = 1001;

    // UI Components
    private TextView tvTotalMembers, tvTotalDeposit, tvTotalCost, tvRemainingBalance,
            tvTotalMeals, tvMealRate, tvNoticeBoard, tvMenuBanner,
            tvDailyAverageCost, tvChatBadgeAdmin;
    private BarChart barChart;
    private Toolbar toolbar;

    // Firebase
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    // Data
    private String currentMessId;
    private double totalCost = 0.0;
    private double totalDeposit = 0.0;
    private double totalMeals = 0.0;

    // Listeners (for proper cleanup)
    private List<ListenerRegistration> listeners = new ArrayList<>();
    private ListenerRegistration currentMessageListener;

    // Static instance for external access
    public static AdminDashboardActivity currentInstance;

    // BroadcastReceiver
    private BroadcastReceiver badgeClearReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, "Broadcast received: " + action);

            if ("clear-admin-badge".equals(action)) {
                Log.d(TAG, "Badge clear broadcast received");
                safeRunOnUiThread(() -> {
                    if (tvChatBadgeAdmin != null) {
                        tvChatBadgeAdmin.setVisibility(View.GONE);
                        Log.d(TAG, "Badge cleared successfully");
                    }
                });

                // Force refresh unread count after clearing
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    refreshUnreadCount();
                }, 1500);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        // Set static instance
        currentInstance = this;

        initializeViews();
        initializeFirebase();
        setupClickListeners();
        setupBottomNavigation();
        registerBroadcastReceiver();
        loadDashboardData();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Chat থেকে ফিরে এলে timestamp update করুন
        if (requestCode == CHAT_REQUEST_CODE) {
            updateLastReadTimestamp();
        }
    }

    private void updateLastReadTimestamp() {
        if (mAuth.getCurrentUser() != null) {
            String currentUserId = mAuth.getCurrentUser().getUid();
            db.collection("users").document(currentUserId)
                    .update("lastReadTimestamp", Timestamp.now())
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Last read timestamp updated successfully");
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to update last read timestamp", e);
                    });
        }
    }

    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar_dashboard);
        setSupportActionBar(toolbar);

        // Data TextViews
        tvTotalMembers = findViewById(R.id.tvTotalMembers);
        tvTotalDeposit = findViewById(R.id.tvTotalDeposit);
        tvTotalCost = findViewById(R.id.tvTotalCost);
        tvRemainingBalance = findViewById(R.id.tvRemainingBalance);
        tvTotalMeals = findViewById(R.id.tvTotalMeals);
        tvMealRate = findViewById(R.id.tvMealRate);
        tvNoticeBoard = findViewById(R.id.tvNoticeBoard);
        tvMenuBanner = findViewById(R.id.tvMenuBanner);
        tvDailyAverageCost = findViewById(R.id.tvDailyAverageCost);
        tvChatBadgeAdmin = findViewById(R.id.tvChatBadgeAdmin);

        // Chart
        barChart = findViewById(R.id.barChart);
    }

    private void initializeFirebase() {
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
    }

    private void setupClickListeners() {
        Button btnAddNotice = findViewById(R.id.btnAddNotice);
        Button btnDownloadPdf = findViewById(R.id.btnDownloadPdf);
        Button btnShareReport = findViewById(R.id.btnShareReport);
        Button btnSetMenu = findViewById(R.id.btnSetMenu);
        FloatingActionButton fabChat = findViewById(R.id.fab_chat);
        CardView cardBazarAnalysis = findViewById(R.id.cardBazarAnalysis);
        CardView cardMealOffRequests = findViewById(R.id.cardMealOffRequests);

        btnAddNotice.setOnClickListener(v -> startActivity(new Intent(this, AddNoticeActivity.class)));
        btnDownloadPdf.setOnClickListener(v -> createPdf(false));
        btnShareReport.setOnClickListener(v -> createPdf(true));
        btnSetMenu.setOnClickListener(v -> startActivity(new Intent(this, SetMenuActivity.class)));

        // Use startActivityForResult for chat
        fabChat.setOnClickListener(v -> {
            Intent intent = new Intent(this, ChatActivity.class);
            startActivityForResult(intent, CHAT_REQUEST_CODE);
        });

        cardBazarAnalysis.setOnClickListener(v -> startActivity(new Intent(this, BazarDashboardActivity.class)));
        cardMealOffRequests.setOnClickListener(v -> startActivity(new Intent(this, AdminMealOffRequestsActivity.class)));

        // Add animation to Bazar Analysis card with error handling
        try {
            Animation upDown = AnimationUtils.loadAnimation(this, R.anim.up_down_animation);
            if (upDown != null) {
                cardBazarAnalysis.startAnimation(upDown);
            }
        } catch (Exception e) {
            Log.w(TAG, "Animation file not found: " + e.getMessage());
        }
    }

    private void registerBroadcastReceiver() {
        try {
            IntentFilter filter = new IntentFilter("clear-admin-badge");
            LocalBroadcastManager.getInstance(this).registerReceiver(badgeClearReceiver, filter);
            Log.d(TAG, "BroadcastReceiver registered");
        } catch (Exception e) {
            Log.e(TAG, "Error registering broadcast receiver", e);
        }
    }

    // Static method for external badge clearing
    public static void clearBadgeFromOutside() {
        if (currentInstance != null && currentInstance.tvChatBadgeAdmin != null) {
            currentInstance.runOnUiThread(() -> {
                currentInstance.tvChatBadgeAdmin.setVisibility(View.GONE);
                Log.d(TAG, "Badge cleared from static method");
            });
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        currentInstance = null; // Clear static reference
        cleanupResources();
    }

    private void cleanupResources() {
        // Remove message listener separately
        removeMessageListener();

        // Remove all other Firestore listeners
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

        // Unregister broadcast receiver
        try {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(badgeClearReceiver);
            Log.d(TAG, "BroadcastReceiver unregistered");
        } catch (IllegalArgumentException e) {
            Log.w(TAG, "Receiver not registered", e);
        } catch (Exception e) {
            Log.e(TAG, "Error unregistering receiver", e);
        }

        Log.d(TAG, "All resources cleaned up");
    }

    private void removeMessageListener() {
        if (currentMessageListener != null) {
            try {
                currentMessageListener.remove();
                currentMessageListener = null;
                Log.d(TAG, "Message listener removed");
            } catch (Exception e) {
                Log.w(TAG, "Error removing message listener", e);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.dashboard_overflow_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.menu_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.nav_dashboard);
            bottomNav.setOnItemSelectedListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.nav_dashboard) return true;
                if (itemId == R.id.nav_members) startActivity(new Intent(this, MembersActivity.class));
                if (itemId == R.id.nav_deposit) startActivity(new Intent(this, DepositActivity.class));
                if (itemId == R.id.nav_cost) startActivity(new Intent(this, CostHistoryActivity.class));
                if (itemId == R.id.nav_meal) startActivity(new Intent(this, MealActivity.class));
                return true;
            });
        }
    }

    private void loadDashboardData() {
        if (mAuth.getCurrentUser() == null) {
            Log.e(TAG, "User not authenticated");
            redirectToLogin();
            return;
        }

        String adminUid = mAuth.getCurrentUser().getUid();

        db.collection("users").document(adminUid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        currentMessId = documentSnapshot.getString("messId");

                        if (TextUtils.isEmpty(currentMessId)) {
                            Log.e(TAG, "MessId not found for admin");
                            Toast.makeText(this, "Mess ID not found", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        setupAllDataListeners();
                    } else {
                        Log.e(TAG, "Admin document does not exist");
                        Toast.makeText(this, "Admin data not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading admin data", e);
                    Toast.makeText(this, "Error loading admin data", Toast.LENGTH_SHORT).show();
                });
    }

    private void redirectToLogin() {
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    private void setupAllDataListeners() {
        setupMembersCountListener();
        setupDepositsListener();
        setupCostsListener();
        setupMealsListener();
        setupLatestNoticeListener();
        setupTodaysMenuListener();
        setupUnreadMessageListener();
    }

    private void setupUnreadMessageListener() {
        if (TextUtils.isEmpty(currentMessId) || mAuth.getCurrentUser() == null) {
            Log.w(TAG, "Cannot setup unread message listener");
            return;
        }

        String currentUserId = mAuth.getCurrentUser().getUid();
        Log.d(TAG, "Setting up unread message listener for user: " + currentUserId);

        // Real-time listener for user document to get updated lastReadTimestamp
        ListenerRegistration userListener = db.collection("users").document(currentUserId)
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

                    Log.d(TAG, "User lastReadTimestamp: " + lastRead.toDate());

                    // Remove previous message listener
                    removeMessageListener();

                    // Setup message listener with updated timestamp
                    setupMessageListener(lastRead, currentUserId);
                });

        listeners.add(userListener);
    }

    private void setupMessageListener(Timestamp lastRead, String currentUserId) {
        Log.d(TAG, "Setting up message listener with timestamp: " + lastRead.toDate());

        currentMessageListener = db.collection("messages")
                .whereEqualTo("messId", currentMessId)
                .whereGreaterThan("timestamp", lastRead)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error listening to messages", error);
                        return;
                    }

                    safeRunOnUiThread(() -> updateUnreadMessageBadge(snapshots, currentUserId));
                });

        // Don't add to listeners list as we manage it separately
    }

    private void refreshUnreadCount() {
        Log.d(TAG, "Refreshing unread count...");

        if (mAuth.getCurrentUser() != null && !TextUtils.isEmpty(currentMessId)) {
            String currentUserId = mAuth.getCurrentUser().getUid();

            db.collection("users").document(currentUserId)
                    .get()
                    .addOnSuccessListener(userDoc -> {
                        if (userDoc.exists()) {
                            Timestamp lastRead = userDoc.getTimestamp("lastReadTimestamp");
                            if (lastRead == null) {
                                lastRead = new Timestamp(0, 0);
                            }

                            Log.d(TAG, "Refreshing with lastRead: " + lastRead.toDate());

                            db.collection("messages")
                                    .whereEqualTo("messId", currentMessId)
                                    .whereGreaterThan("timestamp", lastRead)
                                    .get()
                                    .addOnSuccessListener(snapshots -> {
                                        safeRunOnUiThread(() -> updateUnreadMessageBadge(snapshots, currentUserId));
                                    })
                                    .addOnFailureListener(e -> Log.e(TAG, "Error refreshing unread count", e));
                        }
                    })
                    .addOnFailureListener(e -> Log.e(TAG, "Error fetching user for refresh", e));
        }
    }

    private void updateUnreadMessageBadge(QuerySnapshot snapshots, String currentUserId) {
        if (snapshots == null || snapshots.isEmpty()) {
            tvChatBadgeAdmin.setVisibility(View.GONE);
            Log.d(TAG, "No unread messages, hiding badge");
            return;
        }

        int unreadCount = 0;
        for (QueryDocumentSnapshot doc : snapshots) {
            String senderId = doc.getString("senderId");
            if (!TextUtils.isEmpty(senderId) && !senderId.equals(currentUserId)) {
                unreadCount++;
            }
        }

        Log.d(TAG, "Unread count: " + unreadCount);

        if (unreadCount > 0) {
            tvChatBadgeAdmin.setVisibility(View.VISIBLE);
            tvChatBadgeAdmin.setText(String.valueOf(unreadCount));
            Log.d(TAG, "Showing badge with count: " + unreadCount);
        } else {
            tvChatBadgeAdmin.setVisibility(View.GONE);
            Log.d(TAG, "No unread messages from others, hiding badge");
        }
    }

    private void setupMembersCountListener() {
        ListenerRegistration listener = db.collection("users")
                .whereEqualTo("messId", currentMessId)
                .whereEqualTo("role", "member")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error fetching members count", error);
                        return;
                    }

                    int memberCount = (value != null) ? value.size() : 0;
                    safeRunOnUiThread(() -> animateTextView(0, memberCount, tvTotalMembers));
                });

        listeners.add(listener);
    }

    private void setupDepositsListener() {
        ListenerRegistration listener = db.collection("deposits")
                .whereEqualTo("messId", currentMessId)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error fetching deposits", error);
                        return;
                    }

                    totalDeposit = calculateTotal(value, "amount");
                    safeRunOnUiThread(() -> {
                        animateCurrencyTextView(0, (int) totalDeposit, tvTotalDeposit);
                        updateBalance();
                    });
                });

        listeners.add(listener);
    }

    private void setupCostsListener() {
        ListenerRegistration listener = db.collection("costs")
                .whereEqualTo("messId", currentMessId)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error fetching costs", error);
                        return;
                    }

                    totalCost = calculateTotal(value, "amount");
                    double averageCost = calculateDailyAverageCost(value);

                    safeRunOnUiThread(() -> {
                        animateDecimalCurrencyTextView(0, averageCost, tvDailyAverageCost);
                        animateCurrencyTextView(0, (int) totalCost, tvTotalCost);
                        updateBalance();
                        calculateMealRate();
                    });
                });

        listeners.add(listener);
    }

    private double calculateDailyAverageCost(QuerySnapshot value) {
        if (value == null || value.isEmpty()) return 0.0;

        Set<String> uniqueDays = new HashSet<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        for (QueryDocumentSnapshot doc : value) {
            Timestamp costDate = doc.getTimestamp("costDate");
            if (costDate != null) {
                uniqueDays.add(sdf.format(costDate.toDate()));
            }
        }

        return uniqueDays.isEmpty() ? 0.0 : totalCost / uniqueDays.size();
    }

    private void setupMealsListener() {
        ListenerRegistration listener = db.collection("meals")
                .whereEqualTo("messId", currentMessId)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error fetching meals", error);
                        return;
                    }

                    totalMeals = calculateTotal(value, "totalMeal");
                    safeRunOnUiThread(() -> {
                        animateDecimalTextView(0, totalMeals, tvTotalMeals);
                        calculateMealRate();
                    });
                });

        listeners.add(listener);
    }

    private double calculateTotal(QuerySnapshot snapshots, String fieldName) {
        double total = 0.0;
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

    private void setupLatestNoticeListener() {
        ListenerRegistration listener = db.collection("notices")
                .whereEqualTo("messId", currentMessId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(1)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error fetching notices", error);
                        return;
                    }

                    String noticeText = "No new notices.";
                    if (snapshots != null && !snapshots.isEmpty()) {
                        String latestNotice = snapshots.getDocuments().get(0).getString("noticeText");
                        if (!TextUtils.isEmpty(latestNotice)) {
                            noticeText = latestNotice;
                        }
                    }

                    final String finalNoticeText = noticeText;
                    safeRunOnUiThread(() -> tvNoticeBoard.setText(finalNoticeText));
                });

        listeners.add(listener);
    }

    private void setupTodaysMenuListener() {
        SimpleDateFormat docIdFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String todayDateString = docIdFormat.format(new Date());
        String documentId = todayDateString + "_" + currentMessId;

        ListenerRegistration listener = db.collection("daily_menus")
                .document(documentId)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error fetching menu", error);
                        safeRunOnUiThread(() -> tvMenuBanner.setText("Could not load menu"));
                        return;
                    }

                    String menuText = buildMenuText(snapshot);
                    safeRunOnUiThread(() -> tvMenuBanner.setText(menuText));
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

    // Animation Methods
    private void animateTextView(int initialValue, int finalValue, final TextView textView) {
        ValueAnimator valueAnimator = ValueAnimator.ofInt(initialValue, finalValue);
        valueAnimator.setDuration(1500);
        valueAnimator.addUpdateListener(animator ->
                textView.setText(animator.getAnimatedValue().toString()));
        valueAnimator.start();
    }

    private void animateCurrencyTextView(int initialValue, int finalValue, final TextView textView) {
        ValueAnimator valueAnimator = ValueAnimator.ofInt(initialValue, finalValue);
        valueAnimator.setDuration(1500);
        valueAnimator.addUpdateListener(animator -> {
            String text = String.format(Locale.US, "BDT %,d", animator.getAnimatedValue());
            textView.setText(text);
        });
        valueAnimator.start();
    }

    private void animateDecimalTextView(double initialValue, double finalValue, final TextView textView) {
        ValueAnimator valueAnimator = ValueAnimator.ofFloat((float) initialValue, (float) finalValue);
        valueAnimator.setDuration(1500);
        valueAnimator.addUpdateListener(animator -> {
            String text = String.format(Locale.US, "%.1f", animator.getAnimatedValue());
            textView.setText(text);
        });
        valueAnimator.start();
    }

    private void animateDecimalCurrencyTextView(double initialValue, double finalValue, final TextView textView) {
        ValueAnimator valueAnimator = ValueAnimator.ofFloat((float) initialValue, (float) finalValue);
        valueAnimator.setDuration(1500);
        valueAnimator.addUpdateListener(animator -> {
            String text = String.format(Locale.US, "BDT %.2f", animator.getAnimatedValue());
            textView.setText(text);
        });
        valueAnimator.start();
    }

    private void calculateMealRate() {
        double mealRate = (totalMeals > 0) ? (totalCost / totalMeals) : 0.0;
        animateDecimalCurrencyTextView(0, mealRate, tvMealRate);
    }

    private void updateBalance() {
        double remainingBalance = totalDeposit - totalCost;
        animateCurrencyTextView(0, (int) remainingBalance, tvRemainingBalance);
        setupChart();
    }

    private void setupChart() {
        ArrayList<BarEntry> entries = new ArrayList<>();
        entries.add(new BarEntry(1, (float) totalDeposit));
        entries.add(new BarEntry(2, (float) totalCost));

        BarDataSet barDataSet = new BarDataSet(entries, "Overview (1=Deposit, 2=Cost)");
        barDataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        barDataSet.setValueTextColor(Color.BLACK);
        barDataSet.setValueTextSize(16f);

        BarData barData = new BarData(barDataSet);
        barChart.setData(barData);
        barChart.getDescription().setEnabled(false);
        barChart.animateY(1000);
        barChart.invalidate();
    }

    private void createPdf(boolean forSharing) {
        if (TextUtils.isEmpty(currentMessId)) {
            Toast.makeText(this, "Data is still loading. Please try again.", Toast.LENGTH_SHORT).show();
            return;
        }

        Task<QuerySnapshot> membersTask = db.collection("users")
                .whereEqualTo("messId", currentMessId)
                .whereEqualTo("role", "member")
                .get();

        Task<QuerySnapshot> depositsTask = db.collection("deposits")
                .whereEqualTo("messId", currentMessId)
                .get();

        Task<QuerySnapshot> mealsTask = db.collection("meals")
                .whereEqualTo("messId", currentMessId)
                .get();

        Tasks.whenAllSuccess(membersTask, depositsTask, mealsTask)
                .addOnSuccessListener(list -> {
                    try {
                        QuerySnapshot membersSnapshot = (QuerySnapshot) list.get(0);
                        QuerySnapshot depositsSnapshot = (QuerySnapshot) list.get(1);
                        QuerySnapshot mealsSnapshot = (QuerySnapshot) list.get(2);

                        double mealRate = (totalMeals > 0) ? (totalCost / totalMeals) : 0;

                        Map<String, Map<String, Object>> memberDataMap = processMemberData(
                                membersSnapshot, depositsSnapshot, mealsSnapshot);

                        Uri fileUri = generatePdfWithTable(memberDataMap, mealRate);

                        if (fileUri != null) {
                            if (forSharing) {
                                sharePdf(fileUri);
                            } else {
                                Toast.makeText(this, "PDF Report saved to Downloads folder.", Toast.LENGTH_LONG).show();
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing PDF data", e);
                        Toast.makeText(this, "Error creating PDF report", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching data for PDF", e);
                    Toast.makeText(this, "Error fetching data for PDF", Toast.LENGTH_SHORT).show();
                });
    }

    private Map<String, Map<String, Object>> processMemberData(
            QuerySnapshot membersSnapshot,
            QuerySnapshot depositsSnapshot,
            QuerySnapshot mealsSnapshot) {

        Map<String, Map<String, Object>> memberDataMap = new HashMap<>();

        // Initialize member data
        for (QueryDocumentSnapshot doc : membersSnapshot) {
            Map<String, Object> data = new HashMap<>();
            data.put("name", doc.getString("name"));
            data.put("userId", doc.getString("userId"));
            data.put("totalDeposit", 0.0);
            data.put("totalMeals", 0.0);
            memberDataMap.put(doc.getId(), data);
        }

        // Add deposits data
        for (QueryDocumentSnapshot doc : depositsSnapshot) {
            String memberId = doc.getString("memberId");
            if (memberDataMap.containsKey(memberId)) {
                Double amount = doc.getDouble("amount");
                if (amount != null) {
                    double currentDeposit = (double) memberDataMap.get(memberId).get("totalDeposit");
                    memberDataMap.get(memberId).put("totalDeposit", currentDeposit + amount);
                }
            }
        }

        // Add meals data
        for (QueryDocumentSnapshot doc : mealsSnapshot) {
            String memberId = doc.getString("memberId");
            if (memberDataMap.containsKey(memberId)) {
                Double meals = doc.getDouble("totalMeal");
                if (meals != null) {
                    double currentMeals = (double) memberDataMap.get(memberId).get("totalMeals");
                    memberDataMap.get(memberId).put("totalMeals", currentMeals + meals);
                }
            }
        }

        return memberDataMap;
    }

    private Uri generatePdfWithTable(Map<String, Map<String, Object>> memberDataMap, double mealRate) {
        String fileName = "MessReport_Detailed_" + System.currentTimeMillis() + ".pdf";

        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
        values.put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf");
        values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

        Uri uri = getContentResolver().insert(MediaStore.Files.getContentUri("external"), values);

        if (uri != null) {
            try (OutputStream outputStream = getContentResolver().openOutputStream(uri)) {
                // Create PDF document using Android's built-in PDF
                PdfDocument pdfDocument = new PdfDocument();

                // Create page
                PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
                PdfDocument.Page page = pdfDocument.startPage(pageInfo);

                Canvas canvas = page.getCanvas();
                Paint paint = new Paint();
                paint.setAntiAlias(true);

                // Title
                paint.setTextSize(24);
                paint.setTypeface(Typeface.DEFAULT_BOLD);
                paint.setColor(Color.BLACK);
                canvas.drawText("Mess Report", 200, 60, paint);

                // Meal rate
                paint.setTextSize(16);
                paint.setTypeface(Typeface.DEFAULT);
                canvas.drawText("Overall Meal Rate: BDT " + String.format(Locale.US, "%.2f", mealRate), 150, 100, paint);

                // Draw table
                drawTable(canvas, memberDataMap, mealRate);

                pdfDocument.finishPage(page);
                pdfDocument.writeTo(outputStream);
                pdfDocument.close();

                return uri;
            } catch (Exception e) {
                Log.e(TAG, "Error creating PDF", e);
                Toast.makeText(this, "Error creating PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
        return null;
    }

    private void drawTable(Canvas canvas, Map<String, Map<String, Object>> memberDataMap, double mealRate) {
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setTextSize(12);

        // Table headers
        int startY = 140;
        int rowHeight = 25;

        // Header background
        paint.setColor(Color.LTGRAY);
        canvas.drawRect(30, startY - 20, 550, startY, paint);

        // Header text
        paint.setColor(Color.BLACK);
        paint.setTypeface(Typeface.DEFAULT_BOLD);
        canvas.drawText("Name", 40, startY - 5, paint);
        canvas.drawText("User ID", 140, startY - 5, paint);
        canvas.drawText("Deposit", 220, startY - 5, paint);
        canvas.drawText("Meals", 290, startY - 5, paint);
        canvas.drawText("Cost", 350, startY - 5, paint);
        canvas.drawText("Balance", 420, startY - 5, paint);

        // Table data
        int currentY = startY + rowHeight;
        paint.setTypeface(Typeface.DEFAULT);

        double grandTotalDeposit = 0.0, grandTotalMeals = 0.0,
                grandTotalCost = 0.0, grandTotalBalance = 0.0;

        for (Map.Entry<String, Map<String, Object>> entry : memberDataMap.entrySet()) {
            Map<String, Object> data = entry.getValue();

            String name = (String) data.get("name");
            String userId = (String) data.get("userId");
            double deposit = (double) data.get("totalDeposit");
            double meals = (double) data.get("totalMeals");
            double cost = meals * mealRate;
            double balance = deposit - cost;

            grandTotalDeposit += deposit;
            grandTotalMeals += meals;
            grandTotalCost += cost;
            grandTotalBalance += balance;

            // Draw row
            canvas.drawText(name != null ? (name.length() > 12 ? name.substring(0, 12) : name) : "N/A", 40, currentY, paint);
            canvas.drawText(userId != null ? userId : "N/A", 140, currentY, paint);
            canvas.drawText(String.format(Locale.US, "%.0f", deposit), 220, currentY, paint);
            canvas.drawText(String.format(Locale.US, "%.1f", meals), 290, currentY, paint);
            canvas.drawText(String.format(Locale.US, "%.0f", cost), 350, currentY, paint);
            canvas.drawText(String.format(Locale.US, "%.0f", balance), 420, currentY, paint);

            currentY += rowHeight;

            // Stop if page is full
            if (currentY > 750) break;
        }

        // Grand total
        currentY += 10;
        paint.setTypeface(Typeface.DEFAULT_BOLD);
        paint.setColor(Color.BLUE);
        canvas.drawText("TOTAL:", 40, currentY, paint);
        canvas.drawText(String.format(Locale.US, "%.0f", grandTotalDeposit), 220, currentY, paint);
        canvas.drawText(String.format(Locale.US, "%.1f", grandTotalMeals), 290, currentY, paint);
        canvas.drawText(String.format(Locale.US, "%.0f", grandTotalCost), 350, currentY, paint);
        canvas.drawText(String.format(Locale.US, "%.0f", grandTotalBalance), 420, currentY, paint);
    }

    private void sharePdf(Uri fileUri) {
        try {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("application/pdf");
            shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(shareIntent, "Share Report Via"));
        } catch (Exception e) {
            Log.e(TAG, "Error sharing PDF", e);
            Toast.makeText(this, "Error sharing PDF", Toast.LENGTH_SHORT).show();
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
    protected void onPause() {
        super.onPause();
        // Optional: You can pause some listeners here for performance
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Check SharedPreferences for badge clear flag
        SharedPreferences prefs = getSharedPreferences("badge_state", MODE_PRIVATE);
        if (prefs.getBoolean("clear_badge", false)) {
            if (tvChatBadgeAdmin != null) {
                tvChatBadgeAdmin.setVisibility(View.GONE);
                Log.d(TAG, "Badge cleared from SharedPreferences");
            }
            prefs.edit().putBoolean("clear_badge", false).apply();
        }

        // Optional: Resume listeners if paused
    }
}