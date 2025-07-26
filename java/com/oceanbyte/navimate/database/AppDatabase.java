package com.oceanbyte.navimate.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.room.TypeConverters;

import com.oceanbyte.navimate.database.contract.ContractDao;
import com.oceanbyte.navimate.models.ContractEntity;
import com.oceanbyte.navimate.models.JobReport;
import com.oceanbyte.navimate.models.PositionEntity;
import com.oceanbyte.navimate.models.UserProfile;
import com.oceanbyte.navimate.models.Vessel;
import com.oceanbyte.navimate.utils.DateConverter;

@Database(entities = {
        JobReport.class,
        UserProfile.class,
        ContractEntity.class,
        Vessel.class,
        PositionEntity.class
}, version = 5)

@TypeConverters({DateConverter.class, Converters.class})
public abstract class AppDatabase extends RoomDatabase {

    private static AppDatabase instance;

    public abstract ReportDao reportDao();
    public abstract UserProfileDao userProfileDao();
    public abstract VesselDao vesselDao();
    public abstract PositionDao positionDao();
    public abstract ContractDao contractDao();

    // === Миграции базы данных ===
    private static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(SupportSQLiteDatabase db) {
            // Пример: db.execSQL("ALTER TABLE UserProfile ADD COLUMN fullName TEXT");
        }
    };

    private static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(SupportSQLiteDatabase db) {
            // Пример: db.execSQL("CREATE TABLE IF NOT EXISTS `ContractEntity` (...)");
        }
    };

    private static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(SupportSQLiteDatabase db) {
            // Пример: db.execSQL("ALTER TABLE JobReport ADD COLUMN contractId INTEGER DEFAULT 0");
        }
    };

    // Массив всех миграций для удобства
    private static final Migration[] MIGRATIONS = new Migration[] {
        MIGRATION_1_2,
        MIGRATION_2_3,
        MIGRATION_3_4
    };

    /**
     * Получить синглтон AppDatabase. Потокобезопасно.
     */
    public static AppDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    try {
                        instance = Room.databaseBuilder(context.getApplicationContext(),
                                AppDatabase.class, "navimate_db")
                                .addMigrations(MIGRATIONS)
                                .build();
                    } catch (Exception e) {
                        // Логирование ошибки и обработка
                        e.printStackTrace();
                        throw new RuntimeException("Ошибка инициализации базы данных", e);
                    }
                }
            }
        }
        return instance;
    }

    /**
     * Для тестов: создать in-memory базу
     */
    public static AppDatabase createInMemory(Context context) {
        return Room.inMemoryDatabaseBuilder(context.getApplicationContext(), AppDatabase.class)
                .allowMainThreadQueries()
                .build();
    }

    /**
     * Вызывается при первом запуске приложения
     * (например, из Application или SplashActivity)
     * для создания пользовательского профиля, должностей и т.п.
     */
    public void prepopulateIfNeeded() {
        // Пример вызова:
        // getInstance(context).userProfileDao().insertOrUpdate(...);
        // getInstance(context).positionDao().insertAll(...);
    }
}
