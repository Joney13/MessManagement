package com.joney.messmanagement;

import com.google.firebase.Timestamp;

public class Cost {
    private String details;
    private double amount;
    private Timestamp costDate;

    // Firestore-এর জন্য খালি কনস্ট্রাক্টর
    public Cost() {}

    public String getDetails() {
        return details;
    }

    public double getAmount() {
        return amount;
    }

    public Timestamp getCostDate() {
        return costDate;
    }
}