package com.oceanbyte.navimate.repository;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LiveData;

import com.oceanbyte.navimate.database.AppDatabase;
import com.oceanbyte.navimate.database.contract.ContractDao;
import com.oceanbyte.navimate.models.ContractEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Репозиторий для управления контрактами пользователя.
 * Поддерживает фоновую работу и кэширование:
 * - Активный контракт по UUID
 * - Контракт по ID
 */
public class ContractRepository {

    private final ContractDao contractDao;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private final Map<String, ContractEntity> activeContractCache = new HashMap<>();
    private final Map<Integer, ContractEntity> contractByIdCache = new HashMap<>();

    public interface ActiveContractCallback {
        void onResult(ContractEntity contract);
    }

    public ContractRepository(Application application) {
        AppDatabase db = AppDatabase.getInstance(application);
        contractDao = db.contractDao();
    }

    public LiveData<List<ContractEntity>> getContractsLive(String uuid) {
        return contractDao.getContractsLive(uuid);
    }

    /** Вставка контракта с проверкой на даты */
    public void insertContract(ContractEntity newContract, Runnable onSuccess, Consumer<String> onError) {
        executor.execute(() -> {
            List<ContractEntity> existingContracts = contractDao.getContractsByUserUuid(newContract.userUuid);

            for (ContractEntity existing : existingContracts) {
                long existingStart = existing.startDate;
                Long existingEnd = existing.endDate; // может быть null

                long newStart = newContract.startDate;
                Long newEnd = newContract.endDate; // может быть null

                // Если конец текущего контракта не задан, считаем его бесконечно длинным
                long existingEndSafe = (existingEnd != null) ? existingEnd : Long.MAX_VALUE;
                long newEndSafe = (newEnd != null) ? newEnd : Long.MAX_VALUE;

                // Условие пересечения по датам
                boolean overlaps =
                        (newStart <= existingEndSafe) &&
                                (newEndSafe >= existingStart);

                if (overlaps) {
                    postToMain(() -> onError.accept("Новый контракт пересекается по времени с существующим контрактом."));
                    return;
                }
            }

            // Всё чисто — сохраняем
            contractDao.insert(newContract);
            clearCache();
            postToMain(onSuccess);
        });
    }



    public void updateVesselAndPosition(int contractId, String vesselName, String position, Runnable onComplete) {
        executor.execute(() -> {
            contractDao.updateVesselAndPosition(contractId, vesselName, position);
            clearCache();
            postToMain(onComplete);
        });
    }

    public void updateContract(ContractEntity contract, Runnable onComplete) {
        executor.execute(() -> {
            contractDao.updateContract(contract);
            clearCache();
            postToMain(onComplete);
        });
    }

    public void deleteContractAndReports(int contractId, Runnable onComplete) {
        executor.execute(() -> {
            contractDao.deleteContractById(contractId);
            clearCache();
            postToMain(onComplete);
        });
    }

    public void getActiveContract(String uuid, ActiveContractCallback callback) {
        getFromCacheOrExecute(
                activeContractCache,
                uuid,
                () -> contractDao.getActiveContract(uuid),
                callback::onResult
        );
    }

    public void getContractById(int contractId, ActiveContractCallback callback) {
        getFromCacheOrExecute(
                contractByIdCache,
                contractId,
                () -> contractDao.getContractById(contractId),
                callback::onResult
        );
    }

    private <K, V> void getFromCacheOrExecute(Map<K, V> cache, K key, Supplier<V> dbFetcher, Consumer<V> callback) {
        V cached = cache.get(key);
        if (cached != null) {
            mainHandler.post(() -> callback.accept(cached));
        } else {
            executor.execute(() -> {
                V value = dbFetcher.get();
                if (value != null) cache.put(key, value);
                mainHandler.post(() -> callback.accept(value));
            });
        }
    }

    public ContractEntity getActiveContractSync() {
        return contractDao.getActiveContractSync();
    }

    public void clearCache() {
        activeContractCache.clear();
        contractByIdCache.clear();
    }

    private void postToMain(Runnable task) {
        if (task != null) {
            mainHandler.post(task);
        }
    }
}
