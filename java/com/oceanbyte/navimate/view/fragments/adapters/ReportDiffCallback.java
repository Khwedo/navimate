package com.oceanbyte.navimate.view.fragments.adapters;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;

import com.oceanbyte.navimate.models.ReportListItem;

import java.util.List;

public class ReportDiffCallback extends DiffUtil.Callback {

    private final List<ReportListItem> oldList;
    private final List<ReportListItem> newList;

    public ReportDiffCallback(List<ReportListItem> oldList, List<ReportListItem> newList) {
        this.oldList = oldList;
        this.newList = newList;
    }

    @Override
    public int getOldListSize() {
        return oldList.size();
    }

    @Override
    public int getNewListSize() {
        return newList.size();
    }

    @Override
    public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
        ReportListItem oldItem = oldList.get(oldItemPosition);
        ReportListItem newItem = newList.get(newItemPosition);

        // Если оба — заголовки, сравниваем текст
        if (oldItem.isHeader && newItem.isHeader) {
            return oldItem.header.equals(newItem.header);
        }

        // Если оба — отчёты, сравниваем ID
        if (!oldItem.isHeader && !newItem.isHeader) {
            return oldItem.report.id == newItem.report.id;
        }

        return false;
    }

    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        ReportListItem oldItem = oldList.get(oldItemPosition);
        ReportListItem newItem = newList.get(newItemPosition);

        return oldItem.equals(newItem);
    }
}
