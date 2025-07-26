package com.oceanbyte.navimate.repository;

import android.content.Context;
import android.net.Uri;

import androidx.lifecycle.LiveData;

import com.oceanbyte.navimate.database.AppDatabase;
import com.oceanbyte.navimate.database.ReportDao;
import com.oceanbyte.navimate.models.JobReport;
import com.oceanbyte.navimate.utils.ImageUtils;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Репозиторий для управления отчётами:
 * - Загрузка, добавление, обновление, удаление
 * - Сжатие фото
 */
public class ReportRepository {

    private final ReportDao reportDao;
    private final ExecutorService executor = Executors.newFixedThreadPool(4);

    public interface OnCompleteCallback {
        void onComplete();
    }

    public interface Callback<T> {
        void onResult(T result);
    }

    public interface OnReportLoadedCallback {
        void onLoaded(JobReport report);
    }

    public interface OnImageCompressedCallback {
        void onSuccess(String path);
        void onError(String error);
    }

    public ReportRepository(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        this.reportDao = db.reportDao();
    }

    /** Загрузка отчета по ID */
    public void getReportById(long id, OnReportLoadedCallback callback) {
        executor.execute(() -> {
            JobReport report = reportDao.getById(id);
            if (callback != null) callback.onLoaded(report);
        });
    }

    /** Добавление нового отчета */
    public void insertReport(JobReport report, OnCompleteCallback callback) {
        executor.execute(() -> {
            reportDao.insert(report);
            if (callback != null) callback.onComplete();
        });
    }

    /** Обновление отчета */
    public void updateReport(JobReport report, OnCompleteCallback callback) {
        executor.execute(() -> {
            reportDao.update(report);
            if (callback != null) callback.onComplete();
        });
    }

    /** Удаление отчета */
    public void deleteReport(JobReport report, OnCompleteCallback callback) {
        executor.execute(() -> {
            reportDao.delete(report);
            if (callback != null) callback.onComplete();
        });
    }

    /** Получение всех отчётов по контракту (LiveData) */
    public LiveData<List<JobReport>> getReportsByContractLive(int contractId) {
        return reportDao.getReportsByContractLive(contractId);
    }

    /** Получение всех отчетов (если нужно) */
    public LiveData<List<JobReport>> getAllReportsLive() {
        return reportDao.getAllReportsLive();
    }

    /** Получение количества отчетов по контракту */
    public void getReportCountByContract(int contractId, OnReportCountCallback callback) {
        executor.execute(() -> {
            int count = reportDao.getReportCountForContract(contractId);
            if (callback != null) callback.onCountLoaded(count);
        });
    }

    public interface OnReportCountCallback {
        void onCountLoaded(int count);
    }

    /** Сжатие изображения */
    public void compressImage(Context context, Uri uri, OnImageCompressedCallback callback) {
        executor.execute(() -> {
            try {
                String filename = "photo_" + System.currentTimeMillis() + ".jpg";
                String path = ImageUtils.processAndSaveImageToTemp(context, uri, filename, 50);
                if (path != null) {
                    callback.onSuccess(path);
                } else {
                    callback.onError("Ошибка сжатия изображения");
                }
            } catch (Exception e) {
                callback.onError("Ошибка: " + e.getMessage());
            }
        });
    }

    /** Очистка ресурсов, если нужно */
    public void shutdown() {
        executor.shutdown();
    }
    /** метод для пакетного удаления */
    public void deleteReportById(long reportId) {
        executor.execute(() -> reportDao.deleteById(reportId));
    }

    public void getReportsByContractPaged(int contractId, int offset, int limit, Callback<List<JobReport>> callback) {
        executor.execute(() -> {
            List<JobReport> reports = reportDao.getReportsByContractPaged(contractId, limit, offset);
            if (callback != null) callback.onResult(reports);
        });
    }
    public List<JobReport> getReportsFirstBatchSync(int contractId, int limit) {
        return reportDao.getReportsByContractPaged(contractId, limit, 0);
    }


}
