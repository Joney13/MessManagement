package com.joney.messmanagement;

import com.google.firebase.Timestamp;

public class MealHistoryTableItem {
    private Timestamp date;
    private double breakfast;
    private double lunch;
    private double dinner;
    private double guestBreakfast;
    private double guestLunch;
    private double guestDinner;
    private double totalMeal;
    private double totalGuestMeal;

    public MealHistoryTableItem() {
        // Default constructor required for Firestore
    }

    public MealHistoryTableItem(Timestamp date, double breakfast, double lunch, double dinner,
                                double guestBreakfast, double guestLunch, double guestDinner) {
        this.date = date;
        this.breakfast = breakfast;
        this.lunch = lunch;
        this.dinner = dinner;
        this.guestBreakfast = guestBreakfast;
        this.guestLunch = guestLunch;
        this.guestDinner = guestDinner;
        calculateTotals();
    }

    public void calculateTotals() {
        this.totalMeal = breakfast + lunch + dinner;
        this.totalGuestMeal = guestBreakfast + guestLunch + guestDinner;
    }

    // Getters and Setters
    public Timestamp getDate() { return date; }
    public void setDate(Timestamp date) { this.date = date; }

    public double getBreakfast() { return breakfast; }
    public void setBreakfast(double breakfast) { this.breakfast = breakfast; }

    public double getLunch() { return lunch; }
    public void setLunch(double lunch) { this.lunch = lunch; }

    public double getDinner() { return dinner; }
    public void setDinner(double dinner) { this.dinner = dinner; }

    public double getGuestBreakfast() { return guestBreakfast; }
    public void setGuestBreakfast(double guestBreakfast) { this.guestBreakfast = guestBreakfast; }

    public double getGuestLunch() { return guestLunch; }
    public void setGuestLunch(double guestLunch) { this.guestLunch = guestLunch; }

    public double getGuestDinner() { return guestDinner; }
    public void setGuestDinner(double guestDinner) { this.guestDinner = guestDinner; }

    public double getTotalMeal() { return totalMeal; }
    public void setTotalMeal(double totalMeal) { this.totalMeal = totalMeal; }

    public double getTotalGuestMeal() { return totalGuestMeal; }
    public void setTotalGuestMeal(double totalGuestMeal) { this.totalGuestMeal = totalGuestMeal; }
}