package com.oceanbyte.navimate.viewmodels;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.oceanbyte.navimate.database.AppDatabase;
import com.oceanbyte.navimate.models.JobReport;
import com.oceanbyte.navimate.utils.ReminderUtils;

import java.io.File;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ReportDetailViewModel extends AndroidViewModel {

    private final MutableLiveData<JobReport> reportLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final AppDatabase db;
    private final Application app;

    public ReportDetailViewModel(@NonNull Application application) {
        super(application);
        this.app = application;
        db = AppDatabase.getInstance(application);
    }

    public LiveData<JobReport> getReportLiveData() {
        return reportLiveData;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    /** Загрузка отчета по ID */
    public void loadReport(long reportId) {
        isLoading.postValue(true);

        executor.execute(() -> {
            try {
                JobReport report = db.reportDao().getById(reportId);
                reportLiveData.postValue(report);
            } catch (Exception e) {
                errorMessage.postValue("Ошибка при загрузке отчета");
                Log.e("ReportDetailVM", "loadReport error", e);
            } finally {
                isLoading.postValue(false);
            }
        });
    }

    /** Удаление отчета с очисткой ресурсов */
    public void deleteReport(JobReport report, Runnable onSuccess) {
        isLoading.postValue(true);

        executor.execute(() -> {
            try {
                // Отмена напоминания
                ReminderUtils.cancelReminder(app, report);

                // Удаление фото
                deletePhotos(report.beforePhotos);
                deletePhotos(report.afterPhotos);

                // Удаление из базы
                db.reportDao().delete(report);

                // Сообщаем об успехе
                onSuccess.run();

            } catch (Exception e) {
                errorMessage.postValue("Ошибка при удалении отчета");
                Log.e("ReportDetailVM", "deleteReport error", e);

            } finally {
                isLoading.postValue(false);
            }
        });
    }

    /** Удаляет список фото с устройства */
    private void deletePhotos(List<String> photoPaths) {
        if (photoPaths == null) return;

        for (String path : photoPaths) {
            File file = new File(path);
            if (file.exists()) {
                boolean deleted = file.delete();
                Log.d("ReportDetailVM", "Deleted photo: " + path + " => " + deleted);
            }
        }
    }
}
