package com.oceanbyte.navimate.repository;

import android.app.Application;

import com.oceanbyte.navimate.database.AppDatabase;
import com.oceanbyte.navimate.database.PositionDao;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Репозиторий для загрузки должностей из локальной базы данных.
 */
public class PositionRepository {

    private final PositionDao dao;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public PositionRepository(Application application) {
        dao = AppDatabase.getInstance(application).positionDao();
    }

    public interface TitlesCallback {
        void onLoaded(List<String> titles);
    }

    public void getAllTitlesAsync(TitlesCallback callback) {
        executor.execute(() -> {
            List<String> titles = dao.getAllTitles();
            if (callback != null) {
                callback.onLoaded(titles);
            }
        });
    }
}
