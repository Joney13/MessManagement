package com.joney.messmanagement;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MyProfileActivity extends AppCompatActivity {

    private TextView tvProfileName, tvProfileTotalDeposit, tvProfileTotalMeal,
            tvProfileGuestMeal, tvProfileTotalCost, tvProfileBalance;
    private EditText etMessageToAdmin;
    private Button btnSendMessage;
    private CardView cardMealHistory, cardMealOff, cardChatAdmin, cardSettings;

    private FirebaseFirestore db;
    private String memberDocId;
    private String memberName;
    private String messId;
    private String adminId; // To send private message to admin

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_profile);

        Toolbar toolbar = findViewById(R.id.toolbar_my_profile);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        db = FirebaseFirestore.getInstance();
        memberDocId = getIntent().getStringExtra("MEMBER_DOC_ID");

        // Initialize Views
        tvProfileName = findViewById(R.id.tvProfileName);
        tvProfileTotalDeposit = findViewById(R.id.tvProfileTotalDeposit);
        tvProfileTotalMeal = findViewById(R.id.tvProfileTotalMeal);
        tvProfileGuestMeal = findViewById(R.id.tvProfileGuestMeal);
        tvProfileTotalCost = findViewById(R.id.tvProfileTotalCost);
        tvProfileBalance = findViewById(R.id.tvProfileBalance);
        etMessageToAdmin = findViewById(R.id.etMessageToAdmin);
        btnSendMessage = findViewById(R.id.btnSendMessage);
        cardMealHistory = findViewById(R.id.cardMealHistory);
        cardMealOff = findViewById(R.id.cardMealOff);
        cardChatAdmin = findViewById(R.id.cardChatAdmin);
        cardSettings = findViewById(R.id.cardSettings);

        loadMemberProfile();

        // Set Click Listeners
        btnSendMessage.setOnClickListener(v -> sendMessageToAdmin());
        cardMealHistory.setOnClickListener(v -> {
            Intent intent = new Intent(this, MemberMealDetailsActivity.class);
            intent.putExtra("MEMBER_DOC_ID", memberDocId);
            intent.putExtra("MEMBER_NAME", memberName);
            startActivity(intent);
        });
        cardMealOff.setOnClickListener(v -> {
            Intent intent = new Intent(this, MealOffActivity.class);
            intent.putExtra("MEMBER_DOC_ID", memberDocId);
            startActivity(intent);
        });
        cardChatAdmin.setOnClickListener(v -> {
            if (adminId != null && memberName != null) {
                Intent intent = new Intent(this, ChatActivity.class);
                intent.putExtra("chatType", "private");
                intent.putExtra("receiverId", adminId);
                intent.putExtra("receiverName", "Admin");
                // Pass member info for ChatActivity
                intent.putExtra("USER_TYPE", "MEMBER");
                intent.putExtra("MEMBER_DOC_ID", memberDocId);
                intent.putExtra("MEMBER_NAME", memberName);
                intent.putExtra("MESS_ID", messId);
                startActivity(intent);
            } else {
                Toast.makeText(this, "Admin not found, cannot start chat.", Toast.LENGTH_SHORT).show();
            }
        });
        cardSettings.setOnClickListener(v -> {
            // Future implementation for member settings
            Toast.makeText(this, "Settings coming soon!", Toast.LENGTH_SHORT).show();
        });
    }

    private void loadMemberProfile() {
        if (memberDocId == null) {
            Toast.makeText(this, "Could not load profile.", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("users").document(memberDocId).get().addOnSuccessListener(userDoc -> {
            if (userDoc.exists()) {
                memberName = userDoc.getString("name");
                messId = userDoc.getString("messId");
                tvProfileName.setText(memberName);

                if (messId != null) {
                    calculateAndDisplaySummary();
                    findAdminId(); // Find admin to enable chat
                }
            }
        });
    }

    private void findAdminId() {
        db.collection("users")
                .whereEqualTo("messId", messId)
                .whereEqualTo("role", "admin")
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        adminId = queryDocumentSnapshots.getDocuments().get(0).getId();
                    }
                });
    }

    private void calculateAndDisplaySummary() {
        Task<QuerySnapshot> myDepositsTask = db.collection("deposits").whereEqualTo("memberId", memberDocId).get();
        Task<QuerySnapshot> myMealsTask = db.collection("meals").whereEqualTo("memberId", memberDocId).get();
        Task<QuerySnapshot> allCostsTask = db.collection("costs").whereEqualTo("messId", messId).get();
        Task<QuerySnapshot> allMealsTask = db.collection("meals").whereEqualTo("messId", messId).get();

        Tasks.whenAllSuccess(myDepositsTask, myMealsTask, allCostsTask, allMealsTask).addOnSuccessListener(list -> {
            QuerySnapshot myDepositsResult = (QuerySnapshot) list.get(0);
            QuerySnapshot myMealsResult = (QuerySnapshot) list.get(1);
            QuerySnapshot allCostsResult = (QuerySnapshot) list.get(2);
            QuerySnapshot allMealsResult = (QuerySnapshot) list.get(3);

            double totalDeposit = 0;
            for(QueryDocumentSnapshot doc : myDepositsResult) {
                totalDeposit += doc.getDouble("amount");
            }

            double totalMeal = 0, totalGuestMeal = 0, regularMeals = 0;
            for(QueryDocumentSnapshot doc : myMealsResult) {
                totalMeal += doc.getDouble("totalMeal");
                double guestBreakfast = doc.contains("guestBreakfast") ? doc.getDouble("guestBreakfast") : 0;
                double guestLunch = doc.contains("guestLunch") ? doc.getDouble("guestLunch") : 0;
                double guestDinner = doc.contains("guestDinner") ? doc.getDouble("guestDinner") : 0;
                totalGuestMeal += guestBreakfast + guestLunch + guestDinner;
            }
            regularMeals = totalMeal - totalGuestMeal;

            double messTotalCost = 0;
            for(QueryDocumentSnapshot doc : allCostsResult) messTotalCost += doc.getDouble("amount");

            double messTotalMeals = 0;
            for(QueryDocumentSnapshot doc : allMealsResult) messTotalMeals += doc.getDouble("totalMeal");

            double mealRate = (messTotalMeals > 0) ? (messTotalCost / messTotalMeals) : 0;
            double myTotalCost = totalMeal * mealRate;
            double myBalance = totalDeposit - myTotalCost;

            tvProfileTotalDeposit.setText(String.format(Locale.US, "BDT %.2f", totalDeposit));
            tvProfileTotalMeal.setText(String.format(Locale.US, "%.1f Meals", regularMeals));
            tvProfileGuestMeal.setText(String.format(Locale.US, "%.1f Meals", totalGuestMeal));
            tvProfileTotalCost.setText(String.format(Locale.US, "BDT %.2f", myTotalCost));
            tvProfileBalance.setText(String.format(Locale.US, "BDT %.2f", myBalance));
        });
    }

    private void sendMessageToAdmin() {
        String message = etMessageToAdmin.getText().toString().trim();
        if (message.isEmpty()) {
            Toast.makeText(this, "Message cannot be empty.", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> messageData = new HashMap<>();
        messageData.put("messId", messId);
        messageData.put("memberId", memberDocId);
        messageData.put("memberName", memberName);
        messageData.put("message", message);
        messageData.put("timestamp", Timestamp.now());
        messageData.put("isResolved", false);

        db.collection("requests").add(messageData)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Message sent to admin.", Toast.LENGTH_SHORT).show();
                    etMessageToAdmin.setText("");
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to send message.", Toast.LENGTH_SHORT).show());
    }
}