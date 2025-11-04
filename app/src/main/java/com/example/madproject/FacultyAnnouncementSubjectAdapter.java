package com.example.madproject;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class FacultyAnnouncementSubjectAdapter extends RecyclerView.Adapter<FacultyAnnouncementSubjectAdapter.ViewHolder> {

    private List<FacultySubject> subjectList;

    public FacultyAnnouncementSubjectAdapter(List<FacultySubject> subjectList) {
        this.subjectList = subjectList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_faculty_subject_selection, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FacultySubject subject = subjectList.get(position);

        holder.tvSubjectName.setText(subject.getSubjectName());
        holder.tvSubjectCode.setText(subject.getSubjectCode());
        holder.tvBranchSemester.setText(subject.getBranch() + " - Sem " + subject.getSemester());
        holder.checkBox.setChecked(subject.isSelected());

        holder.itemView.setOnClickListener(v -> {
            subject.setSelected(!subject.isSelected());
            holder.checkBox.setChecked(subject.isSelected());
        });

        holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            subject.setSelected(isChecked);
        });
    }

    @Override
    public int getItemCount() {
        return subjectList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvSubjectName, tvSubjectCode, tvBranchSemester;
        CheckBox checkBox;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSubjectName = itemView.findViewById(R.id.tvSubjectName);
            tvSubjectCode = itemView.findViewById(R.id.tvSubjectCode);
            tvBranchSemester = itemView.findViewById(R.id.tvBranchSemester);
            checkBox = itemView.findViewById(R.id.checkBox);
        }
    }
}