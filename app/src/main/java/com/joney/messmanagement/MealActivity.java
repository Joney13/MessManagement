package com.joney.messmanagement;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class MealActivity extends AppCompatActivity {

    private TextView tvSelectedDate;
    private Button btnChangeDate, btnSaveAllMeals, btnViewHistory;
    private RecyclerView mealRecyclerView;
    private MealAdapter mealAdapter;
    private List<MealRecord> mealRecordList = new ArrayList<>();

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String currentMessId;
    private Calendar selectedDate = Calendar.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_meal);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        tvSelectedDate = findViewById(R.id.tvSelectedDate);
        btnChangeDate = findViewById(R.id.btnChangeDate);
        btnSaveAllMeals = findViewById(R.id.btnSaveAllMeals);
        mealRecyclerView = findViewById(R.id.mealRecyclerView);
        btnViewHistory = findViewById(R.id.btnViewHistory);

        mealRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mealAdapter = new MealAdapter(mealRecordList);
        mealRecyclerView.setAdapter(mealAdapter);

        updateDateInView();
        setupDatePicker();
        fetchMessIdAndLoadMembers();

        btnSaveAllMeals.setOnClickListener(v -> saveAllMeals());

        btnViewHistory.setOnClickListener(v -> {
            startActivity(new Intent(this, SelectMemberForHistoryActivity.class));
        });
    }

    private void updateDateInView() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMMM, yyyy", Locale.getDefault());
        tvSelectedDate.setText(sdf.format(selectedDate.getTime()));
    }

    private void setupDatePicker() {
        btnChangeDate.setOnClickListener(v -> {
            DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                    (view, year, month, dayOfMonth) -> {
                        selectedDate.set(Calendar.YEAR, year);
                        selectedDate.set(Calendar.MONTH, month);
                        selectedDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                        updateDateInView();
                    },
                    selectedDate.get(Calendar.YEAR),
                    selectedDate.get(Calendar.MONTH),
                    selectedDate.get(Calendar.DAY_OF_MONTH));
            datePickerDialog.show();
        });
    }

    private void fetchMessIdAndLoadMembers() {
        String adminUid = Objects.requireNonNull(mAuth.getCurrentUser()).getUid();
        db.collection("users").document(adminUid).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                currentMessId = documentSnapshot.getString("messId");
                if (currentMessId != null) {
                    loadMembersForMealEntry();
                }
            }
        });
    }

    private void loadMembersForMealEntry() {
        db.collection("users")
                .whereEqualTo("messId", currentMessId)
                .whereEqualTo("role", "member")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        mealRecordList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            mealRecordList.add(new MealRecord(document.getId(), document.getString("name")));
                        }
                        mealAdapter.notifyDataSetChanged();
                    } else {
                        Toast.makeText(this, "Failed to load members.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void saveAllMeals() {
        if (currentMessId == null) {
            Toast.makeText(this, "Cannot save. Mess ID not found.", Toast.LENGTH_SHORT).show();
            return;
        }

        WriteBatch batch = db.batch();
        List<MealRecord> recordsToSave = mealAdapter.getMealRecords();

        for (MealRecord record : recordsToSave) {
            double totalMeal = record.getBreakfast() + record.getLunch() + record.getDinner() +
                    record.getGuestBreakfast() + record.getGuestLunch() + record.getGuestDinner();
            if (totalMeal > 0) {
                Map<String, Object> mealData = new HashMap<>();
                mealData.put("messId", currentMessId);
                mealData.put("memberId", record.getMemberId());
                mealData.put("memberName", record.getMemberName());
                mealData.put("date", new Timestamp(selectedDate.getTime()));
                mealData.put("breakfast", record.getBreakfast());
                mealData.put("lunch", record.getLunch());
                mealData.put("dinner", record.getDinner());
                mealData.put("guestBreakfast", record.getGuestBreakfast());
                mealData.put("guestLunch", record.getGuestLunch());
                mealData.put("guestDinner", record.getGuestDinner());
                mealData.put("totalMeal", totalMeal);

                batch.set(db.collection("meals").document(), mealData);
            }
        }

        batch.commit().addOnSuccessListener(aVoid -> {
            Toast.makeText(this, "All meals saved successfully!", Toast.LENGTH_SHORT).show();
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Error saving meals: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }
}