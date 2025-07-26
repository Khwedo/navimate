package com.oceanbyte.navimate.repository;

import android.content.Context;
import android.net.Uri;

import com.oceanbyte.navimate.database.AppDatabase;
import com.oceanbyte.navimate.models.JobReport;
import com.oceanbyte.navimate.utils.ImageUtils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CreateReportRepository {

    private final AppDatabase db;
    private final ExecutorService executor;

    public interface InsertCallback {
        void onInserted();
    }

    public interface ImageCompressCallback {
        void onSuccess(String compressedPath);
        void onError(String error);
    }

    public CreateReportRepository(Context context) {
        db = AppDatabase.getInstance(context);
        executor = Executors.newFixedThreadPool(2);
    }

    /** Сохранение JobReport с привязкой к контракту */
    public void saveReport(JobReport report, InsertCallback callback) {
        executor.execute(() -> {
            db.reportDao().insert(report);
            if (callback != null) {
                callback.onInserted();
            }
        });
    }

    /** Сжатие изображения с сохранением до 50КБ */
    public void compressImage(Context context, Uri uri, ImageCompressCallback callback) {
        executor.execute(() -> {
            try {
                String fileName = "photo_" + System.currentTimeMillis() + ".jpg";
                String compressedPath = ImageUtils.processAndSaveImageToTemp(context, uri, fileName, 50);
                if (compressedPath != null) {
                    callback.onSuccess(compressedPath);
                } else {
                    callback.onError("Не удалось сохранить изображение");
                }
            } catch (Exception e) {
                callback.onError(e.getMessage());
            }
        });
    }
}
