package com.oceanbyte.navimate.view.fragments.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.oceanbyte.navimate.R;
import com.oceanbyte.navimate.models.JobReport;
import com.oceanbyte.navimate.models.WeeklyReportItem;

import java.util.List;

public class WeeklyReportAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_REPORT = 1;

    public interface OnReportClickListener {
        void onReportClick(JobReport report);
    }

    private final List<Object> items;
    private final OnReportClickListener listener;

    public WeeklyReportAdapter(List<Object> items, OnReportClickListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        if (items.get(position) instanceof String) return TYPE_HEADER;
        return TYPE_REPORT;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == TYPE_HEADER) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_week_header, parent, false);
            return new HeaderViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_report, parent, false);
            return new ReportViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof HeaderViewHolder) {
            ((HeaderViewHolder) holder).bind((String) items.get(position));
        } else {
            ((ReportViewHolder) holder).bind((JobReport) items.get(position));
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class HeaderViewHolder extends RecyclerView.ViewHolder {
        TextView title;
        HeaderViewHolder(View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.weekTitle);
        }
        void bind(String week) {
            title.setText(week);
        }
    }

    class ReportViewHolder extends RecyclerView.ViewHolder {
        TextView jobTitle, equipment;

        ReportViewHolder(View itemView) {
            super(itemView);
            jobTitle = itemView.findViewById(R.id.textJobTitle);
            equipment = itemView.findViewById(R.id.textEquipment);
            itemView.setOnClickListener(v -> {
                JobReport report = (JobReport) items.get(getAdapterPosition());
                listener.onReportClick(report);
            });
        }

        void bind(JobReport report) {
            jobTitle.setText(report.jobTitle);
            equipment.setText(report.equipmentName);
        }
    }
}
