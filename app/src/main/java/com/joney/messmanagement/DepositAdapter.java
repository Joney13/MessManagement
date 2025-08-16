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

public class DepositAdapter extends FirestoreRecyclerAdapter<Deposit, DepositAdapter.DepositViewHolder> {

    public DepositAdapter(@NonNull FirestoreRecyclerOptions<Deposit> options) {
        super(options);
    }

    @Override
    protected void onBindViewHolder(@NonNull DepositViewHolder holder, int position, @NonNull Deposit model) {
        holder.tvMemberNameDeposit.setText(model.getMemberName());
        holder.tvDepositAmountItem.setText(String.format(Locale.US, "BDT %.2f", model.getAmount()));

        if (model.getDepositDate() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMMM, yyyy", Locale.getDefault());
            holder.tvDepositDateItem.setText(sdf.format(model.getDepositDate().toDate()));
        }
    }

    @NonNull
    @Override
    public DepositViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.deposit_item_layout, parent, false);
        return new DepositViewHolder(view);
    }

    class DepositViewHolder extends RecyclerView.ViewHolder {
        TextView tvMemberNameDeposit, tvDepositDateItem, tvDepositAmountItem;

        public DepositViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMemberNameDeposit = itemView.findViewById(R.id.tvMemberNameDeposit);
            tvDepositDateItem = itemView.findViewById(R.id.tvDepositDateItem);
            tvDepositAmountItem = itemView.findViewById(R.id.tvDepositAmountItem);
        }
    }
}