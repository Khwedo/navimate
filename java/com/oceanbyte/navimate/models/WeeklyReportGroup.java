package com.oceanbyte.navimate.models;

import androidx.annotation.NonNull;

import java.util.List;

public class WeeklyReportGroup {

    @NonNull
    private final String groupTitle;

    @NonNull
    private final List<JobReport> reports;

    public WeeklyReportGroup(@NonNull String groupTitle, @NonNull List<JobReport> reports) {
        this.groupTitle = groupTitle;
        this.reports = reports;
    }

    public String getGroupTitle() {
        return groupTitle;
    }

    public List<JobReport> getReports() {
        return reports;
    }
}
