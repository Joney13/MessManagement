package com.joney.messmanagement;

import com.google.firebase.Timestamp;

public class Deposit {
    private String memberName;
    private double amount;
    private Timestamp depositDate;

    // Firestore-এর জন্য খালি কনস্ট্রাক্টর
    public Deposit() {}

    public String getMemberName() {
        return memberName;
    }

    public double getAmount() {
        return amount;
    }

    public Timestamp getDepositDate() {
        return depositDate;
    }
}