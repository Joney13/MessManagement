package com.joney.messmanagement;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.PopupMenu;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import java.util.Objects;

public class ProductManagementActivity extends AppCompatActivity {

    private TextInputEditText etProductName, etProductUnit;
    private Button btnSaveProduct;
    private RecyclerView productsRecyclerView;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private ProductAdapter adapter;
    private String currentMessId;

    // Edit mode variables
    private boolean isEditMode = false;
    private String editProductId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_management);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        etProductName = findViewById(R.id.etProductName);
        etProductUnit = findViewById(R.id.etProductUnit);
        btnSaveProduct = findViewById(R.id.btnSaveProduct);
        productsRecyclerView = findViewById(R.id.productsRecyclerView);

        fetchMessIdAndSetupRecyclerView();

        // Save বা Update করার জন্য একটি মাত্র Listener
        btnSaveProduct.setOnClickListener(v -> saveOrUpdateProduct());
    }

    private void fetchMessIdAndSetupRecyclerView() {
        if (mAuth.getCurrentUser() == null) return;
        String adminUid = mAuth.getCurrentUser().getUid();
        db.collection("users").document(adminUid).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                currentMessId = documentSnapshot.getString("messId");
                if (currentMessId != null) {
                    setupRecyclerView();
                }
            }
        });
    }

    private void setupRecyclerView() {
        Query query = db.collection("products")
                .whereEqualTo("messId", currentMessId)
                .orderBy("productName", Query.Direction.ASCENDING);

        FirestoreRecyclerOptions<Product> options = new FirestoreRecyclerOptions.Builder<Product>()
                .setQuery(query, Product.class)
                .setLifecycleOwner(this)
                .build();

        adapter = new ProductAdapter(options);
        productsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        productsRecyclerView.setAdapter(adapter);

        adapter.setOnItemClickListener((documentSnapshot, view) -> showPopupMenu(documentSnapshot, view));
    }

    private void showPopupMenu(DocumentSnapshot snapshot, View view) {
        PopupMenu popupMenu = new PopupMenu(this, view);
        popupMenu.getMenuInflater().inflate(R.menu.item_edit_delete_menu, popupMenu.getMenu());
        popupMenu.show();
        popupMenu.setOnMenuItemClickListener(item -> {
            Product product = snapshot.toObject(Product.class);
            if (item.getItemId() == R.id.menu_edit) {
                enterEditMode(product, snapshot.getId());
                return true;
            }
            if (item.getItemId() == R.id.menu_delete) {
                deleteProduct(product, snapshot);
                return true;
            }
            return false;
        });
    }

    private void enterEditMode(Product product, String docId) {
        isEditMode = true;
        editProductId = docId;
        etProductName.setText(product.getProductName());
        etProductUnit.setText(product.getUnit());
        btnSaveProduct.setText("Update Product");
    }

    private void deleteProduct(Product product, DocumentSnapshot snapshot) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Product")
                .setMessage("Are you sure you want to delete " + product.getProductName() + "?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    snapshot.getReference().delete();
                    Toast.makeText(this, "Product deleted", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void saveOrUpdateProduct() {
        String productName = Objects.requireNonNull(etProductName.getText()).toString().trim();
        String productUnit = Objects.requireNonNull(etProductUnit.getText()).toString().trim();

        if (productName.isEmpty() || productUnit.isEmpty()) {
            Toast.makeText(this, "Please enter product name and unit", Toast.LENGTH_SHORT).show();
            return;
        }
        if (currentMessId == null) {
            Toast.makeText(this, "Error: Mess ID not found", Toast.LENGTH_SHORT).show();
            return;
        }

        Product product = new Product(productName, productUnit, currentMessId);

        if (isEditMode) {
            // Update the existing product
            db.collection("products").document(editProductId).set(product)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Product updated successfully", Toast.LENGTH_SHORT).show();
                        resetForm();
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Error updating product", Toast.LENGTH_SHORT).show());
        } else {
            // Save a new product
            db.collection("products").add(product)
                    .addOnSuccessListener(documentReference -> {
                        Toast.makeText(this, "Product saved successfully", Toast.LENGTH_SHORT).show();
                        resetForm();
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Error saving product", Toast.LENGTH_SHORT).show());
        }
    }

    private void resetForm() {
        isEditMode = false;
        editProductId = null;
        etProductName.setText("");
        etProductUnit.setText("");
        btnSaveProduct.setText("Save Product");
    }

    // Adapter-এর জন্য OnItemClickListener ইন্টারফেসটি এখানে যুক্ত করতে হবে
    @Override
    protected void onStart() {
        super.onStart();
        if(adapter != null) adapter.startListening();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(adapter != null) adapter.stopListening();
    }
}