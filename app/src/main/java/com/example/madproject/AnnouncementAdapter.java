package com.example.madproject;

import android.graphics.Color;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AnnouncementAdapter extends RecyclerView.Adapter<AnnouncementAdapter.AnnouncementViewHolder> {

    private static final String TAG = "AnnouncementAdapter";
    private List<Announcement> announcements;

    public AnnouncementAdapter(List<Announcement> announcements) {
        this.announcements = announcements != null ? announcements : new ArrayList<>();
        Log.d(TAG, "Adapter initialized with " + this.announcements.size() + " items");
    }

    @NonNull
    @Override
    public AnnouncementViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_announcement, parent, false);
        return new AnnouncementViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AnnouncementViewHolder holder, int position) {
        Announcement a = announcements.get(position);

        // Title
        holder.tvTitle.setText(!TextUtils.isEmpty(a.getTitle()) ? a.getTitle() : "Untitled Announcement");

        // Message
        holder.tvMessage.setText(!TextUtils.isEmpty(a.getMessage()) ? a.getMessage() : "No description available");

        // Posted by
        String postedBy;
        if (!TextUtils.isEmpty(a.getPosted_by_name()) && !TextUtils.isEmpty(a.getPosted_by_role())) {
            postedBy = "Posted by: " + a.getPosted_by_name() + " (" + a.getPosted_by_role() + ")";
        } else if (!TextUtils.isEmpty(a.getPosted_by())) {
            postedBy = "Posted by: " + a.getPosted_by();
        } else {
            postedBy = "Posted by: Unknown";
        }
        holder.tvPostedBy.setText(postedBy);

        // Date
        Timestamp date = a.getDate();
        if (date != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault());
            holder.tvDate.setText(sdf.format(date.toDate()));
        } else {
            holder.tvDate.setText("Date unavailable");
        }

        // Priority indicator
        if (holder.priorityIndicator != null) {
            String priority = a.getPriority();
            if ("high".equalsIgnoreCase(priority)) {
                holder.priorityIndicator.setBackgroundColor(Color.parseColor("#D73A31"));
            } else if ("medium".equalsIgnoreCase(priority)) {
                holder.priorityIndicator.setBackgroundColor(Color.parseColor("#FF9800"));
            } else {
                holder.priorityIndicator.setBackgroundColor(Color.parseColor("#4CAF50"));
            }
        }
    }

    @Override
    public int getItemCount() {
        return announcements != null ? announcements.size() : 0;
    }

    public void updateList(List<Announcement> newAnnouncements) {
        if (newAnnouncements != null) {
            this.announcements.clear();
            this.announcements.addAll(newAnnouncements);
            notifyDataSetChanged();
        }
    }

    static class AnnouncementViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvMessage, tvPostedBy, tvDate;
        View priorityIndicator;

        public AnnouncementViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvAnnouncementTitle);
            tvMessage = itemView.findViewById(R.id.tvAnnouncementMessage);
            tvPostedBy = itemView.findViewById(R.id.tvPostedBy);
            tvDate = itemView.findViewById(R.id.tvAnnouncementDate);
            priorityIndicator = itemView.findViewById(R.id.priorityIndicator);
        }
    }
}
