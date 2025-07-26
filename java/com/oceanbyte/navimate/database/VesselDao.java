package com.oceanbyte.navimate.database;


import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.oceanbyte.navimate.models.Vessel;

import java.util.List;

@Dao
public interface VesselDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertAll(List<Vessel> vessels);

    @Query("SELECT * FROM Vessel WHERE name LIKE :query || '%' ORDER BY name")
    List<Vessel> searchByName(String query);

    @Query("SELECT * FROM Vessel ORDER BY name")
    List<Vessel> getAllVessels();


}