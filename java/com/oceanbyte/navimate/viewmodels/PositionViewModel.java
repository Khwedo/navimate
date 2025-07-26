package com.oceanbyte.navimate.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.oceanbyte.navimate.repository.PositionRepository;

import java.util.List;

/**
 * ViewModel для управления должностями. Используется для автозаполнения при вводе контракта.
 */
public class PositionViewModel extends AndroidViewModel {

    private final PositionRepository repository;
    private final MutableLiveData<List<String>> positionTitles = new MutableLiveData<>();

    public PositionViewModel(@NonNull Application application) {
        super(application);
        repository = new PositionRepository(application);
        loadPositionTitles();
    }

    public LiveData<List<String>> getAllPositionTitles() {
        return positionTitles;
    }

    private void loadPositionTitles() {
        repository.getAllTitlesAsync(positionTitles::postValue);
    }
}
