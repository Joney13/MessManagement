package com.joney.messmanagement;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class CostActivity extends AppCompatActivity {

    private TextInputEditText etItemName, etItemPrice;
    private Button btnAddItem, btnSaveBazarList;
    private RecyclerView rvBazarList;
    private TextView tvTotalBazarCost;
    private Toolbar toolbar;
    private List<BazarItem> bazarItemList = new ArrayList<>();
    private BazarAdapter bazarAdapter;
    private double totalCost = 0.0;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String currentMessId;
    private boolean isEditMode = false;
    private String editCostId;
    private static final String TAG = "CostActivityDebug";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cost);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        etItemName = findViewById(R.id.etItemName);
        etItemPrice = findViewById(R.id.etItemPrice);
        btnAddItem = findViewById(R.id.btnAddItem);
        btnSaveBazarList = findViewById(R.id.btnSaveBazarList);
        rvBazarList = findViewById(R.id.rvBazarList);
        tvTotalBazarCost = findViewById(R.id.tvTotalBazarCost);
        toolbar = findViewById(R.id.toolbar_cost);

        setupRecyclerView();
        fetchMessId();

        if (getIntent().hasExtra("EDIT_COST_ID")) {
            isEditMode = true;
            editCostId = getIntent().getStringExtra("EDIT_COST_ID");
            toolbar.setTitle("Edit Bazar List");
            btnSaveBazarList.setText("Update Bazar List");
            loadCostDataForEdit();
        }

        btnAddItem.setOnClickListener(v -> addItemToList());
        btnSaveBazarList.setOnClickListener(v -> saveBazarListToFirestore());
    }

    private void loadCostDataForEdit() {
        bazarItemList.clear();
        db.collection("costs").document(editCostId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        if (documentSnapshot.get("items") instanceof List) {
                            List<Map<String, Object>> items = (List<Map<String, Object>>) documentSnapshot.get("items");
                            if (items != null) {
                                for (Map<String, Object> itemMap : items) {
                                    if (itemMap.get("itemName") instanceof String && itemMap.get("itemPrice") instanceof Number) {
                                        String name = (String) itemMap.get("itemName");
                                        double price = ((Number) itemMap.get("itemPrice")).doubleValue();
                                        bazarItemList.add(new BazarItem(name, price));
                                    }
                                }
                            }
                        } else { // Fallback for old format
                            String oldDetails = documentSnapshot.getString("details");
                            Double oldAmount = documentSnapshot.getDouble("amount");
                            if (oldDetails != null && oldAmount != null) {
                                bazarItemList.add(new BazarItem(oldDetails, oldAmount));
                            }
                        }
                        bazarAdapter.notifyDataSetChanged();
                        updateTotalCost();
                    }
                });
    }

    private void fetchMessId() {
        String adminUid = Objects.requireNonNull(mAuth.getCurrentUser()).getUid();
        db.collection("users").document(adminUid).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                currentMessId = documentSnapshot.getString("messId");
            }
        });
    }

    private void setupRecyclerView() {
        bazarAdapter = new BazarAdapter(bazarItemList);
        rvBazarList.setLayoutManager(new LinearLayoutManager(this));
        rvBazarList.setAdapter(bazarAdapter);

        // অ্যাডাপ্টার থেকে ডিলিট সিগন্যাল এখানে হ্যান্ডেল করা হচ্ছে
        bazarAdapter.setOnItemDeleteListener(position -> {
            bazarItemList.remove(position);
            bazarAdapter.notifyItemRemoved(position);
            updateTotalCost();
        });
    }

    private void addItemToList() {
        String itemName = Objects.requireNonNull(etItemName.getText()).toString().trim();
        String itemPriceStr = Objects.requireNonNull(etItemPrice.getText()).toString().trim();
        if (itemName.isEmpty() || itemPriceStr.isEmpty()) {
            Toast.makeText(this, "Please enter item name and price", Toast.LENGTH_SHORT).show();
            return;
        }
        double itemPrice = Double.parseDouble(itemPriceStr);
        bazarItemList.add(new BazarItem(itemName, itemPrice));
        bazarAdapter.notifyItemInserted(bazarItemList.size() - 1);
        etItemName.setText("");
        etItemPrice.setText("");
        updateTotalCost();
    }

    private void updateTotalCost() {
        totalCost = 0.0;
        for (BazarItem item : bazarItemList) {
            totalCost += item.getItemPrice();
        }
        tvTotalBazarCost.setText(String.format(Locale.US, "Total: BDT %.2f", totalCost));
    }

    private void saveBazarListToFirestore() {
        if (bazarItemList.isEmpty()) {
            Toast.makeText(this, "Please add at least one item", Toast.LENGTH_SHORT).show();
            return;
        }
        if (currentMessId == null) {
            Toast.makeText(this, "Error: Mess ID not found", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> costDocument = new HashMap<>();
        costDocument.put("details", "Bazar List");
        costDocument.put("amount", totalCost);
        costDocument.put("costDate", Timestamp.now());
        costDocument.put("messId", currentMessId);
        costDocument.put("items", bazarItemList);

        if (isEditMode) {
            db.collection("costs").document(editCostId).set(costDocument)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "List updated successfully!", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Error updating list", Toast.LENGTH_SHORT).show());
        } else {
            db.collection("costs").add(costDocument)
                    .addOnSuccessListener(documentReference -> {
                        Toast.makeText(this, "Bazar list saved successfully!", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Error saving list", Toast.LENGTH_SHORT).show());
        }
    }
}