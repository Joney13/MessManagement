package com.joney.messmanagement;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class MealHistoryTableAdapter extends RecyclerView.Adapter<MealHistoryTableAdapter.ViewHolder> {

    private List<MealHistoryTableItem> mealList;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd", Locale.getDefault());
    private SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE", Locale.getDefault());

    public MealHistoryTableAdapter(List<MealHistoryTableItem> mealList) {
        this.mealList = mealList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.meal_history_table_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MealHistoryTableItem item = mealList.get(position);

        try {
            // Set date
            if (item.getDate() != null) {
                holder.tvDate.setText(dateFormat.format(item.getDate().toDate()));
                holder.tvDay.setText(dayFormat.format(item.getDate().toDate()));
            } else {
                holder.tvDate.setText("--");
                holder.tvDay.setText("--");
            }

            // Set meal data
            holder.tvBreakfast.setText(formatMeal(item.getBreakfast()));
            holder.tvLunch.setText(formatMeal(item.getLunch()));
            holder.tvDinner.setText(formatMeal(item.getDinner()));
            holder.tvGuestMeals.setText(formatMeal(item.getTotalGuestMeal()));
            holder.tvTotalMeal.setText(formatMeal(item.getTotalMeal()));

            // Color coding for zero values
            setMealColor(holder.tvBreakfast, item.getBreakfast());
            setMealColor(holder.tvLunch, item.getLunch());
            setMealColor(holder.tvDinner, item.getDinner());
            setGuestMealColor(holder.tvGuestMeals, item.getTotalGuestMeal());

        } catch (Exception e) {
            // Handle any formatting errors
            holder.tvDate.setText("Error");
            holder.tvDay.setText("--");
            holder.tvBreakfast.setText("0");
            holder.tvLunch.setText("0");
            holder.tvDinner.setText("0");
            holder.tvGuestMeals.setText("0");
            holder.tvTotalMeal.setText("0");
        }
    }

    @Override
    public int getItemCount() {
        return mealList.size();
    }

    private String formatMeal(double meal) {
        if (meal == 0.0) {
            return "0";
        } else if (meal == 1.0) {
            return "1";
        } else {
            return String.format(Locale.US, "%.1f", meal);
        }
    }

    private void setMealColor(TextView textView, double value) {
        if (value == 0.0) {
            textView.setTextColor(textView.getContext().getResources().getColor(android.R.color.darker_gray));
        } else {
            textView.setTextColor(textView.getContext().getResources().getColor(android.R.color.black));
        }
    }

    private void setGuestMealColor(TextView textView, double value) {
        if (value == 0.0) {
            textView.setTextColor(textView.getContext().getResources().getColor(android.R.color.darker_gray));
        } else {
            textView.setTextColor(textView.getContext().getResources().getColor(android.R.color.holo_purple));
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDate, tvDay, tvBreakfast, tvLunch, tvDinner, tvGuestMeals, tvTotalMeal;

        ViewHolder(View itemView) {
            super(itemView);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvDay = itemView.findViewById(R.id.tvDay);
            tvBreakfast = itemView.findViewById(R.id.tvBreakfast);
            tvLunch = itemView.findViewById(R.id.tvLunch);
            tvDinner = itemView.findViewById(R.id.tvDinner);
            tvGuestMeals = itemView.findViewById(R.id.tvGuestMeals);
            tvTotalMeal = itemView.findViewById(R.id.tvTotalMeal);
        }
    }
}