package com.joney.messmanagement;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MemberMealDetailsActivity extends AppCompatActivity {

    private TextView tvMemberNameDetails, tvMonthName;
    private TableLayout tableLayoutMealHistory;
    private FirebaseFirestore db;
    private String memberDocId;
    private String memberName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_member_meal_details);

        db = FirebaseFirestore.getInstance();
        memberDocId = getIntent().getStringExtra("MEMBER_DOC_ID");
        memberName = getIntent().getStringExtra("MEMBER_NAME");

        tvMemberNameDetails = findViewById(R.id.tvMemberNameDetails);
        tvMonthName = findViewById(R.id.tvMonthName);
        tableLayoutMealHistory = findViewById(R.id.tableLayoutMealHistory);

        tvMemberNameDetails.setText(memberName);

        if (memberDocId == null) {
            Toast.makeText(this, "Error: Member ID not found.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadMealDetailsForCurrentMonth();
    }

    private void loadMealDetailsForCurrentMonth() {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat monthFormat = new SimpleDateFormat("MMMM, yyyy", Locale.getDefault());
        tvMonthName.setText("Month: " + monthFormat.format(calendar.getTime()));

        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        long startOfMonth = calendar.getTimeInMillis();

        calendar.add(Calendar.MONTH, 1);
        long startOfNextMonth = calendar.getTimeInMillis();

        db.collection("meals")
                .whereEqualTo("memberId", memberDocId)
                .whereGreaterThanOrEqualTo("date", new com.google.firebase.Timestamp(startOfMonth / 1000, 0))
                .whereLessThan("date", new com.google.firebase.Timestamp(startOfNextMonth / 1000, 0))
                .orderBy("date", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        Toast.makeText(this, "No meal data found for this month.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    processAndDisplayTable(queryDocumentSnapshots);
                });
    }

    private void processAndDisplayTable(Iterable<QueryDocumentSnapshot> documents) {
        Calendar calendar = Calendar.getInstance();
        int daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);

        Map<Integer, Map<String, Double>> mealDataMap = new HashMap<>();
        for (QueryDocumentSnapshot doc : documents) {
            calendar.setTime(doc.getTimestamp("date").toDate());
            int day = calendar.get(Calendar.DAY_OF_MONTH);
            Map<String, Double> dailyMeals = new HashMap<>();
            dailyMeals.put("breakfast", doc.getDouble("breakfast"));
            dailyMeals.put("lunch", doc.getDouble("lunch"));
            dailyMeals.put("dinner", doc.getDouble("dinner"));
            dailyMeals.put("guestbreakfast", doc.getDouble("guestBreakfast") != null ? doc.getDouble("guestBreakfast") : 0);
            dailyMeals.put("guestlunch", doc.getDouble("guestLunch") != null ? doc.getDouble("guestLunch") : 0);
            dailyMeals.put("guestdinner", doc.getDouble("guestDinner") != null ? doc.getDouble("guestDinner") : 0);
            mealDataMap.put(day, dailyMeals);
        }

        createTable(daysInMonth, mealDataMap);
    }

    private void createTable(int daysInMonth, Map<Integer, Map<String, Double>> mealDataMap) {
        tableLayoutMealHistory.removeAllViews();

        TableRow headerRow = new TableRow(this);
        headerRow.addView(createTableCell("Name/Date", true));
        for (int i = 1; i <= daysInMonth; i++) {
            headerRow.addView(createTableCell(String.format(Locale.US, "%02d", i), true));
        }
        headerRow.addView(createTableCell("Total", true));
        tableLayoutMealHistory.addView(headerRow);

        String[] mealTypes = {"Breakfast", "Lunch", "Dinner", "Guest Breakfast", "Guest Lunch", "Guest Dinner"};
        double grandTotal = 0;

        for (String mealType : mealTypes) {
            TableRow dataRow = new TableRow(this);
            dataRow.addView(createTableCell(mealType, true));
            double rowTotal = 0;
            for (int i = 1; i <= daysInMonth; i++) {
                if (mealDataMap.containsKey(i)) {
                    String fieldName = mealType.toLowerCase().replace(" ", "");
                    Double mealCount = mealDataMap.get(i).get(fieldName);
                    if (mealCount == null) mealCount = 0.0;
                    dataRow.addView(createTableCell(String.valueOf(mealCount), false));
                    rowTotal += mealCount;
                } else {
                    dataRow.addView(createTableCell("0.0", false));
                }
            }
            dataRow.addView(createTableCell(String.valueOf(rowTotal), true));
            tableLayoutMealHistory.addView(dataRow);
            grandTotal += rowTotal;
        }

        TableRow totalRow = new TableRow(this);
        TextView totalLabel = createTableCell("Grand Total Meal", true);
        totalLabel.setGravity(Gravity.END);
        TableRow.LayoutParams params = new TableRow.LayoutParams();
        params.span = daysInMonth + 1; // সব তারিখের কলাম জুড়ে থাকবে
        totalRow.addView(totalLabel, params);
        totalRow.addView(createTableCell(String.valueOf(grandTotal), true));
        tableLayoutMealHistory.addView(totalRow);
    }

    private TextView createTableCell(String text, boolean isHeader) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setPadding(24, 16, 24, 16);
        tv.setGravity(Gravity.CENTER);
        tv.setBackgroundResource(R.drawable.table_cell_border);
        if (isHeader) {
            tv.setTypeface(null, Typeface.BOLD);
            tv.setTextColor(Color.BLACK);
        }
        return tv;
    }
}