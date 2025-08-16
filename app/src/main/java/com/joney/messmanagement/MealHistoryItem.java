package com.joney.messmanagement;

import com.google.firebase.Timestamp;

public class MealHistoryItem {
    private String memberName;
    private double totalMeal;
    private Timestamp date;

    public MealHistoryItem() {} // Firestore-এর জন্য আবশ্যক

    public String getMemberName() { return memberName; }
    public double getTotalMeal() { return totalMeal; }
    public Timestamp getDate() { return date; }
}