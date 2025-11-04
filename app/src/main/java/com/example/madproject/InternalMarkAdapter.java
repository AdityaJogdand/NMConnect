package com.example.madproject;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class InternalMarkAdapter extends RecyclerView.Adapter<InternalMarkAdapter.MarkViewHolder> {

    private List<InternalMark> marksList;
    private Context context;

    public InternalMarkAdapter(List<InternalMark> marksList) {
        this.marksList = marksList != null ? marksList : new ArrayList<>();
    }

    @NonNull
    @Override
    public MarkViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_internal_mark, parent, false);
        return new MarkViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MarkViewHolder holder, int position) {
        InternalMark mark = marksList.get(position);

        // Set subject name
        holder.tvSubjectName.setText(mark.getSubject_name() != null
                ? mark.getSubject_name()
                : "N/A");

        // Set subject code if available
        if (holder.tvSubjectCode != null) {
            holder.tvSubjectCode.setText(mark.getSubject_code() != null
                    ? mark.getSubject_code()
                    : "");
        }

        // Set faculty name if available
        if (holder.tvFacultyName != null) {
            holder.tvFacultyName.setText(mark.getFaculty_name() != null
                    ? mark.getFaculty_name()
                    : "Faculty");
        }

        // Show formatted marks
        String formatted = mark.getFormattedMarks();
        holder.tvMarks.setText(formatted != null ? formatted : "0/50");

        // Set percentage with color coding
        double percentage = mark.getPercentage();
        if (holder.tvPercentage != null) {
            holder.tvPercentage.setText(String.format("%.1f%%", percentage));

            // Color based on percentage
            if (percentage >= 80) {
                holder.tvPercentage.setTextColor(context.getResources().getColor(android.R.color.holo_green_dark));
            } else if (percentage >= 60) {
                holder.tvPercentage.setTextColor(context.getResources().getColor(android.R.color.holo_orange_dark));
            } else {
                holder.tvPercentage.setTextColor(context.getResources().getColor(android.R.color.holo_red_dark));
            }
        }

        // Click listener to open SubjectDetailsActivity
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, SubjectDetailsActivity.class);

            // Get session data
            SessionManager session = new SessionManager(context);
            String branch = session.getBranch();
            String semester = session.getSemester();

            // Pass all subject data
            intent.putExtra("subject_name", mark.getSubject_name());
            intent.putExtra("subject_code", mark.getSubject_code());
            intent.putExtra("faculty_sap_id", mark.getFaculty_sap_id());
            intent.putExtra("faculty_name", mark.getFaculty_name());
            intent.putExtra("branch", branch);
            intent.putExtra("semester", semester);

            // Pass marks data
            intent.putExtra("attendance", mark.getAttendance());
            intent.putExtra("lab_exam", mark.getLab_Exam());
            intent.putExtra("mid_term1", mark.getMid_Term1());
            intent.putExtra("mid_term2", mark.getMid_Term2());
            intent.putExtra("presentation", mark.getPresentation());
            intent.putExtra("viva", mark.getViva());
            intent.putExtra("total_marks", mark.calculateTotal());
            intent.putExtra("percentage", mark.getPercentage());

            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return marksList.size();
    }

    public void updateData(List<InternalMark> newMarks) {
        if (newMarks == null) return;
        marksList.clear();
        marksList.addAll(newMarks);
        notifyDataSetChanged();
    }

    static class MarkViewHolder extends RecyclerView.ViewHolder {
        TextView tvSubjectName, tvSubjectCode, tvFacultyName, tvMarks, tvPercentage;

        public MarkViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSubjectName = itemView.findViewById(R.id.tvSubjectName);
            tvSubjectCode = itemView.findViewById(R.id.tvSubjectCode);
            tvFacultyName = itemView.findViewById(R.id.tvFacultyName);
            tvMarks = itemView.findViewById(R.id.tvMarks);
            tvPercentage = itemView.findViewById(R.id.tvPercentage);
        }
    }
}