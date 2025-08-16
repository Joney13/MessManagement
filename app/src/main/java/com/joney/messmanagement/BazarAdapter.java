package com.joney.messmanagement;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import java.util.Locale;

public class BazarAdapter extends RecyclerView.Adapter<BazarAdapter.BazarViewHolder> {

    private List<BazarItem> itemList;
    private OnItemDeleteListener deleteListener;

    // আইটেম ডিলিট করার জন্য একটি ইন্টারফেস
    public interface OnItemDeleteListener {
        void onItemDelete(int position);
    }

    public void setOnItemDeleteListener(OnItemDeleteListener listener) {
        this.deleteListener = listener;
    }

    public BazarAdapter(List<BazarItem> itemList) {
        this.itemList = itemList;
    }

    @NonNull
    @Override
    public BazarViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.bazar_item_layout, parent, false);
        // ViewHolder তৈরি করার সময় Listener পাস করা হচ্ছে
        return new BazarViewHolder(view, deleteListener);
    }

    @Override
    public void onBindViewHolder(@NonNull BazarViewHolder holder, int position) {
        BazarItem currentItem = itemList.get(position);
        holder.tvItemName.setText(currentItem.getItemName());
        holder.tvItemPrice.setText(String.format(Locale.US, "BDT %.2f", currentItem.getItemPrice()));
    }

    @Override
    public int getItemCount() {
        return itemList.size();
    }

    // ViewHolder ক্লাসটি static রাখতে হবে
    static class BazarViewHolder extends RecyclerView.ViewHolder {
        TextView tvItemName, tvItemPrice;
        ImageButton btnDeleteItem;

        public BazarViewHolder(@NonNull View itemView, OnItemDeleteListener listener) {
            super(itemView);
            tvItemName = itemView.findViewById(R.id.tvItemName);
            tvItemPrice = itemView.findViewById(R.id.tvItemPrice);
            btnDeleteItem = itemView.findViewById(R.id.btnDeleteItem);

            btnDeleteItem.setOnClickListener(v -> {
                if (listener != null) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        listener.onItemDelete(position);
                    }
                }
            });
        }
    }
}