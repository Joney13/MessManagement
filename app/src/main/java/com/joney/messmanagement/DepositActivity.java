package com.joney.messmanagement;

import android.os.Bundle;
import android.util.Log; // Log ক্লাসটি ইম্পোর্ট করুন
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class DepositActivity extends AppCompatActivity {

    private Spinner spinnerMembers;
    private TextInputEditText etDepositAmount;
    private Button btnSaveDeposit;
    private RecyclerView depositRecyclerView;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private DepositAdapter adapter;

    private List<String> memberNames = new ArrayList<>();
    private List<String> memberDocumentIds = new ArrayList<>();
    private String currentMessId;

    // ডিবাগিং এর জন্য একটি ট্যাগ
    private static final String TAG = "DepositActivityDebug";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_deposit);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        spinnerMembers = findViewById(R.id.spinnerMembers);
        etDepositAmount = findViewById(R.id.etDepositAmount);
        btnSaveDeposit = findViewById(R.id.btnSaveDeposit);
        depositRecyclerView = findViewById(R.id.depositRecyclerView);
        depositRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        fetchMessIdAndLoadData();
        btnSaveDeposit.setOnClickListener(v -> saveDeposit());
    }

    private void fetchMessIdAndLoadData() {
        String adminUid = Objects.requireNonNull(mAuth.getCurrentUser()).getUid();
        Log.d(TAG, "Fetching messId for admin UID: " + adminUid);
        db.collection("users").document(adminUid).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                currentMessId = documentSnapshot.getString("messId");
                Log.d(TAG, "Found messId: " + currentMessId);
                if (currentMessId != null) {
                    loadMembersInSpinner();
                    setupRecyclerView();
                } else {
                    Log.e(TAG, "messId is null in admin document!");
                }
            } else {
                Log.e(TAG, "Admin document not found!");
                Toast.makeText(this, "Could not find admin details.", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(e -> Log.e(TAG, "Failed to fetch admin details", e));
    }

    private void setupRecyclerView() {
        Log.d(TAG, "Setting up RecyclerView with messId: " + currentMessId);
        Query query = db.collection("deposits")
                .whereEqualTo("messId", currentMessId)
                .orderBy("depositDate", Query.Direction.DESCENDING);

        FirestoreRecyclerOptions<Deposit> options = new FirestoreRecyclerOptions.Builder<Deposit>()
                .setQuery(query, Deposit.class)
                .setLifecycleOwner(this)
                .build();

        adapter = new DepositAdapter(options);
        depositRecyclerView.setAdapter(adapter);
    }

    // ... বাকি সব কোড অপরিবর্তিত থাকবে ...
    private void loadMembersInSpinner() {
        db.collection("users")
                .whereEqualTo("messId", currentMessId)
                .whereEqualTo("role", "member")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        memberNames.clear();
                        memberDocumentIds.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Member member = document.toObject(Member.class);
                            memberNames.add(member.getName());
                            memberDocumentIds.add(document.getId());
                        }
                        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, memberNames);
                        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        spinnerMembers.setAdapter(spinnerAdapter);
                    } else {
                        Toast.makeText(this, "Failed to load members.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void saveDeposit() {
        String amountStr = Objects.requireNonNull(etDepositAmount.getText()).toString().trim();
        int selectedMemberPosition = spinnerMembers.getSelectedItemPosition();

        if (amountStr.isEmpty()) {
            etDepositAmount.setError("Amount is required");
            return;
        }
        if (selectedMemberPosition < 0 || memberDocumentIds.isEmpty()) {
            Toast.makeText(this, "Please select a member.", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount = Double.parseDouble(amountStr);
        String selectedMemberId = memberDocumentIds.get(selectedMemberPosition);
        String selectedMemberName = memberNames.get(selectedMemberPosition);

        Map<String, Object> deposit = new HashMap<>();
        deposit.put("amount", amount);
        deposit.put("depositDate", Timestamp.now());
        deposit.put("messId", currentMessId);
        deposit.put("memberId", selectedMemberId);
        deposit.put("memberName", selectedMemberName);

        db.collection("deposits").add(deposit)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Deposit saved successfully!", Toast.LENGTH_SHORT).show();
                    etDepositAmount.setText("");
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error saving deposit: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}