package com.oceanbyte.navimate.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.oceanbyte.navimate.models.PositionEntity;

import java.util.List;

@Dao
public interface PositionDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE) // чтобы не перезаписывать существующие
    void insertIfNotExists(PositionEntity position);

    @Query("SELECT * FROM positions WHERE name LIKE :query || '%' ORDER BY name")
    List<PositionEntity> searchByName(String query);

    @Query("SELECT * FROM positions ORDER BY name")
    List<PositionEntity> getAll();

    @Query("SELECT name FROM positions ORDER BY name")
    List<String> getAllTitles();
}
