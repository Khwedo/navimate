package com.oceanbyte.navimate.viewmodels;

import android.app.AlarmManager;
import android.app.Application;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.oceanbyte.navimate.models.JobReport;
import com.oceanbyte.navimate.notifications.ReminderReceiver;
import com.oceanbyte.navimate.repository.ReportRepository;
import com.oceanbyte.navimate.utils.ImageUtils;
import com.oceanbyte.navimate.utils.ReminderUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EditReportViewModel extends AndroidViewModel {

    private final ReportRepository repository;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private final MutableLiveData<JobReport> reportLiveData = new MutableLiveData<>();
    private final MutableLiveData<List<String>> tempBeforePhotos = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<String>> tempAfterPhotos = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<String> jobTitle = new MutableLiveData<>();
    private final MutableLiveData<String> equipmentName = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> updateSuccess = new MutableLiveData<>();
    private final MutableLiveData<Boolean> deleteSuccess = new MutableLiveData<>();
    private final MutableLiveData<Long> reminderTimeLiveData = new MutableLiveData<>();

    private String userNote = "";
    private long reminderTime = 0L;

    public EditReportViewModel(@NonNull Application application) {
        super(application);
        repository = new ReportRepository(application);
    }

    // ---------------- Загрузка ---------------- //
    public void loadReportById(long reportId) {
        isLoading.setValue(true);
        repository.getReportById(reportId, report -> {
            reportLiveData.postValue(report);
            jobTitle.postValue(report.jobTitle);
            equipmentName.postValue(report.equipmentName);
            tempBeforePhotos.postValue(new ArrayList<>(report.beforePhotos != null ? report.beforePhotos : new ArrayList<>()));
            tempAfterPhotos.postValue(new ArrayList<>(report.afterPhotos != null ? report.afterPhotos : new ArrayList<>()));
            userNote = report.userNote != null ? report.userNote : "";
            reminderTime = report.reminderTime;
            reminderTimeLiveData.postValue(reminderTime);
            isLoading.postValue(false);
        });
    }

    // ---------------- Фото ---------------- //

    public LiveData<List<String>> getBeforePhotos() {
        return tempBeforePhotos;
    }

    public LiveData<List<String>> getAfterPhotos() {
        return tempAfterPhotos;
    }

    public void compressAndAddPhoto(Context context, Uri uri, boolean isBefore) {
        isLoading.setValue(true);
        repository.compressImage(context, uri, new ReportRepository.OnImageCompressedCallback() {
            @Override
            public void onSuccess(String path) {
                List<String> list = isBefore ? tempBeforePhotos.getValue() : tempAfterPhotos.getValue();
                if (list == null) list = new ArrayList<>();
                if (list.size() >= 6) {
                    errorMessage.postValue("Максимум 6 фото " + (isBefore ? "'до'" : "'после'"));
                    isLoading.postValue(false);
                    return;
                }
                list = new ArrayList<>(list);
                list.add(path);
                if (isBefore) {
                    tempBeforePhotos.postValue(list);
                } else {
                    tempAfterPhotos.postValue(list);
                }
                isLoading.postValue(false);
            }

            @Override
            public void onError(String error) {
                isLoading.postValue(false);
                errorMessage.postValue("Ошибка изображения: " + error);
            }
        });
    }

    public void removeBeforePhoto(int index) {
        removePhoto(tempBeforePhotos, index);
    }

    public void removeAfterPhoto(int index) {
        removePhoto(tempAfterPhotos, index);
    }

    private void removePhoto(MutableLiveData<List<String>> liveData, int index) {
        List<String> list = liveData.getValue();
        if (list != null && index >= 0 && index < list.size()) {
            List<String> newList = new ArrayList<>(list);
            String path = newList.remove(index);
            ImageUtils.deleteFile(path);
            liveData.postValue(newList);
        }
    }

    public void discardTempPhotos() {
        ImageUtils.deleteTempPhotos(tempBeforePhotos.getValue());
        ImageUtils.deleteTempPhotos(tempAfterPhotos.getValue());
        tempBeforePhotos.setValue(new ArrayList<>());
        tempAfterPhotos.setValue(new ArrayList<>());
    }

    // ---------------- Обновление ---------------- //

    public void setJobTitle(String title) {
        jobTitle.setValue(title);
    }

    public void setEquipmentName(String name) {
        equipmentName.setValue(name);
    }



    public void setUserNote(String note) {
        this.userNote = note;
    }

    public void setReminderTime(long time) {
        this.reminderTime = time;
        reminderTimeLiveData.setValue(time);
    }

    public LiveData<Long> getReminderTime() {
        return reminderTimeLiveData;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<JobReport> getReportLiveData() {
        return reportLiveData;
    }

    public LiveData<Boolean> getUpdateSuccess() {
        return updateSuccess;
    }

    public LiveData<Boolean> getDeleteSuccess() {
        return deleteSuccess;
    }

    public void clearUpdateSuccess() {
        updateSuccess.setValue(null);
    }

    public void clearDeleteSuccess() {
        deleteSuccess.setValue(null);
    }

    public void updateReport(long reportId, int contractId) {
        String title = jobTitle.getValue();
        String equipment = equipmentName.getValue();

        if (title == null || title.trim().isEmpty() || equipment == null || equipment.trim().isEmpty()) {
            errorMessage.setValue("Заполните все поля");
            return;
        }

        isLoading.setValue(true);

        JobReport existing = reportLiveData.getValue();
        if (existing == null) {
            errorMessage.setValue("Отчёт не загружен");
            isLoading.setValue(false);
            return;
        }

        JobReport updated = new JobReport();
        updated.id = reportId;
        updated.contractId = contractId;
        updated.jobTitle = title.trim();
        updated.equipmentName = equipment.trim();
        updated.beforePhotos = commitPhotos(getApplication(), tempBeforePhotos.getValue());
        updated.afterPhotos = commitPhotos(getApplication(), tempAfterPhotos.getValue());
        updated.userNote = userNote;
        updated.reminderTime = reminderTime;
        updated.reportDate = existing.reportDate;
        updated.createdAt = existing.createdAt;

        repository.updateReport(updated, () -> {
            ReminderUtils.cancelReminder(getApplication(), existing);
            ReminderUtils.scheduleReminder(getApplication(), updated);
            isLoading.postValue(false);
            updateSuccess.postValue(true);
        });
    }

    private List<String> commitPhotos(Context context, List<String> tempPaths) {
        return ImageUtils.movePhotosToPermanentStorage(context, tempPaths);
    }

    // ---------------- Удаление ---------------- //

    public void deleteReport(JobReport report) {
        isLoading.setValue(true);
        ReminderUtils.cancelReminder(getApplication(), report);
        repository.deleteReport(report, () -> {
            executor.execute(() -> {
                deletePhotos(report.beforePhotos);
                deletePhotos(report.afterPhotos);
            });
            isLoading.postValue(false);
            deleteSuccess.postValue(true);
        });
    }

    private void deletePhotos(List<String> photoPaths) {
        if (photoPaths == null) return;
        for (String path : photoPaths) {
            File file = new File(path);
            if (file.exists()) {
                boolean deleted = file.delete();
                Log.d("EditReportVM", "Удалено фото: " + path + " => " + deleted);
            }
        }
    }

    // ---------------- Для передачи из фрагмента ---------------- //
    public MutableLiveData<String> getJobTitleLive() {
        return jobTitle;
    }

    public MutableLiveData<String> getEquipmentNameLive() {
        return equipmentName;
    }
}
