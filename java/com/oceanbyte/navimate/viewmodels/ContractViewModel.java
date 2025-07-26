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
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.oceanbyte.navimate.models.ContractEntity;
import com.oceanbyte.navimate.notifications.ReminderReceiver;
import com.oceanbyte.navimate.repository.ContractRepository;

import java.util.List;

/**
 * ViewModel для управления контрактами с кэшированием.
 * Автоматически сбрасывает кэш при добавлении, обновлении или удалении.
 */
public class ContractViewModel extends AndroidViewModel {

    private final ContractRepository repository;
    private final MediatorLiveData<List<ContractEntity>> cachedContracts = new MediatorLiveData<>();
    private LiveData<List<ContractEntity>> currentSource;

    private final MediatorLiveData<ContractEntity> activeContractLiveData = new MediatorLiveData<>();
    private final MutableLiveData<String> errorMessageLiveData = new MutableLiveData<>();

    private String lastUuid = null;

    public ContractViewModel(@NonNull Application application) {
        super(application);
        repository = new ContractRepository(application);
    }

    /**
     * Возвращает LiveData-кэш контрактов, обновляет при смене UUID
     */
    public LiveData<List<ContractEntity>> getContractsLive(String uuid) {
        if (uuid == null || uuid.equals(lastUuid)) {
            return cachedContracts;
        }

        lastUuid = uuid;

        LiveData<List<ContractEntity>> newSource = repository.getContractsLive(uuid);
        if (currentSource != null) {
            cachedContracts.removeSource(currentSource);
        }

        currentSource = newSource;
        cachedContracts.addSource(currentSource, cachedContracts::setValue);

        return cachedContracts;
    }

    /**
     * Добавление нового контракта с обработкой ошибок
     */
    public void insertContract(ContractEntity contract, Runnable onComplete) {
        repository.insertContract(contract, () -> {
            refreshContracts();
            if (onComplete != null) onComplete.run();
        }, error -> {
            errorMessageLiveData.postValue(error); // передаём ошибку из репозитория в UI
        });
    }

    /**
     * Обновление названия судна и должности
     */
    public void updateVesselAndPosition(int contractId, String vesselName, String position, Runnable onComplete) {
        repository.updateVesselAndPosition(contractId, vesselName, position, () -> {
           refreshContracts();
            if (onComplete != null) onComplete.run();
        });
    }

    /**
     * Полное обновление контракта
     */
    public void updateContract(ContractEntity contract, Runnable onComplete) {
        repository.updateContract(contract, () -> {
            refreshContracts();
            if (onComplete != null) onComplete.run();
        });
    }

    /**
     * Удаление контракта
     */
    public void deleteContract(ContractEntity contract, Runnable onComplete) {
        repository.deleteContractAndReports(contract.id, () -> {
            refreshContracts();
            if (onComplete != null) onComplete.run();
        });
    }

    public void deleteContractAndReports(int contractId, Runnable onComplete) {
        repository.deleteContractAndReports(contractId, () -> {
            refreshContracts();
            if (onComplete != null) onComplete.run();
        });
    }

    /**
     * Получение активного контракта по UUID
     */
    public void getActiveContract(String uuid, ContractRepository.ActiveContractCallback callback) {
        repository.getActiveContract(uuid, callback);
    }

    /**
     * Получение контракта по ID
     */
    public void getContractById(int contractId, ContractRepository.ActiveContractCallback callback) {
        repository.getContractById(contractId, callback);
    }

    /**
     * Сброс кэша вручную
     */
    public void refreshContracts() {
        if (lastUuid != null) {
            currentSource = null;
            cachedContracts.setValue(null);
            lastUuid = null;

            // Лог (если нужно для отладки)
            android.util.Log.d("ContractViewModel", "Кеш сброшен. Источник LiveData обновлён.");

            // Автоматическая пересоздача подписки
            getContractsLive(lastUuid);
        }
    }

    /**
     * LiveData активного контракта
     */
    public LiveData<ContractEntity> getActiveContractLiveData() {
        return activeContractLiveData;
    }

    public void loadActiveContract(String uuid) {
        repository.getActiveContract(uuid, contract -> activeContractLiveData.postValue(contract));
    }

    /**
     * LiveData для ошибок
     */
    public LiveData<String> getErrorMessageLiveData() {
        return errorMessageLiveData;
    }


}
