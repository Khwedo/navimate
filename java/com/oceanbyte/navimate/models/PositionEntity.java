package com.oceanbyte.navimate.models;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Сущность "Должность" (PositionEntity) для справочника должностей в базе данных.
 */
@Entity(tableName = "positions")
public class PositionEntity {

    @PrimaryKey
    @NonNull
    public String name;

    public PositionEntity(@NonNull String name) {
        this.name = name.trim();
    }

    @NonNull
    @Override
    public String toString() {
        return name;
    }
}
