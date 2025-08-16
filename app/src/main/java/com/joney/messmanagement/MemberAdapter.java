package com.joney.messmanagement;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class MemberAdapter extends RecyclerView.Adapter<MemberAdapter.MemberViewHolder> {

    private List<Member> memberList;
    private List<String> documentIds;

    public MemberAdapter(List<Member> memberList, List<String> documentIds) {
        this.memberList = memberList;
        this.documentIds = documentIds;
    }

    // নতুন মেথড: ডেটা আপডেট করার জন্য
    public void updateData(List<Member> newMemberList, List<String> newDocIds) {
        this.memberList.clear();
        this.memberList.addAll(newMemberList);
        this.documentIds.clear();
        this.documentIds.addAll(newDocIds);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MemberViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.member_item_layout, parent, false);
        return new MemberViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MemberViewHolder holder, int position) {
        Member currentMember = memberList.get(position);
        holder.tvMemberName.setText(currentMember.getName());
        holder.tvMemberId.setText("User ID: " + currentMember.getUserID());
    }

    @Override
    public int getItemCount() {
        return memberList.size();
    }

    class MemberViewHolder extends RecyclerView.ViewHolder {
        TextView tvMemberName;
        TextView tvMemberId;

        public MemberViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMemberName = itemView.findViewById(R.id.tvMemberName);
            tvMemberId = itemView.findViewById(R.id.tvMemberId);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    String memberDocId = documentIds.get(position);
                    Intent intent = new Intent(v.getContext(), MemberProfileActivity.class);
                    intent.putExtra("MEMBER_DOC_ID", memberDocId);
                    v.getContext().startActivity(intent);
                }
            });
        }
    }
}