package com.example.eventsapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
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

    public interface OnCommentDeleteListener {
        void onDeleteComment(Comment comment);
    }

    private List<Comment> commentList;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
    private String eventCreatorId;
    private String currentUserId;
    private String currentUserAccountType;
    private OnCommentDeleteListener deleteListener;

    public CommentAdapter(List<Comment> commentList) {
        this.commentList = commentList;
    }

    public void setOnCommentDeleteListener(OnCommentDeleteListener listener) {
        this.deleteListener = listener;
    }

    /**
     * Sets the creator ID of the current event to identify host comments and permissions.
     * @param eventCreatorId The ID of the user who created the event.
     */
    public void setEventCreatorId(String eventCreatorId) {
        this.eventCreatorId = eventCreatorId;
        notifyDataSetChanged();
    }

    /**
     * Sets the current user ID to determine permissions.
     * @param currentUserId The ID of the logged-in user.
     */
    public void setCurrentUserId(String currentUserId) {
        this.currentUserId = currentUserId;
        notifyDataSetChanged();
    }

    /**
     * Sets the current user's account type to determine permissions.
     * @param accountType The account type of the logged-in user (e.g., "Admin").
     */
    public void setCurrentUserAccountType(String accountType) {
        this.currentUserAccountType = accountType;
        notifyDataSetChanged();
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

        // Show "EventHost" tag if the comment user is the event creator
        if (eventCreatorId != null && eventCreatorId.equals(comment.getUserId())) {
            holder.tvHostTag.setVisibility(View.VISIBLE);
        } else {
            holder.tvHostTag.setVisibility(View.GONE);
        }

        // Show delete button if current user is the event creator OR is an Admin
        boolean isAdmin = "Admin".equalsIgnoreCase(currentUserAccountType);
        boolean isCreator = currentUserId != null && currentUserId.equals(eventCreatorId);

        if (isAdmin || isCreator) {
            holder.btnDelete.setVisibility(View.VISIBLE);
            holder.btnDelete.setOnClickListener(v -> {
                if (deleteListener != null) {
                    deleteListener.onDeleteComment(comment);
                }
            });
        } else {
            holder.btnDelete.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return commentList.size();
    }

    static class CommentViewHolder extends RecyclerView.ViewHolder {
        TextView tvUserName, tvCommentText, tvTimestamp, tvHostTag;
        ImageButton btnDelete;

        public CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUserName = itemView.findViewById(R.id.tv_comment_user);
            tvCommentText = itemView.findViewById(R.id.tv_comment_text);
            tvTimestamp = itemView.findViewById(R.id.tv_comment_timestamp);
            tvHostTag = itemView.findViewById(R.id.tv_event_host_tag);
            btnDelete = itemView.findViewById(R.id.btn_delete_comment);
        }
    }
}
