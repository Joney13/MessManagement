package com.joney.messmanagement;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.firestore.DocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class MealOffAdapter extends FirestoreRecyclerAdapter<MealOffRequest, MealOffAdapter.MealOffViewHolder> {

    private OnCancelClickListener listener;

    public MealOffAdapter(@NonNull FirestoreRecyclerOptions<MealOffRequest> options) {
        super(options);
    }

    @Override
    protected void onBindViewHolder(@NonNull MealOffViewHolder holder, int position, @NonNull MealOffRequest model) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM, yyyy", Locale.getDefault());
        String startDate = sdf.format(model.getStartDate().toDate());
        String endDate = sdf.format(model.getEndDate().toDate());
        holder.tvDateRange.setText(startDate + " - " + endDate);
        holder.tvRequestStatus.setText("Status: " + model.getStatus());
    }

    @NonNull
    @Override
    public MealOffViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.meal_off_item_layout, parent, false);
        return new MealOffViewHolder(view);
    }

    class MealOffViewHolder extends RecyclerView.ViewHolder {
        TextView tvDateRange, tvRequestStatus;
        Button btnCancelRequest;

        public MealOffViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDateRange = itemView.findViewById(R.id.tvDateRange);
            tvRequestStatus = itemView.findViewById(R.id.tvRequestStatus);
            btnCancelRequest = itemView.findViewById(R.id.btnCancelRequest);

            btnCancelRequest.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onCancelClick(getSnapshots().getSnapshot(position));
                }
            });
        }
    }

    public interface OnCancelClickListener {
        void onCancelClick(DocumentSnapshot documentSnapshot);
    }

    public void setOnCancelClickListener(OnCancelClickListener listener) {
        this.listener = listener;
    }
}