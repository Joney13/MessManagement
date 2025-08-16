package com.joney.messmanagement;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class MealHistoryAdapter extends FirestoreRecyclerAdapter<MealHistoryItem, MealHistoryAdapter.MealHistoryViewHolder> {

    private static final String TAG = "MealHistoryAdapter";
    private OnMealDeletedListener deleteListener;

    // Interface for delete callback
    public interface OnMealDeletedListener {
        void onMealDeleted();
        void onMealDeleteStarted();
    }

    public MealHistoryAdapter(@NonNull FirestoreRecyclerOptions<MealHistoryItem> options) {
        super(options);
        // Enable stable IDs for better performance and crash prevention
        setHasStableIds(true);
    }

    public void setOnMealDeletedListener(OnMealDeletedListener listener) {
        this.deleteListener = listener;
    }

    @Override
    protected void onBindViewHolder(@NonNull MealHistoryViewHolder holder, int position, @NonNull MealHistoryItem model) {
        try {
            // Null safety checks
            if (model == null) {
                Log.w(TAG, "Model is null at position: " + position);
                return;
            }

            // Set member name with null check
            String memberName = model.getMemberName();
            holder.tvHistoryMemberName.setText(memberName != null ? memberName : "Unknown Member");

            // Set total meal with null check
            Double totalMeal = model.getTotalMeal();
            if (totalMeal != null) {
                holder.tvHistoryTotalMeal.setText(String.format(Locale.US, "%.1f", totalMeal));
            } else {
                holder.tvHistoryTotalMeal.setText("0.0");
            }

            // Set date with null check
            if (model.getDate() != null) {
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat("dd MMMM, yyyy", Locale.getDefault());
                    holder.tvHistoryDate.setText(sdf.format(model.getDate().toDate()));
                } catch (Exception e) {
                    Log.e(TAG, "Error formatting date at position: " + position, e);
                    holder.tvHistoryDate.setText("Date not available");
                }
            } else {
                holder.tvHistoryDate.setText("Date not available");
            }

        } catch (Exception e) {
            Log.e(TAG, "Error binding view holder at position: " + position, e);
        }
    }

    @NonNull
    @Override
    public MealHistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.meal_history_item_layout, parent, false);
        return new MealHistoryViewHolder(view);
    }

    @Override
    public long getItemId(int position) {
        try {
            // Return a stable ID based on document ID
            if (position >= 0 && position < getItemCount()) {
                return getSnapshots().getSnapshot(position).getId().hashCode();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting item ID for position: " + position, e);
        }
        return RecyclerView.NO_ID;
    }

    @Override
    public int getItemViewType(int position) {
        return 0; // Single view type
    }

    private void showDeleteConfirmationDialog(Context context, int position) {
        try {
            MealHistoryItem item = getItem(position);
            DocumentSnapshot snapshot = getSnapshots().getSnapshot(position);

            if (item == null || snapshot == null) {
                Toast.makeText(context, "Cannot delete: Invalid data", Toast.LENGTH_SHORT).show();
                return;
            }

            String memberName = item.getMemberName();
            String dateStr = "Unknown Date";

            if (item.getDate() != null) {
                SimpleDateFormat fullDateFormat = new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault());
                dateStr = fullDateFormat.format(item.getDate().toDate());
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle("Delete Meal Record");
            builder.setMessage("Are you sure you want to delete the meal record for " +
                    (memberName != null ? memberName : "Unknown Member") +
                    " on " + dateStr + "?");

            builder.setPositiveButton("Delete", (dialog, which) -> {
                deleteMealRecord(context, snapshot, position);
            });

            builder.setNegativeButton("Cancel", (dialog, which) -> {
                dialog.dismiss();
            });

            // Set button colors
            AlertDialog dialog = builder.create();
            dialog.show();

            // Make delete button red
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(
                    context.getResources().getColor(android.R.color.holo_red_dark));

        } catch (Exception e) {
            Log.e(TAG, "Error showing delete dialog", e);
            Toast.makeText(context, "Error showing delete option", Toast.LENGTH_SHORT).show();
        }
    }

    private void deleteMealRecord(Context context, DocumentSnapshot snapshot, int position) {
        try {
            String documentId = snapshot.getId();

            // Notify delete started
            if (deleteListener != null) {
                deleteListener.onMealDeleteStarted();
            }

            // Show loading toast
            Toast.makeText(context, "Deleting meal record...", Toast.LENGTH_SHORT).show();

            FirebaseFirestore db = FirebaseFirestore.getInstance();

            db.collection("meals").document(documentId)
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Meal record deleted successfully: " + documentId);
                        Toast.makeText(context, "Meal record deleted successfully", Toast.LENGTH_SHORT).show();

                        // Force immediate refresh
                        if (deleteListener != null) {
                            deleteListener.onMealDeleted();
                        }

                        // Also notify item removed for immediate UI update
                        try {
                            notifyItemRemoved(position);
                            notifyItemRangeChanged(position, getItemCount());
                        } catch (Exception e) {
                            Log.w(TAG, "Error updating adapter after delete", e);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error deleting meal record", e);
                        Toast.makeText(context, "Failed to delete meal record: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    });

        } catch (Exception e) {
            Log.e(TAG, "Error in delete process", e);
            Toast.makeText(context, "Error deleting meal record", Toast.LENGTH_SHORT).show();
        }
    }

    class MealHistoryViewHolder extends RecyclerView.ViewHolder {
        TextView tvHistoryMemberName, tvHistoryDate, tvHistoryTotalMeal;

        public MealHistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            tvHistoryMemberName = itemView.findViewById(R.id.tvHistoryMemberName);
            tvHistoryDate = itemView.findViewById(R.id.tvHistoryDate);
            tvHistoryTotalMeal = itemView.findViewById(R.id.tvHistoryTotalMeal);

            // Regular click listener
            itemView.setOnClickListener(v -> {
                try {
                    int position = getAdapterPosition();

                    // Position validation
                    if (position == RecyclerView.NO_POSITION || position < 0 || position >= getItemCount()) {
                        Log.w(TAG, "Invalid position for click: " + position);
                        return;
                    }

                    // Get item safely
                    MealHistoryItem clickedItem = getItem(position);
                    if (clickedItem == null) {
                        Log.w(TAG, "Clicked item is null at position: " + position);
                        return;
                    }

                    // Get member document ID safely
                    String memberDocId = null;
                    try {
                        memberDocId = getSnapshots().getSnapshot(position).getString("memberId");
                    } catch (Exception e) {
                        Log.e(TAG, "Error getting memberId from snapshot", e);
                    }

                    if (memberDocId == null || memberDocId.trim().isEmpty()) {
                        Log.w(TAG, "Member document ID is null or empty");
                        return;
                    }

                    // Get member name safely
                    String memberName = clickedItem.getMemberName();
                    if (memberName == null || memberName.trim().isEmpty()) {
                        memberName = "Unknown Member";
                    }

                    // Start activity
                    Intent intent = new Intent(v.getContext(), MemberMealDetailsActivity.class);
                    intent.putExtra("MEMBER_DOC_ID", memberDocId);
                    intent.putExtra("MEMBER_NAME", memberName);
                    v.getContext().startActivity(intent);

                } catch (Exception e) {
                    Log.e(TAG, "Error handling item click", e);
                }
            });

            // Long click listener for delete
            itemView.setOnLongClickListener(v -> {
                try {
                    int position = getAdapterPosition();

                    // Position validation
                    if (position == RecyclerView.NO_POSITION || position < 0 || position >= getItemCount()) {
                        Log.w(TAG, "Invalid position for long click: " + position);
                        return false;
                    }

                    // Show delete confirmation dialog
                    showDeleteConfirmationDialog(v.getContext(), position);
                    return true; // Consume the long click event

                } catch (Exception e) {
                    Log.e(TAG, "Error handling long click", e);
                    Toast.makeText(v.getContext(), "Error showing delete option", Toast.LENGTH_SHORT).show();
                    return false;
                }
            });
        }
    }

    // Override lifecycle methods for better error handling
    @Override
    public void onViewRecycled(@NonNull MealHistoryViewHolder holder) {
        super.onViewRecycled(holder);
        // Clear any references if needed
    }

    @Override
    public void onViewAttachedToWindow(@NonNull MealHistoryViewHolder holder) {
        super.onViewAttachedToWindow(holder);
        // Handle view attachment if needed
    }

    @Override
    public void onViewDetachedFromWindow(@NonNull MealHistoryViewHolder holder) {
        super.onViewDetachedFromWindow(holder);
        // Handle view detachment if needed
    }
}