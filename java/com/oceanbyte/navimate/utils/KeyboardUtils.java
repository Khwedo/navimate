package com.oceanbyte.navimate.utils;

import android.app.Activity;
import android.content.Context;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

public class KeyboardUtils {

    // Скрывает клавиатуру
    public static void hideKeyboard(Activity activity) {
        View view = activity.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null)
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    // Если пользователь тапает вне EditText — скрываем клавиатуру
    public static void setupHideKeyboardOnTouch(final Activity activity, View rootView) {
        if (rootView == null) return;

        rootView.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                hideKeyboard(activity);
            }
            return false;
        });
    }
}
