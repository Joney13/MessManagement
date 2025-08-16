package com.joney.messmanagement;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class AdminMealOffAdapter extends FirestoreRecyclerAdapter<MealOffRequest, AdminMealOffAdapter.RequestViewHolder> {

    public AdminMealOffAdapter(@NonNull FirestoreRecyclerOptions<MealOffRequest> options) {
        super(options);
    }

    @Override
    protected void onBindViewHolder(@NonNull RequestViewHolder holder, int position, @NonNull MealOffRequest model) {
        holder.tvRequestMemberName.setText(model.getMemberName());
        holder.tvRequestStatusAdmin.setText("Status: " + model.getStatus());

        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM, yyyy", Locale.getDefault());
        String startDate = sdf.format(model.getStartDate().toDate());
        String endDate = sdf.format(model.getEndDate().toDate());
        holder.tvRequestDateRange.setText("From: " + startDate + " - To: " + endDate);
    }

    @NonNull
    @Override
    public RequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.admin_meal_off_item_layout, parent, false);
        return new RequestViewHolder(view);
    }

    class RequestViewHolder extends RecyclerView.ViewHolder {
        TextView tvRequestMemberName, tvRequestDateRange, tvRequestStatusAdmin;
        public RequestViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRequestMemberName = itemView.findViewById(R.id.tvRequestMemberName);
            tvRequestDateRange = itemView.findViewById(R.id.tvRequestDateRange);
            tvRequestStatusAdmin = itemView.findViewById(R.id.tvRequestStatusAdmin);
        }
    }
}