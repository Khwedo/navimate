package com.oceanbyte.navimate.utils;

import android.content.Context;
import android.content.Intent;

import androidx.fragment.app.Fragment;

import com.oceanbyte.navimate.view.PhotoViewerActivity;

import java.util.ArrayList;

public class PhotoOpener {

    /**
     * Открыть полноэкранный просмотр фотографий.
     * @param fragment - фрагмент, из которого вызывается
     * @param photoPaths - список путей к фотографиям
     * @param startPosition - позиция фото, с которого начать просмотр
     */
    public static void open(Fragment fragment, ArrayList<String> photoPaths, int startPosition) {
        if (photoPaths == null || photoPaths.isEmpty() || fragment == null || fragment.getContext() == null) {
            return;
        }

        Intent intent = new Intent(fragment.getContext(), PhotoViewerActivity.class);
        intent.putStringArrayListExtra("photoPaths", photoPaths);
        intent.putExtra("startPosition", startPosition);
        fragment.startActivity(intent);
        fragment.requireActivity().overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    /**
     * Альтернатива — запуск из Activity
     */
    public static void open(Context context, ArrayList<String> photoPaths, int startPosition) {
        if (photoPaths == null || photoPaths.isEmpty() || context == null) {
            return;
        }

        Intent intent = new Intent(context, PhotoViewerActivity.class);
        intent.putStringArrayListExtra("photoPaths", photoPaths);
        intent.putExtra("startPosition", startPosition);
        context.startActivity(intent);
    }
}
