package com.joney.messmanagement;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class SetMenuActivity extends AppCompatActivity {

    private TextView tvMenuDate;
    private TextInputEditText etBreakfastMenu, etLunchMenu, etDinnerMenu;
    private Button btnSaveMenu;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String currentMessId;
    private String todayDateString;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_menu);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        tvMenuDate = findViewById(R.id.tvMenuDate);
        etBreakfastMenu = findViewById(R.id.etBreakfastMenu);
        etLunchMenu = findViewById(R.id.etLunchMenu);
        etDinnerMenu = findViewById(R.id.etDinnerMenu);
        btnSaveMenu = findViewById(R.id.btnSaveMenu);

        // Set current date
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMMM, yyyy", Locale.getDefault());
        String displayDate = "Date: " + sdf.format(new Date());
        tvMenuDate.setText(displayDate);

        // Create date string for document ID
        SimpleDateFormat docIdFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        todayDateString = docIdFormat.format(new Date());

        fetchMessIdAndLoadExistingMenu();

        btnSaveMenu.setOnClickListener(v -> saveMenu());
    }

    private void fetchMessIdAndLoadExistingMenu() {
        String adminUid = Objects.requireNonNull(mAuth.getCurrentUser()).getUid();
        db.collection("users").document(adminUid).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                currentMessId = documentSnapshot.getString("messId");
                if (currentMessId != null) {
                    loadExistingMenu();
                }
            }
        });
    }

    private void loadExistingMenu() {
        // আজকের তারিখ দিয়ে ডকুমেন্টটি খোঁজা হচ্ছে
        db.collection("daily_menus").document(todayDateString + "_" + currentMessId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        etBreakfastMenu.setText(documentSnapshot.getString("breakfastMenu"));
                        etLunchMenu.setText(documentSnapshot.getString("lunchMenu"));
                        etDinnerMenu.setText(documentSnapshot.getString("dinnerMenu"));
                    }
                });
    }


    private void saveMenu() {
        String breakfast = Objects.requireNonNull(etBreakfastMenu.getText()).toString().trim();
        String lunch = Objects.requireNonNull(etLunchMenu.getText()).toString().trim();
        String dinner = Objects.requireNonNull(etDinnerMenu.getText()).toString().trim();

        if (breakfast.isEmpty() && lunch.isEmpty() && dinner.isEmpty()) {
            Toast.makeText(this, "Please enter at least one menu", Toast.LENGTH_SHORT).show();
            return;
        }
        if (currentMessId == null) {
            Toast.makeText(this, "Error: Mess ID not found", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> menu = new HashMap<>();
        menu.put("messId", currentMessId);
        menu.put("date", todayDateString);
        menu.put("breakfastMenu", breakfast);
        menu.put("lunchMenu", lunch);
        menu.put("dinnerMenu", dinner);
        menu.put("timestamp", Timestamp.now());

        // তারিখ এবং messId মিলিয়ে একটি ইউনিক ডকুমেন্ট আইডি তৈরি করা হচ্ছে
        String documentId = todayDateString + "_" + currentMessId;

        db.collection("daily_menus").document(documentId).set(menu)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Menu saved successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}