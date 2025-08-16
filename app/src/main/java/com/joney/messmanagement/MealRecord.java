package com.joney.messmanagement;

public class MealRecord {
    private String memberId;
    private String memberName;
    private double breakfast = 0;
    private double lunch = 0;
    private double dinner = 0;
    private double guestBreakfast = 0;
    private double guestLunch = 0;
    private double guestDinner = 0;
    private boolean isGuestLayoutVisible = false; // For UI state

    public MealRecord(String memberId, String memberName) {
        this.memberId = memberId;
        this.memberName = memberName;
    }

    // Getters
    public String getMemberId() {
        return memberId;
    }
    public String getMemberName() {
        return memberName;
    }
    public double getBreakfast() {
        return breakfast;
    }
    public double getLunch() {
        return lunch;
    }
    public double getDinner() {
        return dinner;
    }
    public double getGuestBreakfast() {
        return guestBreakfast;
    }
    public double getGuestLunch() {
        return guestLunch;
    }
    public double getGuestDinner() {
        return guestDinner;
    }
    public boolean isGuestLayoutVisible() {
        return isGuestLayoutVisible;
    }

    // Setters
    public void setBreakfast(double breakfast) {
        this.breakfast = breakfast;
    }
    public void setLunch(double lunch) {
        this.lunch = lunch;
    }
    public void setDinner(double dinner) {
        this.dinner = dinner;
    }
    public void setGuestBreakfast(double guestBreakfast) {
        this.guestBreakfast = guestBreakfast;
    }
    public void setGuestLunch(double guestLunch) {
        this.guestLunch = guestLunch;
    }
    public void setGuestDinner(double guestDinner) {
        this.guestDinner = guestDinner;
    }
    public void setGuestLayoutVisible(boolean guestLayoutVisible) {
        isGuestLayoutVisible = guestLayoutVisible;
    }
}