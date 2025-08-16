package com.joney.messmanagement;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class BazarAnalysisActivity extends AppCompatActivity {

    private Spinner spinnerProducts;
    private Button btnShowAnalysis;
    private TextView tvAnalysisResult;
    private LineChart priceHistoryChart;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String currentMessId;

    private List<Product> productList = new ArrayList<>();
    private List<String> productNames = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bazar_analysis);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        spinnerProducts = findViewById(R.id.spinnerProducts);
        btnShowAnalysis = findViewById(R.id.btnShowAnalysis);
        tvAnalysisResult = findViewById(R.id.tvAnalysisResult);
        priceHistoryChart = findViewById(R.id.priceHistoryChart);

        fetchMessIdAndLoadProducts();

        btnShowAnalysis.setOnClickListener(v -> showAnalysis());
    }

    private void fetchMessIdAndLoadProducts() {
        String adminUid = mAuth.getCurrentUser().getUid();
        db.collection("users").document(adminUid).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                currentMessId = documentSnapshot.getString("messId");
                if (currentMessId != null) {
                    loadProductsInSpinner();
                }
            }
        });
    }

    private void loadProductsInSpinner() {
        db.collection("products")
                .whereEqualTo("messId", currentMessId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        productList.clear();
                        productNames.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Product product = document.toObject(Product.class);
                            productList.add(product);
                            productNames.add(product.getProductName());
                        }
                        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, productNames);
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        spinnerProducts.setAdapter(adapter);
                    }
                });
    }

    private void showAnalysis() {
        if (productList.isEmpty()) {
            Toast.makeText(this, "No products to analyze.", Toast.LENGTH_SHORT).show();
            return;
        }

        int selectedPosition = spinnerProducts.getSelectedItemPosition();
        Product selectedProduct = productList.get(selectedPosition);
        String selectedProductName = selectedProduct.getProductName();

        tvAnalysisResult.setText("Loading report for " + selectedProductName + "...");
        priceHistoryChart.clear();

        db.collection("costs")
                .whereEqualTo("messId", currentMessId)
                .orderBy("costDate", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Entry> chartEntries = new ArrayList<>();
                    List<String> dateLabels = new ArrayList<>();
                    StringBuilder textReport = new StringBuilder("Price History for " + selectedProductName + ":\n\n");
                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yy", Locale.getDefault());
                    int index = 0;

                    for (QueryDocumentSnapshot costDoc : queryDocumentSnapshots) {
                        List<Map<String, Object>> items = (List<Map<String, Object>>) costDoc.get("items");
                        if (items != null) {
                            for (Map<String, Object> itemMap : items) {
                                String itemName = (String) itemMap.get("itemName");
                                if (selectedProductName.equals(itemName)) {
                                    Date date = costDoc.getTimestamp("costDate").toDate();
                                    float price = ((Number) itemMap.get("itemPrice")).floatValue();

                                    chartEntries.add(new Entry(index, price));
                                    dateLabels.add(sdf.format(date));
                                    textReport.append(sdf.format(date)).append(": BDT ").append(price).append("\n");
                                    index++;
                                }
                            }
                        }
                    }

                    if (chartEntries.isEmpty()) {
                        priceHistoryChart.clear();
                        priceHistoryChart.invalidate();
                        tvAnalysisResult.setText("No purchase history found for " + selectedProductName);
                    } else {
                        setupAndDisplayChart(chartEntries, dateLabels, selectedProductName);
                        tvAnalysisResult.setText(textReport.toString());
                    }
                });
    }

    private void setupAndDisplayChart(List<Entry> entries, List<String> dateLabels, String productName) {
        LineDataSet dataSet = new LineDataSet(entries, "Price of " + productName);
        dataSet.setColor(Color.BLUE);
        dataSet.setValueTextColor(Color.BLACK);
        dataSet.setCircleColor(Color.BLUE);
        dataSet.setLineWidth(2f);
        dataSet.setValueTextSize(12f);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(Color.CYAN);

        LineData lineData = new LineData(dataSet);
        priceHistoryChart.setData(lineData);

        XAxis xAxis = priceHistoryChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(dateLabels));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setGranularityEnabled(true);

        priceHistoryChart.getDescription().setText("Price Fluctuation Over Time");
        priceHistoryChart.animateX(1000);
        priceHistoryChart.invalidate();
    }
}