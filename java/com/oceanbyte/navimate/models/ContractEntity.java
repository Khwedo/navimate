package com.oceanbyte.navimate.models;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "contracts")
public class ContractEntity {

    @PrimaryKey(autoGenerate = true)
    public int id;

    @NonNull
    public String userUuid;

    public String vesselName;
    public String position;

    public Long startDate;
    public Long endDate;

    public ContractEntity() {
    }

    /** Контракт считается активным, если не указана дата окончания */
    public boolean isActive() {
        return endDate == null;
    }

    @NonNull
    @Override
    public String toString() {
        return "Contract{" +
                "vessel='" + vesselName + '\'' +
                ", position='" + position + '\'' +
                ", start=" + startDate +
                ", end=" + endDate +
                ", active=" + isActive() +
                '}';
    }
}
