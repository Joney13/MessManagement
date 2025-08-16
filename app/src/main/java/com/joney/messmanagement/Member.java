package com.joney.messmanagement;

public class Member {
    private String name;
    private String mobile;
    private String email;
    private String description;
    private String userID; // পরিবর্তনটি এখানে করা হয়েছে

    public Member() {
        // Firestore-এর জন্য খালি কনস্ট্রাক্টর আবশ্যক
    }

    public String getName() {
        return name;
    }

    public String getMobile() {
        return mobile;
    }

    public String getEmail() {
        return email;
    }

    public String getDescription() {
        return description;
    }

    // মেথডের নাম getUserId() রাখা যেতে পারে, Firestore স্বয়ংক্রিয়ভাবে মিলিয়ে নেবে
    public String getUserID() {
        return userID;
    }
}