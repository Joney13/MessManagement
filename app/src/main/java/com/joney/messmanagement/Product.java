package com.joney.messmanagement;

public class Product {
    private String productName;
    private String unit;
    private String messId;

    // Firestore-এর জন্য খালি কনস্ট্রাক্টর
    public Product() {}

    public Product(String productName, String unit, String messId) {
        this.productName = productName;
        this.unit = unit;
        this.messId = messId;
    }

    public String getProductName() {
        return productName;
    }

    public String getUnit() {
        return unit;
    }

    public String getMessId() {
        return messId;
    }
}