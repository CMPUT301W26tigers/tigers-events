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

    /**
     * Callback invoked when a comment should be deleted. Only the event creator and
     * admins are shown the delete button, so callers can trust this is an authorised action.
     */
    public interface OnCommentDeleteListener {
        /**
         * Called when the delete button for the given comment is pressed.
         *
         * @param comment The {@link Comment} to remove.
         */
        void onDeleteComment(Comment comment);
    }

    private List<Comment> commentList;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
    private String eventCreatorId;
    private String currentUserId;
    private String currentUserAccountType;
    private OnCommentDeleteListener deleteListener;

    /**
     * Constructs a CommentAdapter with the given list of comments.
     * Call the setter methods to enable delete functionality and host-tag highlighting
     * before attaching this adapter to a RecyclerView.
     *
     * @param commentList The initial list of {@link Comment} objects to display.
     */
    public CommentAdapter(List<Comment> commentList) {
        this.commentList = commentList;
    }

    /**
     * Registers the listener that is invoked when the delete button on a comment is pressed.
     *
     * @param listener The delete callback, or {@code null} to disable deletion handling.
     */
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

    /**
     * Inflates the comment item layout and wraps it in a {@link CommentViewHolder}.
     *
     * @param parent   The parent ViewGroup into which the new view will be added.
     * @param viewType The view type of the new view (unused; only one type exists).
     * @return A new {@link CommentViewHolder} backed by the inflated item view.
     */
    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_comment, parent, false);
        return new CommentViewHolder(view);
    }

    /**
     * Binds a {@link Comment} to the provided {@link CommentViewHolder}. Displays the author name,
     * comment body, and formatted timestamp. Shows an "EventHost" badge when the commenter is the
     * event creator, and shows the delete button only for the event creator or an admin user.
     *
     * @param holder   The ViewHolder to update.
     * @param position The position of the item within the data set.
     */
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

    /**
     * Returns the total number of comments managed by this adapter.
     *
     * @return The size of the comment list.
     */
    @Override
    public int getItemCount() {
        return commentList.size();
    }

    /**
     * ViewHolder for a single comment row.
     * Holds references to the author name, comment text, timestamp, host badge, and delete
     * button views defined in {@code item_comment.xml}.
     */
    static class CommentViewHolder extends RecyclerView.ViewHolder {
        TextView tvUserName, tvCommentText, tvTimestamp, tvHostTag;
        ImageButton btnDelete;

        /**
         * Constructs a CommentViewHolder and resolves all child view references.
         *
         * @param itemView The inflated comment item view.
         */
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
