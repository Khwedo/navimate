package com.oceanbyte.navimate.utils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.oceanbyte.navimate.models.JobReport;
import com.oceanbyte.navimate.notifications.ReminderReceiver;

public class ReminderUtils {

    public static void cancelReminder(Context context, JobReport report) {
        Log.d("ReminderUtils", "Попытка отмены напоминания для ID: " + report.id);
        Log.d("ReminderUtils", "Reminder time (ms): " + report.reminderTime);
        Log.d("ReminderUtils", "Current time: " + System.currentTimeMillis());

        if (report == null || report.id == 0) return;

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.putExtra("jobTitle", report.jobTitle);
        intent.putExtra("reportId", report.id); // 🔥 обязательно

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                (int) report.id,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        alarmManager.cancel(pendingIntent);
    }

    public static void scheduleReminder(Context context, JobReport report) {
        if (report == null || report.id == 0 || report.reminderTime <= 0) return;

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.putExtra("jobTitle", report.jobTitle);
        intent.putExtra("reportId", report.id); // обязательно для идентификации

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                (int) report.id,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                report.reminderTime,
                pendingIntent
        );

        Log.d("ReminderUtils", "Установлено напоминание на: " + report.reminderTime + " для ID: " + report.id);
    }





}
