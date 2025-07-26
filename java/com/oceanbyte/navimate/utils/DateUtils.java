package com.oceanbyte.navimate.utils;

import android.util.Log;

import org.threeten.bp.Instant;
import org.threeten.bp.LocalDate;
import org.threeten.bp.ZoneId;
import org.threeten.bp.ZonedDateTime;
import org.threeten.bp.format.DateTimeFormatter;
import org.threeten.bp.format.DateTimeParseException;
import org.threeten.bp.temporal.WeekFields;

import java.util.Locale;

/**
 * Утильный класс для работы с датами на базе ThreeTenABP (совместимо с API < 26).
 */
public class DateUtils {

    private static final String TAG = "DateUtils";
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.getDefault());

    /**
     * Преобразует строку в миллисекунды (Long)
     */
    public static Long parseDate(String dateStr) {
        try {
            LocalDate date = LocalDate.parse(dateStr, formatter);
            return date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        } catch (DateTimeParseException e) {
            Log.e(TAG, "Ошибка парсинга даты: " + dateStr, e);
            return null;
        }
    }

    /**
     * Преобразует миллисекунды в строку формата yyyy-MM-dd
     */
    public static String formatDate(Long millis) {
        if (millis == null) return "";
        LocalDate date = ZonedDateTime.ofInstant(
                Instant.ofEpochMilli(millis), ZoneId.systemDefault()).toLocalDate();
        return date.format(formatter);
    }

    /**
     * Возвращает текущую дату в формате yyyy-MM-dd
     */
    public static String getTodayFormatted() {
        return LocalDate.now().format(formatter);
    }

    /**
     * Возвращает миллисекунды по указанной дате
     */
    public static long toMillis(int year, int month, int day) {
        LocalDate date = LocalDate.of(year, month + 1, day); // month: 0-based в Android, 1-based в Java
        return date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    /**
     * Возвращает номер недели года (1–53) по локали пользователя
     */
    public static int getWeekOfYear(long millis) {
        LocalDate date = ZonedDateTime.ofInstant(
                Instant.ofEpochMilli(millis), ZoneId.systemDefault()).toLocalDate();
        WeekFields weekFields = WeekFields.of(Locale.getDefault());
        return date.get(weekFields.weekOfWeekBasedYear());
    }

    /**
     * Возвращает год недели (может отличаться от календарного в начале и конце года)
     */
    public static int getWeekYear(long millis) {
        LocalDate date = ZonedDateTime.ofInstant(
                Instant.ofEpochMilli(millis), ZoneId.systemDefault()).toLocalDate();
        WeekFields weekFields = WeekFields.of(Locale.getDefault());
        return date.get(weekFields.weekBasedYear());
    }
}
