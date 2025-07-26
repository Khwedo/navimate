package com.oceanbyte.navimate.models;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class Vessel {
    @PrimaryKey(autoGenerate = true)
    public long id;


    @NonNull
    public String name;

    public Vessel(@NonNull String name) {
        this.name = name;
    }
}

