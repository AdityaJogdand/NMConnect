package com.example.madproject;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class CalendarAdapter extends RecyclerView.Adapter<CalendarAdapter.CalendarViewHolder> {

    private List<Calendar> dateList;
    private int todayPosition = -1;

    public CalendarAdapter(List<Calendar> dateList) {
        this.dateList = dateList;

        // Find today's position
        Calendar today = Calendar.getInstance();
        for (int i = 0; i < dateList.size(); i++) {
            Calendar cal = dateList.get(i);
            if (cal.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                    cal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)) {
                todayPosition = i;
                break;
            }
        }
    }

    @NonNull
    @Override
    public CalendarViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_calendar_date, parent, false);
        return new CalendarViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CalendarViewHolder holder, int position) {
        Calendar date = dateList.get(position);

        SimpleDateFormat dayFormat = new SimpleDateFormat("EEE", Locale.getDefault());
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd", Locale.getDefault());

        String dayName = dayFormat.format(date.getTime()).toUpperCase();
        String dateNumber = dateFormat.format(date.getTime());

        holder.tvDay.setText(dayName);
        holder.tvDate.setText(dateNumber);

        // Check if this is today's date
        if (position == todayPosition) {
            // Today's date - red background with white text
            holder.cardView.setCardBackgroundColor(Color.parseColor("#D73A31"));
            holder.tvDay.setTextColor(Color.WHITE);
            holder.tvDate.setTextColor(Color.WHITE);
        } else {
            // Normal date - white background with dark text
            holder.cardView.setCardBackgroundColor(Color.WHITE);
            holder.tvDay.setTextColor(Color.parseColor("#757575"));
            holder.tvDate.setTextColor(Color.parseColor("#000000"));
        }
    }

    @Override
    public int getItemCount() {
        return dateList.size();
    }

    static class CalendarViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        TextView tvDay, tvDate;

        public CalendarViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardDate);
            tvDay = itemView.findViewById(R.id.tvDay);
            tvDate = itemView.findViewById(R.id.tvDate);
        }
    }
}
// ✅ CalendarHelper.java
class CalendarHelper {
    public static List<Calendar> generateCalendarDates(int daysToShow) {
        List<Calendar> dates = new java.util.ArrayList<>();
        Calendar calendar = Calendar.getInstance();

        // Start from 3 days before today
        calendar.add(Calendar.DAY_OF_MONTH, -3);

        for (int i = 0; i < daysToShow; i++) {
            Calendar cal = (Calendar) calendar.clone();
            dates.add(cal);
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }

        return dates;
    }
}
