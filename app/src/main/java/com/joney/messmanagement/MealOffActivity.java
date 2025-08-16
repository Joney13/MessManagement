package com.joney.messmanagement;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MealOffActivity extends AppCompatActivity {

    private Button btnStartDate, btnEndDate, btnSubmitMealOff;
    private TextView tvSelectedDateRange;
    private RecyclerView rvMealOffHistory;
    private MealOffAdapter adapter;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String memberDocId, messId, memberName;

    private Calendar startDate = Calendar.getInstance();
    private Calendar endDate = Calendar.getInstance();
    private boolean isStartDateSet = false;
    private boolean isEndDateSet = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_meal_off);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        memberDocId = getIntent().getStringExtra("MEMBER_DOC_ID");

        btnStartDate = findViewById(R.id.btnStartDate);
        btnEndDate = findViewById(R.id.btnEndDate);
        btnSubmitMealOff = findViewById(R.id.btnSubmitMealOff);
        tvSelectedDateRange = findViewById(R.id.tvSelectedDateRange);
        rvMealOffHistory = findViewById(R.id.rvMealOffHistory);

        btnStartDate.setOnClickListener(v -> showDatePicker(true));
        btnEndDate.setOnClickListener(v -> showDatePicker(false));
        btnSubmitMealOff.setOnClickListener(v -> submitRequest());

        fetchMemberDetailsAndSetupRecyclerView();
    }

    private void fetchMemberDetailsAndSetupRecyclerView() {
        db.collection("users").document(memberDocId).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                messId = doc.getString("messId");
                memberName = doc.getString("name");
                setupRecyclerView();
            }
        });
    }

    private void setupRecyclerView() {
        Query query = db.collection("meal_off_requests")
                .whereEqualTo("memberId", memberDocId)
                .orderBy("startDate", Query.Direction.DESCENDING);

        FirestoreRecyclerOptions<MealOffRequest> options = new FirestoreRecyclerOptions.Builder<MealOffRequest>()
                .setQuery(query, MealOffRequest.class)
                .setLifecycleOwner(this)
                .build();

        adapter = new MealOffAdapter(options);
        rvMealOffHistory.setLayoutManager(new LinearLayoutManager(this));
        rvMealOffHistory.setAdapter(adapter);

        adapter.setOnCancelClickListener(documentSnapshot -> {
            new AlertDialog.Builder(this)
                    .setTitle("Cancel Request")
                    .setMessage("Are you sure you want to cancel this meal off request?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        documentSnapshot.getReference().update("status", "Cancelled");
                    })
                    .setNegativeButton("No", null)
                    .show();
        });
    }

    private void showDatePicker(boolean isStart) {
        Calendar calendar = isStart ? startDate : endDate;
        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, year, month, dayOfMonth) -> {
                    calendar.set(year, month, dayOfMonth);
                    if (isStart) {
                        isStartDateSet = true;
                    } else {
                        isEndDateSet = true;
                    }
                    updateDateRangeText();
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        datePickerDialog.show();
    }

    private void updateDateRangeText() {
        if (isStartDateSet && isEndDateSet) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            String text = "From: " + sdf.format(startDate.getTime()) + "\nTo: " + sdf.format(endDate.getTime());
            tvSelectedDateRange.setText(text);
        }
    }

    private void submitRequest() {
        if (!isStartDateSet || !isEndDateSet) {
            Toast.makeText(this, "Please select both start and end dates.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (endDate.before(startDate)) {
            Toast.makeText(this, "End date cannot be before start date.", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> request = new HashMap<>();
        request.put("memberId", memberDocId);
        request.put("memberName", memberName);
        request.put("messId", messId);
        request.put("startDate", new Timestamp(startDate.getTime()));
        request.put("endDate", new Timestamp(endDate.getTime()));
        request.put("status", "Active");

        db.collection("meal_off_requests").add(request)
                .addOnSuccessListener(docRef -> Toast.makeText(this, "Meal off request submitted.", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to submit request.", Toast.LENGTH_SHORT).show());
    }
}