package com.joney.messmanagement;

public class BazarItem {
    private String itemName;
    private double itemPrice;

    // Firestore-এর জন্য খালি কনস্ট্রাক্টর
    public BazarItem() {}

    public BazarItem(String itemName, double itemPrice) {
        this.itemName = itemName;
        this.itemPrice = itemPrice;
    }

    public String getItemName() {
        return itemName;
    }

    public double getItemPrice() {
        return itemPrice;
    }
}