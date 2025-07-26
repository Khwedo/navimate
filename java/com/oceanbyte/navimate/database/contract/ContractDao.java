package com.oceanbyte.navimate.database.contract;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import com.oceanbyte.navimate.models.ContractEntity;

import java.util.List;

@Dao
public interface ContractDao {

    /**
     * Вставка одного контракта с заменой, если ID совпадает
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(ContractEntity contract);

    /**
     * Пакетная вставка контрактов (например для синхронизации)
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<ContractEntity> contracts);

    /**
     * Обновление одного контракта
     */
    @Update
    void updateContract(ContractEntity contract);

    /**
     * Пакетное обновление контрактов
     */
    @Update
    void updateContracts(List<ContractEntity> contracts);

    /**
     * Получить активный контракт (синхронно)
     */
    @Query("SELECT * FROM contracts WHERE endDate IS NULL ORDER BY startDate DESC LIMIT 1")
    ContractEntity getActiveContractSync();

    /**
     * Получить все контракты как Flow
     */
    @Query("SELECT * FROM contracts")
    kotlinx.coroutines.flow.Flow<List<ContractEntity>> getAllContractsFlow();

    /**
     * Получить все контракты как LiveData
     */
    @Query("SELECT * FROM contracts")
    LiveData<List<ContractEntity>> getAllContractsLive();

    /**
     * Проверить наличие контракта по id
     */
    @Query("SELECT EXISTS(SELECT 1 FROM contracts WHERE id = :contractId)")
    boolean exists(long contractId);

    /**
     * Удалить контракт по id
     */
    @Query("DELETE FROM contracts WHERE id = :contractId")
    void deleteById(long contractId);

    /**
     * Удалить все контракты
     */
    @Query("DELETE FROM contracts")
    void deleteAll();

    @Transaction
    @Query("SELECT * FROM contracts WHERE userUuid = :uuid ORDER BY startDate DESC")
    LiveData<List<ContractEntity>> getContractsLive(String uuid);

    @Query("SELECT * FROM contracts WHERE userUuid = :uuid AND endDate IS NULL LIMIT 1")
    ContractEntity getActiveContract(String uuid);

    @Query("UPDATE contracts SET vesselName = :vesselName, position = :position WHERE id = :contractId")
    void updateVesselAndPosition(int contractId, String vesselName, String position);

    @Query("DELETE FROM contracts WHERE id = :contractId")
    void deleteContractById(int contractId);

    @Transaction
    @Query("SELECT * FROM contracts WHERE id = :contractId LIMIT 1")
    ContractEntity getContractById(int contractId);

    // Массовое удаление всех контрактов конкретного пользователя (например, при сбросе)
    @Query("DELETE FROM contracts WHERE userUuid = :uuid")
    void deleteAllForUser(String uuid);

    @Query("SELECT * FROM contracts WHERE userUuid = :uuid")
    List<ContractEntity> getContractsByUserUuid(String uuid);
}
