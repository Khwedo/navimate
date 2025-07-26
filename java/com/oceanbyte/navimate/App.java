package com.oceanbyte.navimate;

import android.app.Application;
import com.oceanbyte.navimate.utils.LanguageUtils;

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        LanguageUtils.applySavedLanguage(this);
    }
}
