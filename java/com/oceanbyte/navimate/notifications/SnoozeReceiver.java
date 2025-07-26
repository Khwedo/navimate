package com.oceanbyte.navimate.notifications;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class SnoozeReceiver extends BroadcastReceiver {

    @SuppressLint("ScheduleExactAlarm")
    @Override
    public void onReceive(Context context, Intent intent) {
        String jobTitle = intent.getStringExtra("jobTitle");
        long reportId = intent.getLongExtra("reportId", -1);

        if (reportId == -1) return; // Без reportId повтор не имеет смысла

        Intent reminderIntent = new Intent(context, ReminderReceiver.class);
        reminderIntent.putExtra("jobTitle", jobTitle);
        reminderIntent.putExtra("reportId", reportId);  // 🔥 ОБЯЗАТЕЛЬНО

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                (int) reportId, // 👍 уникальный ID
                reminderIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        long snoozeTimeMillis = System.currentTimeMillis() + (10 * 60 * 1000); // +10 мин

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, snoozeTimeMillis, pendingIntent);
        }
    }
}
