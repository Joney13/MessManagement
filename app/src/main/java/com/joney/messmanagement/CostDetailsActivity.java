package com.joney.messmanagement;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CostDetailsActivity extends AppCompatActivity {

    private TextView tvCostDateDetails, tvTotalCostDetails;
    private RecyclerView rvCostItemsDetails;
    private FirebaseFirestore db;
    private String costDocId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cost_details);

        db = FirebaseFirestore.getInstance();
        costDocId = getIntent().getStringExtra("COST_DOC_ID");

        tvCostDateDetails = findViewById(R.id.tvCostDateDetails);
        tvTotalCostDetails = findViewById(R.id.tvTotalCostDetails);
        rvCostItemsDetails = findViewById(R.id.rvCostItemsDetails);
        rvCostItemsDetails.setLayoutManager(new LinearLayoutManager(this));

        if (costDocId == null) {
            Toast.makeText(this, "Error: Could not load details.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadCostDetails();
    }

    private void loadCostDetails() {
        db.collection("costs").document(costDocId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // তারিখ এবং মোট খরচ দেখানো
                        Timestamp timestamp = documentSnapshot.getTimestamp("costDate");
                        if (timestamp != null) {
                            SimpleDateFormat sdf = new SimpleDateFormat("dd MMMM, yyyy", Locale.getDefault());
                            tvCostDateDetails.setText("Date: " + sdf.format(timestamp.toDate()));
                        }
                        double totalAmount = documentSnapshot.getDouble("amount");
                        tvTotalCostDetails.setText(String.format(Locale.US, "Total Cost: BDT %.2f", totalAmount));

                        // আইটেমের তালিকা দেখানো
                        List<Map<String, Object>> items = (List<Map<String, Object>>) documentSnapshot.get("items");
                        if (items != null) {
                            List<BazarItem> bazarItemList = new ArrayList<>();
                            for (Map<String, Object> itemMap : items) {
                                String name = (String) itemMap.get("itemName");
                                Double price = (Double) itemMap.get("itemPrice");
                                if (name != null && price != null) {
                                    bazarItemList.add(new BazarItem(name, price));
                                }
                            }
                            BazarAdapter adapter = new BazarAdapter(bazarItemList);
                            rvCostItemsDetails.setAdapter(adapter);
                        }
                    } else {
                        Toast.makeText(this, "Details not found.", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}