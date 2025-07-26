package com.oceanbyte.navimate.models;

import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import com.oceanbyte.navimate.database.Converters;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

@Entity
@TypeConverters(Converters.class)
public class JobReport {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public String equipmentName;
    public String jobTitle;

    @ColumnInfo(name = "report_date")
    public String reportDate; // "yyyy-MM-dd"

    @ColumnInfo(name = "before_photos")
    public List<String> beforePhotos;

    @ColumnInfo(name = "after_photos")
    public List<String> afterPhotos;

    @ColumnInfo(name = "created_at")
    public long createdAt; // timestamp

    @ColumnInfo(name = "contract_id")
    public int contractId;

    @ColumnInfo(name = "user_note")
    @Nullable
    public String userNote; // Заметка пользователя (не экспортируется)

    @ColumnInfo(name = "reminder_time")
    public long reminderTime; // Время напоминания (в мс), 0 если не установлено

    // ======== Формат даты ========
    private static final ThreadLocal<SimpleDateFormat> sdf =
            ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()));

    private static final ThreadLocal<SimpleDateFormat> outputDateFormat =
            ThreadLocal.withInitial(() -> new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()));

    // ======== Конструкторы ========
    public JobReport() {
        // Обязателен для Room
    }

    @Ignore
    public JobReport(String jobTitle, String equipmentName, List<String> beforePhotos, List<String> afterPhotos) {
        this.jobTitle = jobTitle;
        this.equipmentName = equipmentName;
        this.beforePhotos = beforePhotos;
        this.afterPhotos = afterPhotos;
        this.reportDate = sdf.get().format(new Date());
        this.createdAt = System.currentTimeMillis();
        this.reminderTime = 0; // по умолчанию не задан
    }

    @Ignore
    public JobReport(String jobTitle, String equipmentName, List<String> beforePhotos, List<String> afterPhotos, int contractId) {
        this(jobTitle, equipmentName, beforePhotos, afterPhotos);
        this.contractId = contractId;
    }

    // ======== Методы ========
    public int getWeekOfYear() {
        try {
            Date date = sdf.get().parse(reportDate);
            Calendar calendar = Calendar.getInstance();
            calendar.setFirstDayOfWeek(Calendar.MONDAY);
            calendar.setTime(date);
            return calendar.get(Calendar.WEEK_OF_YEAR);
        } catch (ParseException e) {
            e.printStackTrace();
            return -1;
        }
    }

    public int getYear() {
        try {
            Date date = sdf.get().parse(reportDate);
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);
            return calendar.get(Calendar.YEAR);
        } catch (ParseException e) {
            return -1;
        }
    }

    public String getFormattedDate() {
        try {
            Date date = sdf.get().parse(reportDate);
            return outputDateFormat.get().format(date);
        } catch (ParseException e) {
            return reportDate;
        }
    }

    @Override
    public String toString() {
        return "JobReport{" +
                "id=" + id +
                ", equipmentName='" + equipmentName + '\'' +
                ", jobTitle='" + jobTitle + '\'' +
                ", reportDate='" + reportDate + '\'' +
                ", contractId=" + contractId +
                ", note='" + userNote + '\'' +
                ", reminderTime=" + reminderTime +
                '}';
    }
}
