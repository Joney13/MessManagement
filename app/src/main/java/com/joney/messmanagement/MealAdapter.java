package com.joney.messmanagement;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import java.util.Locale;

public class MealAdapter extends RecyclerView.Adapter<MealAdapter.MealViewHolder> {

    private List<MealRecord> mealRecordList;

    public MealAdapter(List<MealRecord> mealRecordList) {
        this.mealRecordList = mealRecordList;
    }

    @NonNull
    @Override
    public MealViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.meal_item_layout, parent, false);
        return new MealViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MealViewHolder holder, int position) {
        MealRecord currentRecord = mealRecordList.get(position);

        holder.tvMemberNameMeal.setText(currentRecord.getMemberName());
        holder.tvBreakfastCount.setText(String.format(Locale.US, "%.1f", currentRecord.getBreakfast()));
        holder.tvLunchCount.setText(String.format(Locale.US, "%.1f", currentRecord.getLunch()));
        holder.tvDinnerCount.setText(String.format(Locale.US, "%.1f", currentRecord.getDinner()));
        holder.tvGuestBreakfastCount.setText(String.format(Locale.US, "%.1f", currentRecord.getGuestBreakfast()));
        holder.tvGuestLunchCount.setText(String.format(Locale.US, "%.1f", currentRecord.getGuestLunch()));
        holder.tvGuestDinnerCount.setText(String.format(Locale.US, "%.1f", currentRecord.getGuestDinner()));

        // গেস্ট মিল লেআউটের visibility সেট করা
        holder.guestMealLayout.setVisibility(currentRecord.isGuestLayoutVisible() ? View.VISIBLE : View.GONE);
        holder.btnToggleGuestMeal.setText(currentRecord.isGuestLayoutVisible() ? "Hide Guest Meal" : "Add Guest Meal");

        // টগল বাটনের জন্য Listener
        holder.btnToggleGuestMeal.setOnClickListener(v -> {
            currentRecord.setGuestLayoutVisible(!currentRecord.isGuestLayoutVisible());
            notifyItemChanged(position);
        });

        // সব +/- বাটনের জন্য Listener সেট করা
        setupMealCounterListeners(holder, currentRecord, position);
    }

    private void setupMealCounterListeners(@NonNull MealViewHolder holder, MealRecord currentRecord, int position) {
        // Breakfast
        holder.btnBreakfastPlus.setOnClickListener(v -> {
            currentRecord.setBreakfast(currentRecord.getBreakfast() + 0.5);
            notifyItemChanged(position);
        });
        holder.btnBreakfastMinus.setOnClickListener(v -> {
            if (currentRecord.getBreakfast() > 0) {
                currentRecord.setBreakfast(currentRecord.getBreakfast() - 0.5);
                notifyItemChanged(position);
            }
        });

        // Lunch
        holder.btnLunchPlus.setOnClickListener(v -> {
            currentRecord.setLunch(currentRecord.getLunch() + 1);
            notifyItemChanged(position);
        });
        holder.btnLunchMinus.setOnClickListener(v -> {
            if (currentRecord.getLunch() > 0) {
                currentRecord.setLunch(currentRecord.getLunch() - 1);
                notifyItemChanged(position);
            }
        });

        // Dinner
        holder.btnDinnerPlus.setOnClickListener(v -> {
            currentRecord.setDinner(currentRecord.getDinner() + 1);
            notifyItemChanged(position);
        });
        holder.btnDinnerMinus.setOnClickListener(v -> {
            if (currentRecord.getDinner() > 0) {
                currentRecord.setDinner(currentRecord.getDinner() - 1);
                notifyItemChanged(position);
            }
        });

        // Guest Breakfast
        holder.btnGuestBreakfastPlus.setOnClickListener(v -> {
            currentRecord.setGuestBreakfast(currentRecord.getGuestBreakfast() + 0.5);
            notifyItemChanged(position);
        });
        holder.btnGuestBreakfastMinus.setOnClickListener(v -> {
            if (currentRecord.getGuestBreakfast() > 0) {
                currentRecord.setGuestBreakfast(currentRecord.getGuestBreakfast() - 0.5);
                notifyItemChanged(position);
            }
        });

        // Guest Lunch
        holder.btnGuestLunchPlus.setOnClickListener(v -> {
            currentRecord.setGuestLunch(currentRecord.getGuestLunch() + 1);
            notifyItemChanged(position);
        });
        holder.btnGuestLunchMinus.setOnClickListener(v -> {
            if (currentRecord.getGuestLunch() > 0) {
                currentRecord.setGuestLunch(currentRecord.getGuestLunch() - 1);
                notifyItemChanged(position);
            }
        });

        // Guest Dinner
        holder.btnGuestDinnerPlus.setOnClickListener(v -> {
            currentRecord.setGuestDinner(currentRecord.getGuestDinner() + 1);
            notifyItemChanged(position);
        });
        holder.btnGuestDinnerMinus.setOnClickListener(v -> {
            if (currentRecord.getGuestDinner() > 0) {
                currentRecord.setGuestDinner(currentRecord.getGuestDinner() - 1);
                notifyItemChanged(position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return mealRecordList.size();
    }

    public List<MealRecord> getMealRecords() {
        return mealRecordList;
    }

    public static class MealViewHolder extends RecyclerView.ViewHolder {
        TextView tvMemberNameMeal, tvBreakfastCount, tvLunchCount, tvDinnerCount, tvGuestBreakfastCount, tvGuestLunchCount, tvGuestDinnerCount;
        ImageButton btnBreakfastPlus, btnBreakfastMinus, btnLunchPlus, btnLunchMinus, btnDinnerPlus, btnDinnerMinus,
                btnGuestBreakfastPlus, btnGuestBreakfastMinus, btnGuestLunchPlus, btnGuestLunchMinus,
                btnGuestDinnerPlus, btnGuestDinnerMinus;
        LinearLayout guestMealLayout;
        Button btnToggleGuestMeal;

        public MealViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMemberNameMeal = itemView.findViewById(R.id.tvMemberNameMeal);

            tvBreakfastCount = itemView.findViewById(R.id.tvBreakfastCount);
            btnBreakfastPlus = itemView.findViewById(R.id.btnBreakfastPlus);
            btnBreakfastMinus = itemView.findViewById(R.id.btnBreakfastMinus);

            tvLunchCount = itemView.findViewById(R.id.tvLunchCount);
            btnLunchPlus = itemView.findViewById(R.id.btnLunchPlus);
            btnLunchMinus = itemView.findViewById(R.id.btnLunchMinus);

            tvDinnerCount = itemView.findViewById(R.id.tvDinnerCount);
            btnDinnerPlus = itemView.findViewById(R.id.btnDinnerPlus);
            btnDinnerMinus = itemView.findViewById(R.id.btnDinnerMinus);

            guestMealLayout = itemView.findViewById(R.id.guestMealLayout);
            btnToggleGuestMeal = itemView.findViewById(R.id.btnToggleGuestMeal);

            tvGuestBreakfastCount = itemView.findViewById(R.id.tvGuestBreakfastCount);
            btnGuestBreakfastPlus = itemView.findViewById(R.id.btnGuestBreakfastPlus);
            btnGuestBreakfastMinus = itemView.findViewById(R.id.btnGuestBreakfastMinus);

            tvGuestLunchCount = itemView.findViewById(R.id.tvGuestLunchCount);
            btnGuestLunchPlus = itemView.findViewById(R.id.btnGuestLunchPlus);
            btnGuestLunchMinus = itemView.findViewById(R.id.btnGuestLunchMinus);

            tvGuestDinnerCount = itemView.findViewById(R.id.tvGuestDinnerCount);
            btnGuestDinnerPlus = itemView.findViewById(R.id.btnGuestDinnerPlus);
            btnGuestDinnerMinus = itemView.findViewById(R.id.btnGuestDinnerMinus);
        }
    }
}