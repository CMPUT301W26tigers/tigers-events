package com.example.eventsapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

/**
 * Adapter for displaying a list of comments in a RecyclerView within the Event view.
 */
public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.CommentViewHolder> {

    private List<Comment> commentList;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());

    public CommentAdapter(List<Comment> commentList) {
        this.commentList = commentList;
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_comment, parent, false);
        return new CommentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        Comment comment = commentList.get(position);
        holder.tvUserName.setText(comment.getUserName());
        holder.tvCommentText.setText(comment.getText());
        if (comment.getTimestamp() != null) {
            holder.tvTimestamp.setText(dateFormat.format(comment.getTimestamp()));
        }
    }

    @Override
    public int getItemCount() {
        return commentList.size();
    }

    static class CommentViewHolder extends RecyclerView.ViewHolder {
        TextView tvUserName, tvCommentText, tvTimestamp;

        public CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUserName = itemView.findViewById(R.id.tv_comment_user);
            tvCommentText = itemView.findViewById(R.id.tv_comment_text);
            tvTimestamp = itemView.findViewById(R.id.tv_comment_timestamp);
        }
    }
}
