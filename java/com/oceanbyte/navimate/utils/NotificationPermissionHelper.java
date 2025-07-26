package com.oceanbyte.navimate.utils;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

public class NotificationPermissionHelper {

    public static boolean isNotificationPermissionGranted(Activity activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return true; // Разрешение не требуется до Android 13
        }
        return ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED;
    }

    public static void requestNotificationPermissionIfNeeded(
            Activity activity,
            ActivityResultLauncher<String> permissionLauncher
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!isNotificationPermissionGranted(activity)) {
                new AlertDialog.Builder(activity)
                        .setTitle("Разрешение на уведомления")
                        .setMessage("Разрешите отправку уведомлений, чтобы получать напоминания о задачах.")
                        .setPositiveButton("Разрешить", (dialog, which) ->
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS))
                        .setNegativeButton("Отмена", null)
                        .show();
            }
        }
    }
}
