package com.oceanbyte.navimate.models;

import androidx.annotation.Nullable;

import com.oceanbyte.navimate.utils.ReportUtils;

// Класс для универсального списка: заголовки недель и сами отчёты
public class ReportListItem {

    public final boolean isHeader;
    @Nullable
    public final String header;

    @Nullable
    public final JobReport report;
    public final boolean isToday;

    // Конструктор для заголовка недели
    public ReportListItem(String header) {
        this.isHeader = true;
        this.header = header;
        this.report = null;
        this.isToday = false;
    }

    // Конструктор для обычного отчёта
    public ReportListItem(JobReport report) {
        this.isHeader = false;
        this.header = null;
        this.report = report;
        this.isToday = ReportUtils.isToday(report.reportDate);
    }

}
