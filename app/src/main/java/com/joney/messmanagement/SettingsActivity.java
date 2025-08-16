package com.joney.messmanagement;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.List;
import java.util.Objects;

public class SettingsActivity extends AppCompatActivity {

    private Button btnClearAllData, btnChangePassword, btnLogout;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String currentMessId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        btnClearAllData = findViewById(R.id.btnClearAllData);
        btnChangePassword = findViewById(R.id.btnChangePassword);
        btnLogout = findViewById(R.id.btnLogout);

        fetchMessId();

        btnClearAllData.setOnClickListener(v -> showClearDataConfirmationDialog());
        btnChangePassword.setOnClickListener(v -> showChangePasswordDialog());

        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            Intent intent = new Intent(SettingsActivity.this, WelcomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void showChangePasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_change_password, null);
        builder.setView(dialogView);
        builder.setTitle("Change Password");

        final EditText etCurrentPassword = dialogView.findViewById(R.id.etCurrentPassword);
        final EditText etNewPassword = dialogView.findViewById(R.id.etNewPassword);
        final EditText etConfirmPassword = dialogView.findViewById(R.id.etConfirmPassword);

        builder.setPositiveButton("Update", (dialog, which) -> {
            String currentPass = etCurrentPassword.getText().toString();
            String newPass = etNewPassword.getText().toString();
            String confirmPass = etConfirmPassword.getText().toString();

            if (currentPass.isEmpty() || newPass.isEmpty() || confirmPass.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!newPass.equals(confirmPass)) {
                Toast.makeText(this, "New passwords do not match", Toast.LENGTH_SHORT).show();
                return;
            }
            if (newPass.length() < 6) {
                Toast.makeText(this, "New password must be at least 6 characters", Toast.LENGTH_SHORT).show();
                return;
            }

            changePassword(currentPass, newPass);
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void changePassword(String currentPassword, String newPassword) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null || user.getEmail() == null) {
            Toast.makeText(this, "User not found. Please re-login.", Toast.LENGTH_SHORT).show();
            return;
        }

        AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), currentPassword);

        user.reauthenticate(credential).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                user.updatePassword(newPassword).addOnCompleteListener(updateTask -> {
                    if (updateTask.isSuccessful()) {
                        Toast.makeText(this, "Password updated successfully", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Error: " + updateTask.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                Toast.makeText(this, "Authentication failed. Wrong current password?", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchMessId() {
        if(mAuth.getCurrentUser() == null) return;
        String adminUid = mAuth.getCurrentUser().getUid();
        db.collection("users").document(adminUid).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                currentMessId = documentSnapshot.getString("messId");
            }
        });
    }

    private void showClearDataConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Warning")
                .setMessage("Are you sure you want to delete all data for this mess? This action cannot be undone.")
                .setPositiveButton("Yes, Delete Everything", (dialog, which) -> {
                    if (currentMessId != null) {
                        deleteAllData();
                    } else {
                        Toast.makeText(this, "Error: Mess ID not found.", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteAllData() {
        Toast.makeText(this, "Deleting data... Please wait.", Toast.LENGTH_SHORT).show();

        Task<QuerySnapshot> membersTask = db.collection("users").whereEqualTo("messId", currentMessId).whereEqualTo("role", "member").get();
        Task<QuerySnapshot> depositsTask = db.collection("deposits").whereEqualTo("messId", currentMessId).get();
        Task<QuerySnapshot> costsTask = db.collection("costs").whereEqualTo("messId", currentMessId).get();
        Task<QuerySnapshot> mealsTask = db.collection("meals").whereEqualTo("messId", currentMessId).get();
        Task<QuerySnapshot> noticesTask = db.collection("notices").whereEqualTo("messId", currentMessId).get();

        Tasks.whenAllSuccess(membersTask, depositsTask, costsTask, mealsTask, noticesTask).addOnSuccessListener(list -> {
            WriteBatch batch = db.batch();
            for (Object snapshot : list) {
                for (QueryDocumentSnapshot doc : (QuerySnapshot) snapshot) {
                    batch.delete(doc.getReference());
                }
            }
            batch.commit().addOnSuccessListener(aVoid -> {
                Toast.makeText(this, "All mess data has been deleted successfully.", Toast.LENGTH_LONG).show();
            }).addOnFailureListener(e -> {
                Toast.makeText(this, "Failed to delete data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
        });
    }
}