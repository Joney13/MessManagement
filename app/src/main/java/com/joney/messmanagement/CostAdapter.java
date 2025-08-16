package com.joney.messmanagement;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.firestore.DocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class CostAdapter extends FirestoreRecyclerAdapter<Cost, CostAdapter.CostViewHolder> {

    public CostAdapter(@NonNull FirestoreRecyclerOptions<Cost> options) {
        super(options);
    }

    @Override
    protected void onBindViewHolder(@NonNull CostViewHolder holder, int position, @NonNull Cost model) {
        holder.tvCostDetails.setText(model.getDetails());
        holder.tvCostAmount.setText(String.format(Locale.US, "BDT %.2f", model.getAmount()));

        if (model.getCostDate() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMMM, yyyy", Locale.getDefault());
            holder.tvCostDate.setText(sdf.format(model.getCostDate().toDate()));
        }
    }

    @NonNull
    @Override
    public CostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.cost_item_layout, parent, false);
        return new CostViewHolder(view);
    }

    class CostViewHolder extends RecyclerView.ViewHolder {
        TextView tvCostDetails, tvCostDate, tvCostAmount;
        ImageView ivMenuOptions;

        public CostViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCostDetails = itemView.findViewById(R.id.tvCostDetails);
            tvCostDate = itemView.findViewById(R.id.tvCostDate);
            tvCostAmount = itemView.findViewById(R.id.tvCostAmount);
            ivMenuOptions = itemView.findViewById(R.id.ivMenuOptions);

            // Click listener for the whole item to view details
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    String docId = getSnapshots().getSnapshot(position).getId();
                    Intent intent = new Intent(v.getContext(), CostDetailsActivity.class);
                    intent.putExtra("COST_DOC_ID", docId);
                    v.getContext().startActivity(intent);
                }
            });

            // Click listener for the menu icon
            ivMenuOptions.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    showPopupMenu(v, position);
                }
            });
        }

        private void showPopupMenu(View view, int position) {
            PopupMenu popupMenu = new PopupMenu(view.getContext(), view);
            popupMenu.getMenuInflater().inflate(R.menu.item_edit_delete_menu, popupMenu.getMenu());
            popupMenu.setOnMenuItemClickListener(item -> {
                DocumentSnapshot snapshot = getSnapshots().getSnapshot(position);
                if (item.getItemId() == R.id.menu_edit) {
                    // Handle Edit
                    Intent intent = new Intent(view.getContext(), CostActivity.class);
                    intent.putExtra("EDIT_COST_ID", snapshot.getId());
                    view.getContext().startActivity(intent);
                    return true;
                }
                if (item.getItemId() == R.id.menu_delete) {
                    // Handle Delete
                    snapshot.getReference().delete()
                            .addOnSuccessListener(aVoid -> Toast.makeText(view.getContext(), "Item deleted", Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e -> Toast.makeText(view.getContext(), "Error deleting item", Toast.LENGTH_SHORT).show());
                    return true;
                }
                return false;
            });
            popupMenu.show();
        }
    }
}