package com.oceanbyte.navimate.utils;

import android.content.Context;
import android.util.Log;

import com.oceanbyte.navimate.database.AppDatabase;
import com.oceanbyte.navimate.models.PositionEntity;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PositionSeeder {

    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    public static void seed(Context context) {
        executor.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(context);
            List<String> positions = Arrays.asList(
                    "Captain", "Chief Engineer", "Second Engineer", "Third Engineer",
                    "Electrical Engineer", "ETO", "Bosun", "Motorman", "Oiler",
                    "Cook", "Steward", "Deck Cadet", "Engine Cadet", "Chief Officer",
                    "Second Officer", "Third Officer", "Welder", "Fitter", "Pumpman",
                    "Crane Operator", "Rigger", "Able Seaman", "Ordinary Seaman",
                    "Chief Electrician", "Junior Electrician", "Mechanic", "Turner",
                    "Hydraulic Engineer", "Radio Officer", "Storekeeper", "Painter",
                    "Reeferman", "Watchman", "Assistant Cook", "Messman",
                    "Chief Cook", "Deck Fitter", "Engine Fitter", "Plumber", "Loader"
            );

            for (String position : positions) {
                db.positionDao().insertIfNotExists(new PositionEntity(position));
            }

            Log.d("PositionSeeder", "Positions seeded.");
        });
    }
}
