package com.oceanbyte.navimate.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.oceanbyte.navimate.models.JobReport;

import java.util.List;

@Dao
public interface ReportDao {

    /** Вставка нового отчёта */
    @Insert
    void insert(JobReport report);

    /** Обновление отчёта */
    @Update
    void update(JobReport report);

    /** Удаление отчёта */
    @Delete
    void delete(JobReport report);

    /** Удаление по ID (для пакетного удаления) */
    @Query("DELETE FROM JobReport WHERE id = :reportId")
    void deleteById(long reportId);

    /** Получение отчета по ID */
    @Query("SELECT * FROM JobReport WHERE id = :id")
    JobReport getById(long id);

    /** Получение всех отчетов по контракту (LiveData) */
    @Query("SELECT * FROM JobReport WHERE contract_id = :contractId ORDER BY created_at DESC")
    LiveData<List<JobReport>> getReportsByContractLive(int contractId);

    /** Получение всех отчетов (LiveData) */
    @Query("SELECT * FROM JobReport ORDER BY created_at DESC")
    LiveData<List<JobReport>> getAllReportsLive();

    /** Получение количества отчетов по контракту */
    @Query("SELECT COUNT(*) FROM JobReport WHERE contract_id = :contractId")
    int getReportCountForContract(int contractId);

    @Query("SELECT * FROM JobReport WHERE contract_id = :contractId ORDER BY created_at DESC LIMIT :limit OFFSET :offset")
    List<JobReport> getReportsByContractPaged(int contractId, int limit, int offset);

}
