package com.oceanbyte.navimate.utils;

import android.content.Context;

import com.oceanbyte.navimate.database.AppDatabase;
import com.oceanbyte.navimate.models.Vessel;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CompanySeeder {
    public static void seed(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        ExecutorService executor = Executors.newSingleThreadExecutor();

        executor.execute(() -> {
            if (db.vesselDao().getAllVessels().isEmpty()) {
                // Пример компаний
                List<Vessel> vessels = Arrays.asList(
                        new Vessel("Maersk"),
                        new Vessel("MSC"),
                        new Vessel("Oceanic"),
                        new Vessel("NSC")
                );
                db.vesselDao().insertAll(vessels);


            }
        });
    }
}
