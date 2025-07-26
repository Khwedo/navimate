package com.oceanbyte.navimate.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.oceanbyte.navimate.models.UserProfile;

@Dao
public interface UserProfileDao {

    // Вставка или обновление (используется REPLACE)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdate(UserProfile profile);

    // Явное обновление (может использоваться при частичном редактировании)
    @Update
    void update(UserProfile profile);

    // Получение профиля по ID (всегда id = 1)
    @Query("SELECT * FROM user_profile WHERE id = :id LIMIT 1")
    UserProfile getById(long id);

    // LiveData-поток (если используется в ViewModel)
    @Query("SELECT * FROM user_profile WHERE id = 1 LIMIT 1")
    LiveData<UserProfile> getLiveProfile();
}
