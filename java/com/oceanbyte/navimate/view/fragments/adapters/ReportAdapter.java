package com.oceanbyte.navimate.view.fragments.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.oceanbyte.navimate.R;
import com.oceanbyte.navimate.models.JobReport;
import com.oceanbyte.navimate.models.ReportListItem;
import com.oceanbyte.navimate.utils.ReportUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ReportAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0; // TYPE_HEADER — заголовок недели, например 03 — 09 June
    private static final int TYPE_ITEM = 1;   // TYPE_ITEM — обычный отчёт

    private final List<ReportListItem> items = new ArrayList<>(); // ReportListItem — это универсальный объект:
    private final Set<Long> selectedIds = new HashSet<>();

    /** Это колбэки, через которые Fragment узнаёт:

 какой отчёт был нажат;

 добавили ли отчёт;

 нажали ли долго;

 изменилось ли число выделенных отчётов.*/


    private final OnReportClickListener listener;
    private OnAddReportClickListener onAddReportClickListener;
    private OnReportLongClickListener longClickListener;
    private OnSelectionChangedListener selectionChangedListener;

    private boolean selectionEnabled = false;

    public interface OnReportClickListener {
        void onReportClick(JobReport report);
    }

    public interface OnAddReportClickListener {
        void onAddReportClick(String weekTitle);
    }

    public interface OnReportLongClickListener {
        void onLongClick();
    }

    public ReportAdapter(List<ReportListItem> groupedItems, OnReportClickListener listener) {
        this.items.addAll(groupedItems);
        this.listener = listener;
    }

    public void setOnAddReportClickListener(OnAddReportClickListener listener) {
        this.onAddReportClickListener = listener;
    }

    public void setOnReportLongClickListener(OnReportLongClickListener listener) {
        this.longClickListener = listener;
    }

    public void setOnSelectionChangedListener(OnSelectionChangedListener listener) {
        this.selectionChangedListener = listener;
    }
/**DiffUtil — это умный способ обновить список без моргания.
 * Он считает, какие элементы изменились, удалились, добавились, и обновляет только их.*/

    public void setItems(List<ReportListItem> newItems) {
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new ReportDiffCallback(this.items, newItems));
        this.items.clear(); // 👈 переносим вниз
        diffResult.dispatchUpdatesTo(this); // 👈 сначала обновляем адаптер
        this.items.addAll(newItems);       // 👈 только потом обновляем данные
    }

    public List<JobReport> getSelectedReports() {
        List<JobReport> selectedReports = new ArrayList<>();
        for (ReportListItem item : items) {
            if (!item.isHeader && item.report != null && selectedIds.contains(item.report.id)) {
                selectedReports.add(item.report);
            }
        }
        return selectedReports;
    }
/** Добавляет/удаляет ID*/
    public void toggleSelection(long reportId) {
        boolean changed;
        if (selectedIds.contains(reportId)) {
            selectedIds.remove(reportId);
            changed = true;
        } else {
            selectedIds.add(reportId);
            changed = true;
        }

        if (changed) {
            if (selectionChangedListener != null) {
                selectionChangedListener.onSelectionChanged(selectedIds.size());
            }
        }

        // ВАЖНО: флаг должен обновляться после колбэка
        setSelectionEnabled(!selectedIds.isEmpty());
    }
    /**Очищает выбор*/
    public void clearSelections() {
        selectedIds.clear();
        setSelectionEnabled(false);
    }

    public boolean hasSelection() {
        return !selectedIds.isEmpty();
    }

    public Set<Long> getSelectedIds() {
        return new HashSet<>(selectedIds);
    }

    public void restoreSelections(Set<Long> savedIds) {
        if (savedIds != null) {
            selectedIds.clear();
            selectedIds.addAll(savedIds);
            setSelectionEnabled(!selectedIds.isEmpty());
        }
    }

    public void setSelectionEnabled(boolean enabled) {
        this.selectionEnabled = enabled;
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position).isHeader ? TYPE_HEADER : TYPE_ITEM;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_HEADER) {
            View view = inflater.inflate(R.layout.item_week_header, parent, false);
            return new HeaderViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.item_report, parent, false);
            return new ReportViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ReportListItem item = items.get(position);

        if (holder instanceof HeaderViewHolder) {
            ((HeaderViewHolder) holder).bind(item.header, v -> {
                if (onAddReportClickListener != null) {
                    onAddReportClickListener.onAddReportClick(item.header);
                }
            });
        } else if (holder instanceof ReportViewHolder) {
            JobReport report = item.report;
            ReportViewHolder viewHolder = (ReportViewHolder) holder;

            viewHolder.textEquipment.setText(report.equipmentName);
            viewHolder.textJobTitle.setText(report.jobTitle);
            viewHolder.textDate.setText(report.getFormattedDate());

            viewHolder.checkSelect.setOnCheckedChangeListener(null);
            boolean isSelected = selectedIds.contains(report.id);
            viewHolder.checkSelect.setChecked(isSelected);

            viewHolder.itemView.setSelected(isSelected);

            viewHolder.checkSelect.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (report.id != 0) {
                    toggleSelection(report.id);
                    notifyItemChanged(holder.getBindingAdapterPosition());
                }
            });
            if (ReportUtils.isToday(report.reportDate)) {
                viewHolder.textToday.setVisibility(View.VISIBLE);
            } else {
                viewHolder.textToday.setVisibility(View.GONE);
            }

            viewHolder.itemView.setOnClickListener(v -> {
                if (selectionEnabled) {
                    toggleSelection(report.id);
                    notifyItemChanged(holder.getBindingAdapterPosition());
                } else if (listener != null) {
                    listener.onReportClick(report);
                }
            });

            viewHolder.itemView.setOnLongClickListener(v -> {
                if (report.id != 0) {
                    toggleSelection(report.id);

                    int selectedCount = getSelectedReports().size();

                    if (selectionChangedListener != null) {
                        selectionChangedListener.onSelectionChanged(selectedCount);
                    }

                    // ВАЖНО: запускать longClickListener только если что-то выбрано
                    if (selectedCount > 0 && longClickListener != null) {
                        longClickListener.onLongClick();
                    }

                    notifyItemChanged(holder.getBindingAdapterPosition());
                    return true;
                }
                return false;
            });
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        TextView textHeader;
        View btnAdd;

        public HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            textHeader = itemView.findViewById(R.id.weekTitle);
        }

        public void bind(String weekTitle, View.OnClickListener onAddClick) {
            textHeader.setText(weekTitle);
            if (btnAdd != null) {
                btnAdd.setOnClickListener(onAddClick);
            }
        }
    }

    static class ReportViewHolder extends RecyclerView.ViewHolder {
        TextView textEquipment, textJobTitle, textDate, textToday;
        CheckBox checkSelect;


        public ReportViewHolder(@NonNull View itemView) {
            super(itemView);
            textEquipment = itemView.findViewById(R.id.textEquipment);
            textJobTitle = itemView.findViewById(R.id.textJobTitle);
            textDate = itemView.findViewById(R.id.textDate);
            checkSelect = itemView.findViewById(R.id.checkSelect);
            textToday = itemView.findViewById(R.id.textToday);
        }
    }

    public int getFirstCurrentWeekIndex() {
        for (int i = 0; i < items.size(); i++) {
            ReportListItem item = items.get(i);
            if (!item.isHeader && item.report != null && ReportUtils.isCurrentWeek(item.report.reportDate)) {
                return i;
            }
        }
        return -1;
    }
    public interface OnSelectionChangedListener {
        void onSelectionChanged(int count);
    }
    public void appendItems(List<ReportListItem> newItems) {
        int startPos = items.size();
        items.addAll(newItems);
        notifyItemRangeInserted(startPos, newItems.size());
    }


}
