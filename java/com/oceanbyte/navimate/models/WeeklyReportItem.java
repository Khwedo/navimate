package com.oceanbyte.navimate.models;

import java.util.List;

public class WeeklyReportItem {
    public String weekTitle;
    public List<JobReport> reports;

    public WeeklyReportItem(String weekTitle, List<JobReport> reports) {
        this.weekTitle = weekTitle;
        this.reports = reports;
    }
}
