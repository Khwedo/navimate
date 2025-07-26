package com.oceanbyte.navimate.utils;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.UUID;

/**
 * Утилита для управления UUID пользователя и флагами первого запуска.
 */
public class UserUtils {
    private static final String PREF_NAME = "user_prefs";
    private static final String KEY_UUID = "user_uuid";
    private static final String KEY_FIRST_RUN = "first_run";

    /**
     * Получает UUID пользователя, генерирует и сохраняет, если ещё не установлен.
     */
    public static String getUserUUID(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String uuid = prefs.getString(KEY_UUID, null);

        if (uuid == null) {
            uuid = UUID.randomUUID().toString();
            prefs.edit().putString(KEY_UUID, uuid).apply();
        }

        return uuid;
    }

    /**
     * Возвращает true, если это первый запуск.
     */
    public static boolean isFirstRun(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_FIRST_RUN, true);
    }

    /**
     * Устанавливает флаг "не первый запуск".
     */
    public static void setFirstRunCompleted(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_FIRST_RUN, false).apply();
    }

    /**
     * Очистить UUID (например, при выходе из аккаунта).
     */
    public static void clearUUID(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().remove(KEY_UUID).apply();
    }

    public static String getOrCreateUserUuid(Context context) {
        return getUserUUID(context);
    }
}
