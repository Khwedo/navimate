package com.oceanbyte.navimate.utils;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;

import java.util.Locale;

public class LanguageUtils {

    private static final String PREFS_NAME = "app_settings";
    private static final String KEY_LANGUAGE = "app_language";

    /**
     * Сохраняем выбранный язык в SharedPreferences
     */
    public static void saveLanguage(Context context, String langCode) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_LANGUAGE, langCode)
                .apply();
    }

    /**
     * Загружаем сохранённый язык
     */
    public static String getSavedLanguage(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_LANGUAGE, Locale.getDefault().getLanguage());
    }

    /**
     * Применяем сохранённый язык — вызывается в Application.onCreate()
     */
    public static void applySavedLanguage(Context context) {
        String langCode = getSavedLanguage(context);
        setLocale(context, langCode);
    }

    /**
     * Устанавливаем локаль для всего приложения (без перезапуска Activity)
     */
    private static void setLocale(Context context, String languageCode) {
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);

        Resources resources = context.getResources();
        Configuration config = resources.getConfiguration();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocale(locale);
        } else {
            config.locale = locale;
        }

        resources.updateConfiguration(config, resources.getDisplayMetrics());
    }

    /**
     * Меняем язык и перезапускаем активити (например, из SettingsFragment)
     */
    public static void changeAppLanguage(Activity activity, String languageCode) {
        saveLanguage(activity, languageCode); // Сохраняем выбранный язык
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);

        Resources resources = activity.getResources();
        Configuration config = resources.getConfiguration();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocale(locale);
        } else {
            config.locale = locale;
        }

        resources.updateConfiguration(config, resources.getDisplayMetrics());

        activity.recreate(); // Перезапускаем активити
    }
}
