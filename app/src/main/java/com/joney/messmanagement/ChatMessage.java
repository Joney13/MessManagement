package com.joney.messmanagement;

import com.google.firebase.Timestamp;

public class ChatMessage {
    private String messageText;
    private String senderName;
    private String senderId;
    private Timestamp timestamp;
    private String messId;

    // Firestore-এর জন্য খালি কনস্ট্রাক্টর
    public ChatMessage() {}

    public ChatMessage(String messageText, String senderName, String senderId, String messId) {
        this.messageText = messageText;
        this.senderName = senderName;
        this.senderId = senderId;
        this.messId = messId;
        this.timestamp = Timestamp.now();
    }

    public String getMessageText() { return messageText; }
    public String getSenderName() { return senderName; }
    public String getSenderId() { return senderId; }
    public Timestamp getTimestamp() { return timestamp; }
    public String getMessId() { return messId; }
}