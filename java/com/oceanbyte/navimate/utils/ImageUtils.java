package com.oceanbyte.navimate.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.net.Uri;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Утилита для обработки изображений: сжатие, водяной знак, сохранение.
 */
public class ImageUtils {

    private static final String TEMP_DIR = "NaviMate/temp_photos";
    private static final String FINAL_DIR = "NaviMate/photos";
    private static final int MAX_SIZE_DP = 1800; // Увеличено с 1200 для лучшего качества

    /** Сжатие и сохранение изображения во временную папку */
    public static String processAndSaveImageToTemp(Context context, Uri imageUri, String watermark, int maxSizeDp) {
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(imageUri);
            Bitmap original = BitmapFactory.decodeStream(inputStream);
            if (original == null) return null;

            Bitmap resized = resizeBitmapKeepingRatio(original, MAX_SIZE_DP); // Используем обновлённое значение
            Bitmap withWatermark = addWatermark(resized, watermark);

            File tempDir = new File(context.getCacheDir(), TEMP_DIR);
            if (!tempDir.exists()) tempDir.mkdirs();

            String filename = "temp_" + System.currentTimeMillis() + ".jpg";
            File outputFile = new File(tempDir, filename);

            FileOutputStream out = new FileOutputStream(outputFile);
            withWatermark.compress(Bitmap.CompressFormat.JPEG, 92, out); // Качество увеличено с 85 до 92
            out.close();

            return outputFile.getAbsolutePath();

        } catch (Exception e) {
            Log.e("ImageUtils", "processAndSaveImageToTemp error", e);
            return null;
        }
    }

    /** Переносит изображения из временной папки в постоянную */
    public static List<String> movePhotosToPermanentStorage(Context context, List<String> tempPaths) {
        List<String> finalPaths = new ArrayList<>();
        if (tempPaths == null) return finalPaths;

        File finalDir = new File(context.getFilesDir(), FINAL_DIR);
        if (!finalDir.exists()) finalDir.mkdirs();

        for (String tempPath : tempPaths) {
            try {
                File tempFile = new File(tempPath);
                if (!tempFile.exists()) continue;

                String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
                File finalFile = new File(finalDir, "photo_" + timestamp + "_" + tempFile.getName());

                boolean renamed = tempFile.renameTo(finalFile);
                if (renamed) {
                    finalPaths.add(finalFile.getAbsolutePath());
                }

            } catch (Exception e) {
                Log.e("ImageUtils", "move error: " + tempPath, e);
            }
        }

        return finalPaths;
    }

    /** Удаление файла по пути */
    public static void deleteFile(String path) {
        if (path == null) return;
        try {
            File file = new File(path);
            if (file.exists()) file.delete();
        } catch (Exception e) {
            Log.e("ImageUtils", "deleteFile error", e);
        }
    }

    /** Удаление всех временных фото */
    public static void deleteTempPhotos(List<String> paths) {
        if (paths == null) return;
        for (String path : paths) {
            deleteFile(path);
        }
    }

    /** Уменьшает размер изображения до заданного максимального размера */
    private static Bitmap resizeBitmapKeepingRatio(Bitmap source, int maxSize) {
        int width = source.getWidth();
        int height = source.getHeight();

        float ratio = (float) width / height;
        int newWidth = width;
        int newHeight = height;

        if (width > height && width > maxSize) {
            newWidth = maxSize;
            newHeight = (int) (maxSize / ratio);
        } else if (height > width && height > maxSize) {
            newHeight = maxSize;
            newWidth = (int) (maxSize * ratio);
        } else if (width > maxSize || height > maxSize) {
            newWidth = maxSize;
            newHeight = maxSize;
        }

        return Bitmap.createScaledBitmap(source, newWidth, newHeight, true);
    }

    /** Наносит водяной знак на изображение */
    private static Bitmap addWatermark(Bitmap original, String watermark) {
        Bitmap result = original.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(result);
        Paint paint = new Paint();

        paint.setColor(0xAAFFFFFF); // Белый с прозрачностью
        paint.setTextSize(32);
        paint.setAntiAlias(true);
        paint.setShadowLayer(1f, 1f, 1f, 0xFF000000);

        Rect bounds = new Rect();
        paint.getTextBounds(watermark, 0, watermark.length(), bounds);
        int x = 16;
        int y = result.getHeight() - 16;

        canvas.drawText(watermark, x, y, paint);
        return result;
    }
}
