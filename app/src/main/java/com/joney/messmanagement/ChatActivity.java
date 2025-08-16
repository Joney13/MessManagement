package com.joney.messmanagement;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ChatActivity extends AppCompatActivity {

    private static final String TAG = "ChatActivity";

    private RecyclerView chatRecyclerView;
    private EditText etMessage;
    private ImageButton btnSendMessage;
    private Toolbar toolbar;
    private FirebaseFirestore db;
    private ChatAdapter adapter;

    private String chatType;
    private String currentMessId;
    private String currentUserName;
    private String currentUserId;

    // For private chat
    private String receiverId;
    private String receiverName;
    private String conversationId;

    // Flag to track if timestamp should be updated
    private boolean shouldUpdateTimestamp = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // Initialize views first
        initializeViews();

        // Then setup user and chat
        checkUserTypeAndSetup();

        btnSendMessage.setOnClickListener(v -> sendMessage());
    }

    private void initializeViews() {
        db = FirebaseFirestore.getInstance();
        toolbar = findViewById(R.id.toolbar_chat);
        chatRecyclerView = findViewById(R.id.chatRecyclerView);
        etMessage = findViewById(R.id.etMessage);
        btnSendMessage = findViewById(R.id.btnSendMessage);

        // Set toolbar as action bar
        setSupportActionBar(toolbar);
    }

    private void checkUserTypeAndSetup() {
        FirebaseUser fUser = FirebaseAuth.getInstance().getCurrentUser();
        chatType = getIntent().getStringExtra("chatType");
        String userType = getIntent().getStringExtra("USER_TYPE");

        if (fUser != null && !"MEMBER".equals(userType)) {
            // Firebase authenticated user (Admin)
            setupAdminUser(fUser);
        } else if ("MEMBER".equals(userType)) {
            // Member user
            setupMemberUser();
        } else {
            // Invalid state
            Log.e(TAG, "Invalid user state");
            Toast.makeText(this, "Unable to identify user type", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void setupAdminUser(FirebaseUser fUser) {
        currentUserId = fUser.getUid();

        db.collection("users").document(currentUserId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        // Null safety check
                        currentUserName = doc.getString("name");
                        currentMessId = doc.getString("messId");

                        if (currentUserName == null || currentMessId == null) {
                            Log.e(TAG, "Missing user data: name or messId is null");
                            Toast.makeText(this, "User data incomplete", Toast.LENGTH_SHORT).show();
                            finish();
                            return;
                        }

                        if ("private".equals(chatType)) {
                            setupPrivateChat();
                        } else {
                            toolbar.setTitle("Mess Group Chat (Admin)");
                        }
                        setupChatRecyclerView();
                    } else {
                        Log.e(TAG, "User document does not exist");
                        Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching user data", e);
                    Toast.makeText(this, "Error loading user data", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void setupMemberUser() {
        currentUserId = getIntent().getStringExtra("MEMBER_DOC_ID");
        currentUserName = getIntent().getStringExtra("MEMBER_NAME");
        currentMessId = getIntent().getStringExtra("MESS_ID");

        // Null safety check
        if (currentUserId == null || currentUserName == null || currentMessId == null) {
            Log.e(TAG, "Missing member data");
            Toast.makeText(this, "Member data incomplete", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        toolbar.setTitle("Mess Group Chat");
        setupChatRecyclerView();
    }

    private void setupPrivateChat() {
        receiverId = getIntent().getStringExtra("receiverId");
        receiverName = getIntent().getStringExtra("receiverName");

        if (receiverId == null || receiverName == null) {
            Log.e(TAG, "Missing private chat data");
            Toast.makeText(this, "Private chat data incomplete", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        toolbar.setTitle(receiverName);
        createConversationId();
    }

    private void updateLastReadTimestamp() {
        if (!shouldUpdateTimestamp || TextUtils.isEmpty(currentUserId)) {
            Log.w(TAG, "Skipping timestamp update");
            return;
        }

        Log.d(TAG, "Updating lastReadTimestamp for user: " + currentUserId);

        db.collection("users").document(currentUserId)
                .update("lastReadTimestamp", Timestamp.now())
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Timestamp updated successfully at: " + Timestamp.now().toDate());
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error updating timestamp", e));
    }

    private void createConversationId() {
        if (currentUserId == null || receiverId == null) {
            Log.e(TAG, "Cannot create conversation ID: missing user IDs");
            return;
        }

        List<String> ids = Arrays.asList(currentUserId, receiverId);
        Collections.sort(ids);
        conversationId = ids.get(0) + "_" + ids.get(1);
    }

    private void setupChatRecyclerView() {
        if (TextUtils.isEmpty(currentUserId)) {
            Log.e(TAG, "Cannot setup chat: currentUserId is null");
            return;
        }

        Query query = buildQuery();
        if (query == null) {
            Log.e(TAG, "Cannot build query");
            return;
        }

        FirestoreRecyclerOptions<ChatMessage> options = new FirestoreRecyclerOptions.Builder<ChatMessage>()
                .setQuery(query, ChatMessage.class)
                .setLifecycleOwner(this)
                .build();

        adapter = new ChatAdapter(options, currentUserId);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        chatRecyclerView.setLayoutManager(layoutManager);
        chatRecyclerView.setAdapter(adapter);

        // Auto scroll to bottom when new messages arrive
        adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                scrollToBottom();
            }
        });
    }

    private Query buildQuery() {
        if ("private".equals(chatType)) {
            if (TextUtils.isEmpty(conversationId)) {
                Log.e(TAG, "Conversation ID is null for private chat");
                return null;
            }
            return db.collection("conversations").document(conversationId)
                    .collection("messages").orderBy("timestamp", Query.Direction.ASCENDING);
        } else {
            if (TextUtils.isEmpty(currentMessId)) {
                Log.e(TAG, "MessId is null for group chat");
                return null;
            }
            return db.collection("messages")
                    .whereEqualTo("messId", currentMessId)
                    .orderBy("timestamp", Query.Direction.ASCENDING);
        }
    }

    private void scrollToBottom() {
        if (adapter != null && adapter.getItemCount() > 0) {
            chatRecyclerView.scrollToPosition(adapter.getItemCount() - 1);
        }
    }

    private void sendMessage() {
        String messageText = etMessage.getText().toString().trim();
        if (TextUtils.isEmpty(messageText)) {
            return;
        }

        // Disable send button temporarily to prevent spam
        btnSendMessage.setEnabled(false);

        if ("private".equals(chatType)) {
            sendPrivateMessage(messageText);
        } else {
            sendGroupMessage(messageText);
        }
    }

    private void sendPrivateMessage(String messageText) {
        if (TextUtils.isEmpty(conversationId)) {
            Log.e(TAG, "Cannot send private message: conversationId is null");
            enableSendButton();
            return;
        }

        ChatMessage chatMessage = new ChatMessage(messageText, currentUserName, currentUserId, null);
        db.collection("conversations").document(conversationId)
                .collection("messages")
                .add(chatMessage)
                .addOnSuccessListener(documentReference -> {
                    etMessage.setText("");
                    enableSendButton();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to send private message", e);
                    Toast.makeText(this, "Failed to send message", Toast.LENGTH_SHORT).show();
                    enableSendButton();
                });
    }

    private void sendGroupMessage(String messageText) {
        if (TextUtils.isEmpty(currentMessId)) {
            Log.e(TAG, "Cannot send group message: messId is null");
            enableSendButton();
            return;
        }

        ChatMessage chatMessage = new ChatMessage(messageText, currentUserName, currentUserId, currentMessId);
        db.collection("messages")
                .add(chatMessage)
                .addOnSuccessListener(documentReference -> {
                    etMessage.setText("");
                    enableSendButton();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to send group message", e);
                    Toast.makeText(this, "Failed to send message", Toast.LENGTH_SHORT).show();
                    enableSendButton();
                });
    }

    private void enableSendButton() {
        btnSendMessage.setEnabled(true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        shouldUpdateTimestamp = true;
        Log.d(TAG, "Chat resumed - enabling timestamp updates");
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Chat থেকে বের হওয়ার সময় timestamp update করুন
        updateLastReadTimestamp();
        shouldUpdateTimestamp = false;
        Log.d(TAG, "Chat paused - timestamp updated and disabled further updates");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Chat destroyed");
    }

    @Override
    public void finish() {
        // Activity finish হওয়ার আগে timestamp update করুন
        updateLastReadTimestamp();
        super.finish();
    }

    @Override
    public void onBackPressed() {
        // Back button press করলে timestamp update করুন
        updateLastReadTimestamp();
        super.onBackPressed();
    }
}