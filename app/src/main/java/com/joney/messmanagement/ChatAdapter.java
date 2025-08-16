package com.joney.messmanagement;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class ChatAdapter extends FirestoreRecyclerAdapter<ChatMessage, RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_SENT = 1;
    private static final int VIEW_TYPE_RECEIVED = 2;
    private String currentUserId;

    public ChatAdapter(@NonNull FirestoreRecyclerOptions<ChatMessage> options, String currentUserId) {
        super(options);
        this.currentUserId = currentUserId;
    }

    @Override
    public int getItemViewType(int position) {
        if (getItem(position).getSenderId().equals(currentUserId)) {
            return VIEW_TYPE_SENT;
        } else {
            return VIEW_TYPE_RECEIVED;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_SENT) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_sent, parent, false);
            return new SentMessageHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_received, parent, false);
            return new ReceivedMessageHolder(view);
        }
    }

    @Override
    protected void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position, @NonNull ChatMessage model) {
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        String time = sdf.format(model.getTimestamp().toDate());

        if (getItemViewType(position) == VIEW_TYPE_SENT) {
            ((SentMessageHolder) holder).bind(model, time);
        } else {
            ((ReceivedMessageHolder) holder).bind(model, time);
        }
    }

    private static class SentMessageHolder extends RecyclerView.ViewHolder {
        TextView tvMessageText, tvMessageTime;
        SentMessageHolder(View itemView) {
            super(itemView);
            tvMessageText = itemView.findViewById(R.id.tvMessageText);
            tvMessageTime = itemView.findViewById(R.id.tvMessageTime);
        }
        void bind(ChatMessage message, String time) {
            tvMessageText.setText(message.getMessageText());
            tvMessageTime.setText(time);
        }
    }

    private static class ReceivedMessageHolder extends RecyclerView.ViewHolder {
        TextView tvMessageText, tvMessageTime, tvSenderName;
        ReceivedMessageHolder(View itemView) {
            super(itemView);
            tvMessageText = itemView.findViewById(R.id.tvMessageText);
            tvMessageTime = itemView.findViewById(R.id.tvMessageTime);
            tvSenderName = itemView.findViewById(R.id.tvSenderName);
        }
        void bind(ChatMessage message, String time) {
            tvMessageText.setText(message.getMessageText());
            tvMessageTime.setText(time);
            tvSenderName.setText(message.getSenderName());
        }
    }
}