package com.joney.messmanagement;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class BazarDashboardActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bazar_dashboard);

        Button btnManageProducts = findViewById(R.id.btnGoToManageProducts);
        Button btnBazarAnalysis = findViewById(R.id.btnGoToBazarAnalysis);

        btnManageProducts.setOnClickListener(v -> {
            startActivity(new Intent(this, ProductManagementActivity.class));
        });

        btnBazarAnalysis.setOnClickListener(v -> {
            startActivity(new Intent(this, BazarAnalysisActivity.class));
        });
    }
}