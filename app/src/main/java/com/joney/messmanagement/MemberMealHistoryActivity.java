package com.joney.messmanagement;

import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class MemberMealHistoryActivity extends AppCompatActivity {

    private static final String TAG = "MemberMealHistory";

    // UI Components
    private TextView tvMemberNameDetails, tvMonthName;
    private TableLayout tableLayoutMealHistory;
    private Toolbar toolbar;

    // Firebase
    private FirebaseFirestore db;

    // Data
    private String memberDocId;
    private String memberName;
    private String messId;
    private double totalMeals = 0.0;
    private double totalGuestMeals = 0.0;
    private int totalDays = 0;
    private boolean isDataLoaded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            setContentView(R.layout.activity_member_meal_history);
            initializeViews();
            initializeFirebase();
            getMemberData();
            setupCurrentMonth();
            loadMealHistory();
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate", e);
            handleError("Failed to initialize activity", e);
        }
    }

    private void initializeViews() {
        try {
            toolbar = findViewById(R.id.toolbar_meal_details);
            if (toolbar != null) {
                setSupportActionBar(toolbar);

                // Enable back button
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                    getSupportActionBar().setTitle("Meal History");
                }

                // Toolbar back button
                toolbar.setNavigationOnClickListener(v -> {
                    logUserAction("Back button clicked");
                    onBackPressed();
                });
            } else {
                Log.w(TAG, "Toolbar not found in layout");
            }

            tvMemberNameDetails = findViewById(R.id.tvMemberNameDetails);
            tvMonthName = findViewById(R.id.tvMonthName);
            tableLayoutMealHistory = findViewById(R.id.tableLayoutMealHistory);

            // Validate required views
            if (tableLayoutMealHistory == null) {
                throw new RuntimeException("TableLayout not found in layout");
            }

            Log.d(TAG, "Views initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing views", e);
            throw e;
        }
    }

    private void initializeFirebase() {
        try {
            db = FirebaseFirestore.getInstance();
            Log.d(TAG, "Firebase initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing Firebase", e);
            throw e;
        }
    }

    private void getMemberData() {
        try {
            memberDocId = getIntent().getStringExtra("MEMBER_DOC_ID");
            memberName = getIntent().getStringExtra("MEMBER_NAME");
            messId = getIntent().getStringExtra("MESS_ID");

            if (TextUtils.isEmpty(memberDocId)) {
                Log.e(TAG, "Member ID not found in intent");
                handleError("Member ID not found", null);
                return;
            }

            // Set member name if provided
            if (!TextUtils.isEmpty(memberName) && tvMemberNameDetails != null) {
                tvMemberNameDetails.setText(memberName);
                Log.d(TAG, "Member name set from intent: " + memberName);
            } else {
                // Load member name from database
                loadMemberName();
            }

            Log.d(TAG, "Loading data for member: " + memberDocId);
        } catch (Exception e) {
            Log.e(TAG, "Error getting member data", e);
            handleError("Failed to load member data", e);
        }
    }

    private void loadMemberName() {
        if (db == null || TextUtils.isEmpty(memberDocId)) {
            return;
        }

        db.collection("users").document(memberDocId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    try {
                        if (documentSnapshot.exists()) {
                            memberName = documentSnapshot.getString("name");
                            messId = documentSnapshot.getString("messId");

                            if (!TextUtils.isEmpty(memberName) && tvMemberNameDetails != null) {
                                tvMemberNameDetails.setText(memberName);
                                Log.d(TAG, "Member name loaded from database: " + memberName);
                            }
                        } else {
                            Log.w(TAG, "Member document not found");
                            if (tvMemberNameDetails != null) {
                                tvMemberNameDetails.setText("Unknown Member");
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing member document", e);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading member name", e);
                    if (tvMemberNameDetails != null) {
                        tvMemberNameDetails.setText("Error Loading Name");
                    }
                });
    }

    private void setupCurrentMonth() {
        try {
            if (tvMonthName != null) {
                Calendar calendar = Calendar.getInstance();
                SimpleDateFormat monthFormat = new SimpleDateFormat("MMMM, yyyy", Locale.US);
                String currentMonth = monthFormat.format(calendar.getTime());
                tvMonthName.setText("Month: " + currentMonth);
                Log.d(TAG, "Month set: " + currentMonth);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting up current month", e);
        }
    }

    private void loadMealHistory() {
        if (TextUtils.isEmpty(memberDocId) || db == null) {
            Log.e(TAG, "Cannot load meal history: missing data");
            showEmptyTable();
            return;
        }

        try {
            Log.d(TAG, "Starting to load meal history for member: " + memberDocId);
            logUserAction("Loading meal history");

            // Get current month's start and end dates
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.DAY_OF_MONTH, 1);
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            Date monthStart = calendar.getTime();

            calendar.add(Calendar.MONTH, 1);
            calendar.add(Calendar.DAY_OF_MONTH, -1);
            calendar.set(Calendar.HOUR_OF_DAY, 23);
            calendar.set(Calendar.MINUTE, 59);
            calendar.set(Calendar.SECOND, 59);
            Date monthEnd = calendar.getTime();

            Log.d(TAG, "Querying meals from " + monthStart + " to " + monthEnd);

            // Query meals for current month
            db.collection("meals")
                    .whereEqualTo("memberId", memberDocId)
                    .whereGreaterThanOrEqualTo("date", new Timestamp(monthStart))
                    .whereLessThanOrEqualTo("date", new Timestamp(monthEnd))
                    .orderBy("date", Query.Direction.ASCENDING)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        try {
                            Log.d(TAG, "Query successful. Document count: " + queryDocumentSnapshots.size());

                            if (queryDocumentSnapshots.isEmpty()) {
                                Log.w(TAG, "No meal documents found for member: " + memberDocId);
                                tryAlternativeQueries(monthStart, monthEnd);
                                return;
                            }

                            processMealDocuments(queryDocumentSnapshots);
                            isDataLoaded = true;
                        } catch (Exception e) {
                            Log.e(TAG, "Error processing query results", e);
                            handleError("Error processing meal data", e);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error loading meal history", e);
                        handleError("Failed to load meal history", e);
                    });

        } catch (Exception e) {
            Log.e(TAG, "Error in loadMealHistory", e);
            handleError("Error loading meal history", e);
        }
    }

    private void tryAlternativeQueries(Date monthStart, Date monthEnd) {
        Log.d(TAG, "Trying alternative queries...");

        // Try with different field names that might be used
        String[] possibleFields = {"memberDocId", "userId", "member_id", "user_id"};
        boolean foundData = false;

        for (String field : possibleFields) {
            if (foundData) break;

            Log.d(TAG, "Trying field: " + field);

            db.collection("meals")
                    .whereEqualTo(field, memberDocId)
                    .whereGreaterThanOrEqualTo("date", new Timestamp(monthStart))
                    .whereLessThanOrEqualTo("date", new Timestamp(monthEnd))
                    .orderBy("date", Query.Direction.ASCENDING)
                    .limit(1)
                    .get()
                    .addOnSuccessListener(snapshots -> {
                        if (!snapshots.isEmpty()) {
                            Log.d(TAG, "Found data with field: " + field);
                            loadMealHistoryWithField(field, monthStart, monthEnd);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.w(TAG, "Failed with field " + field + ": " + e.getMessage());
                    });
        }

        // If no alternative works after a delay, show empty table
        new android.os.Handler().postDelayed(() -> {
            if (!isDataLoaded) {
                showEmptyTable();
            }
        }, 3000); // Wait 3 seconds for alternative queries
    }

    private void loadMealHistoryWithField(String fieldName, Date monthStart, Date monthEnd) {
        db.collection("meals")
                .whereEqualTo(fieldName, memberDocId)
                .whereGreaterThanOrEqualTo("date", new Timestamp(monthStart))
                .whereLessThanOrEqualTo("date", new Timestamp(monthEnd))
                .orderBy("date", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    processMealDocuments(queryDocumentSnapshots);
                    isDataLoaded = true;
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error with field " + fieldName, e);
                    showEmptyTable();
                });
    }

    private void processMealDocuments(com.google.firebase.firestore.QuerySnapshot queryDocumentSnapshots) {
        try {
            // Clear existing table
            if (tableLayoutMealHistory != null) {
                tableLayoutMealHistory.removeAllViews();
            }

            // Create header row
            createTableHeader();

            totalMeals = 0.0;
            totalGuestMeals = 0.0;
            totalDays = 0;

            Log.d(TAG, "Processing " + queryDocumentSnapshots.size() + " meal documents");

            SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM", Locale.US);

            if (queryDocumentSnapshots.isEmpty()) {
                createEmptyTableRow();
            } else {
                for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                    try {
                        Log.d(TAG, "Processing document: " + document.getId());

                        // Get date
                        Timestamp timestamp = document.getTimestamp("date");
                        String dateStr = timestamp != null ? dateFormat.format(timestamp.toDate()) : "N/A";

                        // Get meal data with multiple possible field names
                        double breakfast = getDoubleValueWithAlternatives(document,
                                new String[]{"breakfast", "breakfastMeal", "morning"});
                        double lunch = getDoubleValueWithAlternatives(document,
                                new String[]{"lunch", "lunchMeal", "noon"});
                        double dinner = getDoubleValueWithAlternatives(document,
                                new String[]{"dinner", "dinnerMeal", "evening"});

                        // Get guest meal data
                        double guestBreakfast = getDoubleValueWithAlternatives(document,
                                new String[]{"guestBreakfast", "guest_breakfast", "guestMorning"});
                        double guestLunch = getDoubleValueWithAlternatives(document,
                                new String[]{"guestLunch", "guest_lunch", "guestNoon"});
                        double guestDinner = getDoubleValueWithAlternatives(document,
                                new String[]{"guestDinner", "guest_dinner", "guestEvening"});

                        // Validate meal data
                        if (isValidMealData(breakfast, lunch, dinner, guestBreakfast, guestLunch, guestDinner)) {
                            // Calculate totals
                            double totalMeal = breakfast + lunch + dinner;
                            double totalGuestMeal = guestBreakfast + guestLunch + guestDinner;
                            double grandTotal = totalMeal + totalGuestMeal;

                            // Update summary
                            totalMeals += totalMeal;
                            totalGuestMeals += totalGuestMeal;
                            totalDays++;

                            // Create table row
                            createTableRow(dateStr, breakfast, lunch, dinner,
                                    guestBreakfast, guestLunch, guestDinner, grandTotal);

                            Log.d(TAG, "Added meal item: B=" + breakfast +
                                    ", L=" + lunch + ", D=" + dinner + ", Total=" + grandTotal);
                        } else {
                            Log.d(TAG, "Skipping document with no valid meal data: " + document.getId());
                        }

                    } catch (Exception e) {
                        Log.e(TAG, "Error processing meal document: " + document.getId(), e);
                    }
                }
            }

            // Add summary row
            createSummaryRow();

            Log.d(TAG, "Processed " + totalDays + " meal items");
            Log.d(TAG, "Total meals: " + totalMeals + ", Guest meals: " + totalGuestMeals + ", Days: " + totalDays);

            logUserAction("Meal history loaded successfully - " + totalDays + " days");

        } catch (Exception e) {
            Log.e(TAG, "Error in processMealDocuments", e);
            handleError("Error processing meal data", e);
        }
    }

    private void createTableHeader() {
        if (tableLayoutMealHistory == null) return;

        TableRow headerRow = new TableRow(this);
        headerRow.setBackgroundColor(ContextCompat.getColor(this, android.R.color.darker_gray));

        String[] headers = {"Date", "B", "L", "D", "GB", "GL", "GD", "Total"};

        for (String header : headers) {
            TextView textView = createTableCell(header, true);
            textView.setTextColor(ContextCompat.getColor(this, android.R.color.white));
            headerRow.addView(textView);
        }

        tableLayoutMealHistory.addView(headerRow);
    }

    private void createTableRow(String date, double breakfast, double lunch, double dinner,
                                double guestBreakfast, double guestLunch, double guestDinner, double total) {
        if (tableLayoutMealHistory == null) return;

        TableRow row = new TableRow(this);

        // Alternate row colors for better readability
        if (totalDays % 2 == 0) {
            row.setBackgroundColor(ContextCompat.getColor(this, android.R.color.white));
        } else {
            row.setBackgroundColor(Color.parseColor("#F8F9FA"));
        }

        // Add border effect
        row.setPadding(1, 1, 1, 1);

        row.addView(createTableCell(date, false));
        row.addView(createTableCell(formatMealValue(breakfast), false));
        row.addView(createTableCell(formatMealValue(lunch), false));
        row.addView(createTableCell(formatMealValue(dinner), false));
        row.addView(createTableCell(formatMealValue(guestBreakfast), false));
        row.addView(createTableCell(formatMealValue(guestLunch), false));
        row.addView(createTableCell(formatMealValue(guestDinner), false));

        TextView totalCell = createTableCell(formatMealValue(total), false);
        totalCell.setTextColor(ContextCompat.getColor(this, android.R.color.holo_blue_dark));
        totalCell.setTypeface(null, android.graphics.Typeface.BOLD);
        row.addView(totalCell);

        // Add click listener for row details
        row.setOnClickListener(v -> showMealDetails(date, breakfast, lunch, dinner,
                guestBreakfast, guestLunch, guestDinner, total));

        tableLayoutMealHistory.addView(row);
    }

    private void createEmptyTableRow() {
        if (tableLayoutMealHistory == null) return;

        TableRow row = new TableRow(this);
        TextView emptyCell = new TextView(this);
        emptyCell.setText("No meal data found for this month");
        emptyCell.setPadding(16, 16, 16, 16);
        emptyCell.setGravity(android.view.Gravity.CENTER);
        emptyCell.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray));
        emptyCell.setTypeface(null, android.graphics.Typeface.ITALIC);

        TableRow.LayoutParams params = new TableRow.LayoutParams();
        params.span = 8; // Span across all columns
        emptyCell.setLayoutParams(params);

        row.addView(emptyCell);
        tableLayoutMealHistory.addView(row);
    }

    private void createSummaryRow() {
        if (tableLayoutMealHistory == null) return;

        TableRow summaryRow = new TableRow(this);
        summaryRow.setBackgroundColor(Color.parseColor("#E3F2FD")); // Light blue background

        // Summary data
        TextView summaryLabel = createTableCell("TOTAL", true);
        summaryLabel.setTextColor(Color.BLACK);
        summaryLabel.setTypeface(null, android.graphics.Typeface.BOLD);

        TextView totalMealsCell = createTableCell(String.format(Locale.US, "%.1f", totalMeals), true);
        totalMealsCell.setTextColor(Color.parseColor("#2196F3")); // Blue

        TextView totalGuestCell = createTableCell(String.format(Locale.US, "%.1f", totalGuestMeals), true);
        totalGuestCell.setTextColor(Color.parseColor("#9C27B0")); // Purple

        TextView grandTotalCell = createTableCell(String.format(Locale.US, "%.1f", totalMeals + totalGuestMeals), true);
        grandTotalCell.setTextColor(Color.parseColor("#F44336")); // Red
        grandTotalCell.setTypeface(null, android.graphics.Typeface.BOLD);

        summaryRow.addView(summaryLabel);
        summaryRow.addView(createTableCell("", true)); // Empty cell for B
        summaryRow.addView(createTableCell("", true)); // Empty cell for L
        summaryRow.addView(createTableCell("", true)); // Empty cell for D
        summaryRow.addView(totalMealsCell); // Regular meals total
        summaryRow.addView(totalGuestCell); // Guest meals total
        summaryRow.addView(createTableCell("", true)); // Empty cell
        summaryRow.addView(grandTotalCell); // Grand total

        tableLayoutMealHistory.addView(summaryRow);
    }

    private TextView createTableCell(String text, boolean isHeader) {
        TextView textView = new TextView(this);
        textView.setText(text);
        textView.setPadding(12, 8, 12, 8);
        textView.setGravity(android.view.Gravity.CENTER);

        if (isHeader) {
            textView.setTypeface(null, android.graphics.Typeface.BOLD);
            textView.setTextSize(12);
        } else {
            textView.setTextSize(11);
            textView.setTextColor(ContextCompat.getColor(this, android.R.color.black));
        }

        // Add border
        textView.setBackgroundResource(android.R.drawable.editbox_background);

        TableRow.LayoutParams params = new TableRow.LayoutParams(
                TableRow.LayoutParams.WRAP_CONTENT,
                TableRow.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(1, 1, 1, 1);
        textView.setLayoutParams(params);

        return textView;
    }

    private String formatMealValue(double value) {
        if (value == 0.0) {
            return "-";
        }
        return String.format(Locale.US, "%.1f", value);
    }

    private double getDoubleValueWithAlternatives(QueryDocumentSnapshot document, String[] fieldNames) {
        for (String fieldName : fieldNames) {
            if (document.contains(fieldName)) {
                Double value = document.getDouble(fieldName);
                if (value != null) {
                    return value;
                }
            }
        }
        return 0.0;
    }

    private void showEmptyTable() {
        safeRunOnUiThread(() -> {
            if (tableLayoutMealHistory != null) {
                // Clear existing table
                tableLayoutMealHistory.removeAllViews();

                // Create header row
                createTableHeader();

                // Create empty row
                createEmptyTableRow();
            }
        });
    }

    private void showMealDetails(String date, double breakfast, double lunch, double dinner,
                                 double guestBreakfast, double guestLunch, double guestDinner, double total) {
        logUserAction("Viewing meal details for " + date);

        String details = String.format(Locale.US,
                "Date: %s\n\n" +
                        "Regular Meals:\n" +
                        "• Breakfast: %s\n" +
                        "• Lunch: %s\n" +
                        "• Dinner: %s\n" +
                        "Subtotal: %.1f\n\n" +
                        "Guest Meals:\n" +
                        "• Breakfast: %s\n" +
                        "• Lunch: %s\n" +
                        "• Dinner: %s\n" +
                        "Subtotal: %.1f\n\n" +
                        "Grand Total: %.1f meals",
                date,
                formatMealValue(breakfast), formatMealValue(lunch), formatMealValue(dinner),
                breakfast + lunch + dinner,
                formatMealValue(guestBreakfast), formatMealValue(guestLunch), formatMealValue(guestDinner),
                guestBreakfast + guestLunch + guestDinner,
                total);

        new AlertDialog.Builder(this)
                .setTitle("Meal Details")
                .setMessage(details)
                .setPositiveButton("OK", null)
                .setIcon(android.R.drawable.ic_menu_info_details)
                .show();
    }

    // Menu methods
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.meal_history_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (id == R.id.action_statistics) {
            showMealStatistics();
            return true;
        } else if (id == R.id.action_refresh) {
            refreshData();
            return true;
        } else if (id == R.id.action_export) {
            exportTableData();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Activity destroyed");
    }

    // Helper method to safely run UI operations
    private void safeRunOnUiThread(Runnable action) {
        if (!isFinishing() && !isDestroyed()) {
            runOnUiThread(action);
        }
    }

    // Method to refresh data
    public void refreshData() {
        logUserAction("Refreshing meal history data");
        totalMeals = 0.0;
        totalGuestMeals = 0.0;
        totalDays = 0;
        isDataLoaded = false;

        if (tableLayoutMealHistory != null) {
            tableLayoutMealHistory.removeAllViews();
        }

        Toast.makeText(this, "Refreshing data...", Toast.LENGTH_SHORT).show();
        loadMealHistory();
    }

    // Method to export table data
    private void exportTableData() {
        logUserAction("Export table data requested");

        if (totalDays == 0) {
            Toast.makeText(this, "No data to export", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create export data
        StringBuilder exportData = new StringBuilder();
        exportData.append("Meal History Report\n");
        exportData.append("Member: ").append(memberName != null ? memberName : "Unknown").append("\n");
        exportData.append("Month: ").append(tvMonthName != null ? tvMonthName.getText() : "Unknown").append("\n\n");
        exportData.append("Summary:\n");
        exportData.append("Total Days: ").append(totalDays).append("\n");
        exportData.append("Total Regular Meals: ").append(String.format(Locale.US, "%.1f", totalMeals)).append("\n");
        exportData.append("Total Guest Meals: ").append(String.format(Locale.US, "%.1f", totalGuestMeals)).append("\n");
        exportData.append("Grand Total: ").append(String.format(Locale.US, "%.1f", totalMeals + totalGuestMeals)).append("\n");

        // Show export options
        new AlertDialog.Builder(this)
                .setTitle("Export Data")
                .setMessage("Export functionality will be available in future updates.\n\nSummary:\n" + exportData.toString())
                .setPositiveButton("OK", null)
                .show();
    }

    // Method to show meal statistics
    private void showMealStatistics() {
        logUserAction("Viewing meal statistics");

        if (totalDays > 0) {
            double avgMealsPerDay = totalMeals / totalDays;
            double avgGuestMealsPerDay = totalGuestMeals / totalDays;
            double avgTotalPerDay = (totalMeals + totalGuestMeals) / totalDays;

            String stats = String.format(Locale.US,
                    "📊 Monthly Statistics\n\n" +
                            "📅 Total Days: %d\n" +
                            "🍽️ Total Regular Meals: %.1f\n" +
                            "👥 Total Guest Meals: %.1f\n" +
                            "📈 Grand Total: %.1f\n\n" +
                            "📊 Daily Averages:\n" +
                            "• Regular Meals: %.2f/day\n" +
                            "• Guest Meals: %.2f/day\n" +
                            "• Total Meals: %.2f/day\n\n" +
                            "📋 Meal Distribution:\n" +
                            "• Regular: %.1f%%\n" +
                            "• Guest: %.1f%%",
                    totalDays, totalMeals, totalGuestMeals, totalMeals + totalGuestMeals,
                    avgMealsPerDay, avgGuestMealsPerDay, avgTotalPerDay,
                    (totalMeals / (totalMeals + totalGuestMeals)) * 100,
                    (totalGuestMeals / (totalMeals + totalGuestMeals)) * 100);

            new AlertDialog.Builder(this)
                    .setTitle("Meal Statistics")
                    .setMessage(stats)
                    .setPositiveButton("OK", null)
                    .setIcon(android.R.drawable.ic_menu_info_details)
                    .show();
        } else {
            Toast.makeText(this, "No data available for statistics", Toast.LENGTH_SHORT).show();
        }
    }

    // Method to validate meal data
    private boolean isValidMealData(double breakfast, double lunch, double dinner,
                                    double guestBreakfast, double guestLunch, double guestDinner) {
        // Check if at least one meal value is greater than 0
        return (breakfast > 0 || lunch > 0 || dinner > 0 ||
                guestBreakfast > 0 || guestLunch > 0 || guestDinner > 0);
    }

    // Method to handle errors
    private void handleError(String message, Exception e) {
        Log.e(TAG, message, e);
        safeRunOnUiThread(() -> {
            String errorMsg = message;
            if (e != null && e.getMessage() != null) {
                errorMsg += ": " + e.getMessage();
            }
            Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();

            // Show empty table on error
            showEmptyTable();
        });
    }

    // Method to log user actions
    private void logUserAction(String action) {
        String memberInfo = !TextUtils.isEmpty(memberName) ? memberName : "Unknown Member";
        Log.d(TAG, "User action: " + action + " by member: " + memberInfo + " (ID: " + memberDocId + ")");
    }

    // Method to check data validity
    private boolean validateData() {
        if (TextUtils.isEmpty(memberDocId)) {
            Log.e(TAG, "Member ID is null or empty");
            return false;
        }
        return true;
    }

    // Method to format currency (if needed for cost calculations)
    private String formatCurrency(double amount) {
        return String.format(Locale.US, "BDT %.2f", amount);
    }

    // Method to handle different date formats
    private Timestamp parseDate(QueryDocumentSnapshot document) {
        // Try different date field names
        String[] dateFields = {"date", "mealDate", "timestamp", "createdAt"};

        for (String field : dateFields) {
            if (document.contains(field)) {
                Timestamp timestamp = document.getTimestamp(field);
                if (timestamp != null) {
                    return timestamp;
                }
            }
        }

        return null;
    }

    // Method to handle network errors
    private void handleNetworkError(Exception e) {
        Log.e(TAG, "Network error", e);
        safeRunOnUiThread(() -> {
            String errorMessage = "Network error. Please check your connection.";
            if (e.getMessage() != null && e.getMessage().contains("offline")) {
                errorMessage = "You are offline. Please check your internet connection.";
            }
            Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
        });
    }

    // Method to filter data by date range
    private void filterDataByDateRange(Date startDate, Date endDate) {
        logUserAction("Filtering data by date range");

        if (TextUtils.isEmpty(memberDocId) || db == null) {
            Toast.makeText(this, "Cannot filter: missing data", Toast.LENGTH_SHORT).show();
            return;
        }

        // Clear existing data
        totalMeals = 0.0;
        totalGuestMeals = 0.0;
        totalDays = 0;

        if (tableLayoutMealHistory != null) {
            tableLayoutMealHistory.removeAllViews();
        }

        // Query meals for custom date range
        db.collection("meals")
                .whereEqualTo("memberId", memberDocId)
                .whereGreaterThanOrEqualTo("date", new Timestamp(startDate))
                .whereLessThanOrEqualTo("date", new Timestamp(endDate))
                .orderBy("date", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(this::processMealDocuments)
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error filtering data", e);
                    handleError("Failed to filter data", e);
                });
    }

    // Method to search meals by specific criteria
    private void searchMeals(String searchQuery) {
        logUserAction("Searching meals: " + searchQuery);
        // Implementation for searching specific meal records
        Toast.makeText(this, "Search feature coming soon", Toast.LENGTH_SHORT).show();
    }

    // Method to handle orientation changes
    @Override
    public void onConfigurationChanged(android.content.res.Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Log.d(TAG, "Configuration changed");
        // The table should automatically adjust due to HorizontalScrollView
    }

    // Method to save table state
    private void saveTableState() {
        try {
            android.content.SharedPreferences prefs = getSharedPreferences("MealHistoryState", MODE_PRIVATE);
            android.content.SharedPreferences.Editor editor = prefs.edit();

            editor.putString("memberDocId", memberDocId);
            editor.putString("memberName", memberName);
            editor.putFloat("totalMeals", (float) totalMeals);
            editor.putFloat("totalGuestMeals", (float) totalGuestMeals);
            editor.putInt("totalDays", totalDays);
            editor.putBoolean("isDataLoaded", isDataLoaded);
            editor.putLong("lastUpdated", System.currentTimeMillis());

            editor.apply();
            Log.d(TAG, "Table state saved");
        } catch (Exception e) {
            Log.e(TAG, "Error saving table state", e);
        }
    }

    // Method to restore table state
    private void restoreTableState() {
        try {
            android.content.SharedPreferences prefs = getSharedPreferences("MealHistoryState", MODE_PRIVATE);

            if (prefs.contains("memberDocId")) {
                String savedMemberId = prefs.getString("memberDocId", "");

                // Only restore if it's the same member
                if (savedMemberId.equals(memberDocId)) {
                    totalMeals = prefs.getFloat("totalMeals", 0.0f);
                    totalGuestMeals = prefs.getFloat("totalGuestMeals", 0.0f);
                    totalDays = prefs.getInt("totalDays", 0);
                    isDataLoaded = prefs.getBoolean("isDataLoaded", false);

                    long lastUpdated = prefs.getLong("lastUpdated", 0);
                    long timeDiff = System.currentTimeMillis() - lastUpdated;

                    // If data is older than 5 minutes, refresh it
                    if (timeDiff > 300000) {
                        refreshData();
                    }

                    Log.d(TAG, "Table state restored");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error restoring table state", e);
        }
    }

    // Method to clear saved state
    private void clearSavedState() {
        try {
            android.content.SharedPreferences prefs = getSharedPreferences("MealHistoryState", MODE_PRIVATE);
            prefs.edit().clear().apply();
            Log.d(TAG, "Saved state cleared");
        } catch (Exception e) {
            Log.e(TAG, "Error clearing saved state", e);
        }
    }

    // Method to show loading indicator
    private void showLoadingIndicator(boolean show) {
        // You can implement a progress bar here if needed
        if (show) {
            Log.d(TAG, "Loading data...");
        } else {
            Log.d(TAG, "Loading completed");
        }
    }

    // Method to validate Firebase connection
    private void validateFirebaseConnection() {
        if (db != null) {
            db.collection("test")
                    .limit(1)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        Log.d(TAG, "Firebase connection validated");
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Firebase connection failed", e);
                        handleNetworkError(e);
                    });
        }
    }

    // Method to check app permissions
    private void checkPermissions() {
        // Check if app has necessary permissions
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            if (checkSelfPermission(android.Manifest.permission.INTERNET) !=
                    android.content.pm.PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "Internet permission not granted");
            }
        }
    }

    // Lifecycle methods
    @Override
    protected void onResume() {
        super.onResume();
        logUserAction("Activity resumed");

        // Restore table state if available
        restoreTableState();

        // Validate Firebase connection
        validateFirebaseConnection();
    }

    @Override
    protected void onPause() {
        super.onPause();
        logUserAction("Activity paused");

        // Save current state
        saveTableState();
    }

    @Override
    protected void onStop() {
        super.onStop();
        logUserAction("Activity stopped");
    }

    @Override
    protected void onStart() {
        super.onStart();
        logUserAction("Activity started");

        // Check permissions
        checkPermissions();
    }

    // Method to handle back press with confirmation
    @Override
    public void onBackPressed() {
        logUserAction("Back button pressed");

        // If data is being loaded, show confirmation
        if (!isDataLoaded) {
            new AlertDialog.Builder(this)
                    .setTitle("Loading Data")
                    .setMessage("Data is still loading. Are you sure you want to go back?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        logUserAction("Back confirmed during loading");
                        super.onBackPressed();
                    })
                    .setNegativeButton("No", null)
                    .show();
        } else {
            super.onBackPressed();
        }
    }

    // Method to show help dialog
    private void showHelpDialog() {
        String helpText = "📱 Meal History Help\n\n" +
                "📊 Table Columns:\n" +
                "• Date: Meal date\n" +
                "• B, L, D: Breakfast, Lunch, Dinner\n" +
                "• GB, GL, GD: Guest meals\n" +
                "• Total: Daily total meals\n\n" +
                "🔧 Features:\n" +
                "• Tap any row for details\n" +
                "• Use menu for statistics\n" +
                "• Pull to refresh data\n" +
                "• Export functionality coming soon\n\n" +
                "❓ Need help? Contact admin.";

        new AlertDialog.Builder(this)
                .setTitle("Help")
                .setMessage(helpText)
                .setPositiveButton("OK", null)
                .setIcon(android.R.drawable.ic_menu_help)
                .show();
    }

    // Method to show about dialog
    private void showAboutDialog() {
        String aboutText = "📱 Mess Management System\n" +
                "Version 1.0\n\n" +
                "👨‍💻 Developed by: Your Team\n" +
                "📧 Support: support@example.com\n\n" +
                "🔧 Features:\n" +
                "• Meal tracking\n" +
                "• History viewing\n" +
                "• Statistics\n" +
                "• Real-time updates";

        new AlertDialog.Builder(this)
                .setTitle("About")
                .setMessage(aboutText)
                .setPositiveButton("OK", null)
                .setIcon(android.R.drawable.ic_menu_info_details)
                .show();
    }

    // Final cleanup method
    private void finalCleanup() {
        try {
            // Clear all references
            memberDocId = null;
            memberName = null;
            messId = null;

            // Clear UI references
            tvMemberNameDetails = null;
            tvMonthName = null;
            tableLayoutMealHistory = null;
            toolbar = null;

            // Clear Firebase reference
            db = null;

            Log.d(TAG, "Final cleanup completed");
        } catch (Exception e) {
            Log.e(TAG, "Error in final cleanup", e);
        }
    }

    @Override
    protected void finalize() throws Throwable {
        finalCleanup();
        super.finalize();
    }
}