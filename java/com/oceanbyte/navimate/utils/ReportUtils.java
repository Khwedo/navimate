package com.oceanbyte.navimate.utils;

import android.util.Log;

import com.oceanbyte.navimate.models.JobReport;
import com.oceanbyte.navimate.models.ReportListItem;
import org.threeten.bp.format.TextStyle;
import org.threeten.bp.LocalDate;
import org.threeten.bp.format.DateTimeFormatter;
import org.threeten.bp.format.DateTimeParseException;
import org.threeten.bp.temporal.WeekFields;

import java.util.*;

/**
 * Утилита для группировки JobReport по неделям с локализованными заголовками.
 * Пример заголовка: "Май • неделя 22 (2025)"
 */
public class ReportUtils {

    private static final String TAG = "ReportUtils";
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.getDefault());

    /**
     * Группирует отчёты по неделям с учётом локали.
     */
    public static List<ReportListItem> groupReportsByWeek(List<JobReport> reports, Locale locale) {
        List<ReportListItem> result = new ArrayList<>();

        Map<String, List<JobReport>> weeklyMap = new TreeMap<>(Collections.reverseOrder());
        Map<String, String> weekLabels = new HashMap<>();

        WeekFields weekFields = WeekFields.of(locale);

        for (JobReport report : reports) {
            try {
                LocalDate date = LocalDate.parse(report.reportDate, formatter);
                int week = date.get(weekFields.weekOfWeekBasedYear());
                int year = date.get(weekFields.weekBasedYear());

                String weekKey = year + "-W" + String.format(locale, "%02d", week);
                String weekLabel = weekLabels.computeIfAbsent(weekKey,
                        k -> generateLocalizedLabel(date, locale, week, year));

                weeklyMap.computeIfAbsent(weekKey, k -> new ArrayList<>()).add(report);

            } catch (DateTimeParseException e) {
                Log.e(TAG, "Ошибка парсинга даты отчёта: " + report.reportDate, e);
            }
        }

        for (Map.Entry<String, List<JobReport>> entry : weeklyMap.entrySet()) {
            String weekKey = entry.getKey();
            String localizedLabel = weekLabels.getOrDefault(weekKey, weekKey);
            result.add(new ReportListItem(localizedLabel));

            List<JobReport> weekReports = entry.getValue();
            weekReports.sort((a, b) -> {
                int cmp = b.reportDate.compareTo(a.reportDate);
                return (cmp != 0) ? cmp : Long.compare(b.createdAt, a.createdAt);
            });

            for (JobReport r : weekReports) {
                result.add(new ReportListItem(r));
            }
        }

        return result;
    }

    private static String generateLocalizedLabel(LocalDate date, Locale locale, int week, int year) {
        String monthName = date.getMonth().getDisplayName(TextStyle.FULL, locale);
        if (!monthName.isEmpty()) {
            monthName = monthName.substring(0, 1).toUpperCase(locale) + monthName.substring(1);
        }
        return String.format(locale, "%s • неделя %02d (%d)", monthName, week, year);
    }

    /**
     * Возвращает локализованный заголовок недели по дате в формате "yyyy-MM-dd"
     */
    public static String getWeekTitle(String dateString) {
        try {
            LocalDate date = LocalDate.parse(dateString, formatter);
            Locale locale = Locale.getDefault();
            int week = date.get(WeekFields.of(locale).weekOfWeekBasedYear());
            int year = date.get(WeekFields.of(locale).weekBasedYear());
            return generateLocalizedLabel(date, locale, week, year);
        } catch (DateTimeParseException e) {
            Log.e(TAG, "Ошибка получения заголовка недели: " + dateString, e);
            return "";
        }
    }

    /**
     * Проверяет, относится ли дата к текущей неделе (по строке)
     */
    public static boolean isCurrentWeek(String dateString) {
        try {
            LocalDate reportDate = LocalDate.parse(dateString, formatter);
            LocalDate today = LocalDate.now();
            WeekFields weekFields = WeekFields.of(Locale.getDefault());
            return reportDate.get(weekFields.weekOfWeekBasedYear()) == today.get(weekFields.weekOfWeekBasedYear())
                    && reportDate.get(weekFields.weekBasedYear()) == today.get(weekFields.weekBasedYear());
        } catch (DateTimeParseException e) {
            Log.e(TAG, "Ошибка проверки недели: " + dateString, e);
            return false;
        }
    }

    /**
     * Проверяет, является ли дата сегодняшним днём
     */
    public static boolean isToday(String dateString) {
        try {
            LocalDate reportDate = LocalDate.parse(dateString, formatter);
            LocalDate today = LocalDate.now();
            return reportDate.equals(today);
        } catch (DateTimeParseException e) {
            Log.e(TAG, "Ошибка при проверке isToday: " + dateString, e);
            return false;
        }
    }
}
