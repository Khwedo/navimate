package com.oceanbyte.navimate.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.oceanbyte.navimate.models.JobReport;
import com.oceanbyte.navimate.repository.ReportRepository;
import com.oceanbyte.navimate.utils.ReminderUtils;

import java.util.List;

public class ReportViewModel extends AndroidViewModel {

    private final ReportRepository repository;
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MediatorLiveData<List<JobReport>> cachedReports = new MediatorLiveData<>();

    private int lastLoadedContractId = -1;
    private LiveData<List<JobReport>> currentLiveData;

    public ReportViewModel(@NonNull Application application) {
        super(application);
        repository = new ReportRepository(application);
    }

    /**
     * Получение списка отчётов по контракту с кэшированием
     */
    public LiveData<List<JobReport>> getReportsByContract(int contractId) {
        if (contractId != lastLoadedContractId || cachedReports.getValue() == null) {
            lastLoadedContractId = contractId;

            if (currentLiveData != null) {
                cachedReports.removeSource(currentLiveData);
            }

            currentLiveData = repository.getReportsByContractLive(contractId);
            cachedReports.addSource(currentLiveData, cachedReports::setValue);
        }

        return cachedReports;
    }

    /**
     * Сброс кэша (например, при обновлении списка)
     */
    public void clearCache() {
        cachedReports.postValue(null);
        lastLoadedContractId = -1;
        if (currentLiveData != null) {
            cachedReports.removeSource(currentLiveData);
            currentLiveData = null;
        }
    }

    /**
     * Пагинация (ленивая подгрузка отчётов)
     */
    public void getReportsPaged(int contractId, int offset, int limit, ReportRepository.Callback<List<JobReport>> callback) {
        repository.getReportsByContractPaged(contractId, offset, limit, callback);
    }

    /**
     * Синхронная загрузка первой порции отчётов
     */
    public List<JobReport> getReportsFirstBatchSync(int contractId, int limit) {
        return repository.getReportsFirstBatchSync(contractId, limit);
    }

    /**
     * Загрузка всех отчётов (например, для экспорта)
     */
    public void loadAllReports(MutableLiveData<List<JobReport>> targetLiveData) {
        isLoading.setValue(true);
        repository.getAllReportsLive().observeForever(reports -> {
            isLoading.postValue(false);
            targetLiveData.postValue(reports);
        });
    }

    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getErrorMessage() { return errorMessage; }

    public void deleteReport(JobReport report) {
        isLoading.setValue(true);
        ReminderUtils.cancelReminder(getApplication(), report); // отмена напоминания

        repository.deleteReport(report, () -> {
            isLoading.postValue(false);
            clearCache(); // чтобы обновился список
        });
    }


}
