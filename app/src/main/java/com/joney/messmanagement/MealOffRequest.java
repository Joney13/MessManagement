package com.joney.messmanagement;

import com.google.firebase.Timestamp;

public class MealOffRequest {
    private String memberId;
    private String memberName;
    private String messId;
    private Timestamp startDate;
    private Timestamp endDate;
    private String status; // e.g., "Active", "Cancelled"

    public MealOffRequest() {} // Firestore-এর জন্য আবশ্যক

    // Getters
    public String getMemberId() { return memberId; }
    public String getMemberName() { return memberName; }
    public String getMessId() { return messId; }
    public Timestamp getStartDate() { return startDate; }
    public Timestamp getEndDate() { return endDate; }
    public String getStatus() { return status; }
}