package com.oceanbyte.navimate.models;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "user_profile")
public class UserProfile {

    @PrimaryKey
    @ColumnInfo(name = "id")
    public long id = 1;  // Одиночный профиль

    @NonNull
    @ColumnInfo(name = "uuid")
    public String uuid;  // Уникальный идентификатор пользователя (устанавливается при первом запуске)

    @NonNull
    @ColumnInfo(name = "full_name")
    public String fullName; // Полное имя пользователя

    @Ignore
    public boolean isFirstLaunch = true; // Не сохраняется в базу данных

    // Конструктор по умолчанию (обязателен для Room)
    public UserProfile() {
        this.uuid = "";
        this.fullName = "";
    }

    // Основной конструктор
    public UserProfile(@NonNull String uuid, @NonNull String fullName) {
        this.uuid = uuid;
        this.fullName = fullName;
    }
}
