package com.joney.messmanagement;

// প্রয়োজনীয় ক্লাসগুলো ইম্পোর্ট করা হয়েছে
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class WelcomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        Button registerMessButton = findViewById(R.id.registerMessButton);
        Button loginButton = findViewById(R.id.loginButton);

        registerMessButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // RegisterActivity চালু করার জন্য Intent
                startActivity(new Intent(WelcomeActivity.this, RegisterActivity.class));
            }
        });

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // LoginActivity চালু করার জন্য Intent
                startActivity(new Intent(WelcomeActivity.this, LoginActivity.class));
            }
        });
    }
}