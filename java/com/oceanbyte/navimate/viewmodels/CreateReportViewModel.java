package com.oceanbyte.navimate.viewmodels;

import android.app.AlarmManager;
import android.app.Application;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.oceanbyte.navimate.models.ContractEntity;
import com.oceanbyte.navimate.models.JobReport;
import com.oceanbyte.navimate.repository.ContractRepository;
import com.oceanbyte.navimate.repository.CreateReportRepository;
import com.oceanbyte.navimate.utils.ImageUtils;
import com.oceanbyte.navimate.notifications.ReminderReceiver;

import java.util.ArrayList;
import java.util.List;

public class CreateReportViewModel extends AndroidViewModel {

    private final CreateReportRepository reportRepository;
    private final ContractRepository contractRepository;

    public final MutableLiveData<String> jobTitle = new MutableLiveData<>("");
    public final MutableLiveData<String> equipmentName = new MutableLiveData<>("");

    private final MutableLiveData<List<String>> tempBeforePhotos = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<String>> tempAfterPhotos = new MutableLiveData<>(new ArrayList<>());

    private final MutableLiveData<ContractEntity> activeContractLiveData = new MutableLiveData<>();

    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> isSaving = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> saveCompleted = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Long> reminderTimeLiveData = new MutableLiveData<>(0L);

    private String userNote = "";
    private long reminderTime = 0L;

    public CreateReportViewModel(@NonNull Application application) {
        super(application);
        this.reportRepository = new CreateReportRepository(application);
        this.contractRepository = new ContractRepository(application);
    }

    // ---------------- Фото ---------------- //

    public LiveData<List<String>> getBeforePhotos() {
        return tempBeforePhotos;
    }

    public LiveData<List<String>> getAfterPhotos() {
        return tempAfterPhotos;
    }

    public boolean canAddPhoto(boolean isBefore) {
        List<String> list = isBefore ? tempBeforePhotos.getValue() : tempAfterPhotos.getValue();
        return list == null || list.size() < 6;
    }

    public void compressAndAddPhoto(Context context, Uri uri, boolean isBefore) {
        isLoading.setValue(true);
        reportRepository.compressImage(context, uri, new CreateReportRepository.ImageCompressCallback() {
            @Override
            public void onSuccess(String compressedPath) {
                if (compressedPath == null || compressedPath.isEmpty()) {
                    postError("Не удалось сжать изображение.");
                    return;
                }

                List<String> list = isBefore
                        ? new ArrayList<>(tempBeforePhotos.getValue() != null ? tempBeforePhotos.getValue() : new ArrayList<>())
                        : new ArrayList<>(tempAfterPhotos.getValue() != null ? tempAfterPhotos.getValue() : new ArrayList<>());

                if (list.size() >= 6) {
                    postError("Можно добавить максимум 6 фото '" + (isBefore ? "до" : "после") + "'");
                    return;
                }

                list.add(compressedPath);
                if (isBefore) {
                    tempBeforePhotos.postValue(list);
                } else {
                    tempAfterPhotos.postValue(list);
                }

                isLoading.postValue(false);
            }

            @Override
            public void onError(String error) {
                postError("Ошибка сжатия: " + error);
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
            String removedPath = newList.remove(index);
            liveData.setValue(newList);
            ImageUtils.deleteFile(removedPath); // сразу удаляем временный файл
        }
    }

    public void discardTempPhotos() {
        ImageUtils.deleteTempPhotos(tempBeforePhotos.getValue());
        ImageUtils.deleteTempPhotos(tempAfterPhotos.getValue());
        tempBeforePhotos.setValue(new ArrayList<>());
        tempAfterPhotos.setValue(new ArrayList<>());
    }

    private List<String> commitPhotos(Context context, List<String> tempPaths) {
        return ImageUtils.movePhotosToPermanentStorage(context, tempPaths);
    }

    // ---------------- Контракт ---------------- //

    public void loadActiveContract(String uuid) {
        contractRepository.getActiveContract(uuid, contract -> {
            if (contract != null) {
                activeContractLiveData.postValue(contract);
            } else {
                postError("Активный контракт не найден.");
            }
        });
    }
    public ContractEntity getActiveContract() {
        return activeContractLiveData.getValue();
    }

    public LiveData<ContractEntity> getActiveContractLiveData() {
        return activeContractLiveData;
    }

    // ---------------- Сохранение ---------------- //

    public void setJobDetails(String title, String equipment) {
        jobTitle.setValue(title);
        equipmentName.setValue(equipment);
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

    public void saveReport() {
        String title = jobTitle.getValue();
        String equipment = equipmentName.getValue();
        ContractEntity contract = activeContractLiveData.getValue();

        if (title == null || title.trim().isEmpty() || equipment == null || equipment.trim().isEmpty()) {
            errorMessage.setValue("Пожалуйста, заполните все поля.");
            return;
        }

        if (contract == null || contract.id == 0) {
            errorMessage.setValue("Отчёт не может быть сохранён без активного контракта.");
            return;
        }

        isSaving.setValue(true);

        // Переносим фото из temp в постоянную директорию
        List<String> finalBeforePhotos = commitPhotos(getApplication(), tempBeforePhotos.getValue());
        List<String> finalAfterPhotos = commitPhotos(getApplication(), tempAfterPhotos.getValue());

        JobReport report = new JobReport(title.trim(), equipment.trim(), finalBeforePhotos, finalAfterPhotos);
        report.contractId = contract.id;
        report.userNote = userNote;
        report.reminderTime = reminderTime;

        reportRepository.saveReport(report, () -> {
            isSaving.postValue(false);
            saveCompleted.postValue(true);
            scheduleReminder(getApplication(), report);
        });
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<Boolean> getIsSaving() {
        return isSaving;
    }

    public LiveData<Boolean> getSaveCompleted() {
        return saveCompleted;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public void clearForm() {
        discardTempPhotos(); // удаляем незасохраняемые фото
        jobTitle.setValue("");
        equipmentName.setValue("");
        tempBeforePhotos.setValue(new ArrayList<>());
        tempAfterPhotos.setValue(new ArrayList<>());
        saveCompleted.setValue(false);
        userNote = "";
        reminderTime = 0L;
    }

    private void postError(String message) {
        isLoading.postValue(false);
        errorMessage.postValue(message);
    }

    private void scheduleReminder(Context context, JobReport report) {
        if (report.reminderTime <= 0) return;

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.putExtra("jobTitle", report.jobTitle);
        intent.putExtra("reportId", report.id); // обязательно

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                (int) report.id,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                Intent permissionIntent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                permissionIntent.setData(Uri.parse("package:" + context.getPackageName()));
                permissionIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(permissionIntent);
                return;
            }
        }

        alarmManager.setExact(AlarmManager.RTC_WAKEUP, report.reminderTime, pendingIntent);
    }
}
