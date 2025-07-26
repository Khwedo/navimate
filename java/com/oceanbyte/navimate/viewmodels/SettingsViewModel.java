package com.oceanbyte.navimate.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.oceanbyte.navimate.R;
import com.oceanbyte.navimate.models.ContractEntity;
import com.oceanbyte.navimate.repository.ContractRepository;
import com.oceanbyte.navimate.models.UserProfile;
import com.oceanbyte.navimate.repository.SettingsRepository;

import java.util.concurrent.Executors;

public class SettingsViewModel extends AndroidViewModel {

    private final SettingsRepository repository;
    private final ContractRepository contractRepository;

    private final MediatorLiveData<UserProfile> cachedProfile = new MediatorLiveData<>();
    private final MutableLiveData<ContractEntity> activeContractLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessageLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> saveSuccessLiveData = new MutableLiveData<>();

    private boolean isProfileLoaded = false;

    public SettingsViewModel(@NonNull Application application) {
        super(application);
        this.repository = new SettingsRepository(application);
        this.contractRepository = new ContractRepository(application);
    }

    public LiveData<UserProfile> getUserProfile() {
        return cachedProfile;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessageLiveData;
    }

    public LiveData<Boolean> getSaveSuccess() {
        return saveSuccessLiveData;
    }

    public void loadCachedUserProfile() {
        if (!isProfileLoaded) {
            repository.loadProfile(profile -> {
                if (profile != null && !profile.fullName.isEmpty()) {
                    cachedProfile.postValue(profile);
                    isProfileLoaded = true;
                } else {
                    errorMessageLiveData.postValue(getApplication().getString(R.string.fault_launch_profile));
                }
            });
        }
    }

    public void reloadUserProfile() {
        clearCache();
        loadCachedUserProfile();
    }

    public void saveUserProfile(@NonNull UserProfile updatedProfile) {
        String newName = updatedProfile.fullName == null ? "" : updatedProfile.fullName.trim();

        if (newName.isEmpty()) {
            errorMessageLiveData.postValue(getApplication().getString(R.string.error_empty_name));
            saveSuccessLiveData.postValue(false);
            return;
        }

        UserProfile current = cachedProfile.getValue();
        if (current != null && newName.equals(current.fullName)) {
            errorMessageLiveData.postValue(getApplication().getString(R.string.no_changes));
            saveSuccessLiveData.postValue(false);
            return;
        }

        repository.saveProfile(newName, () -> {
            // После сохранения принудительно перезагружаем профиль из базы
            repository.loadProfile(profile -> {
                if (profile != null) {
                    cachedProfile.postValue(profile);
                    isProfileLoaded = true;
                    saveSuccessLiveData.postValue(true);
                } else {
                    errorMessageLiveData.postValue(getApplication().getString(R.string.fault_launch_profile));
                    saveSuccessLiveData.postValue(false);
                }
            });
        });
    }

    public void updateCachedProfileName(String newName) {
        UserProfile profile = cachedProfile.getValue();
        if (profile != null && newName != null && !newName.trim().isEmpty()) {
            profile.fullName = newName.trim();
            cachedProfile.setValue(profile);
        }
    }

    public void clearCache() {
        cachedProfile.setValue(null);
        isProfileLoaded = false;
    }

    public LiveData<ContractEntity> getActiveContract() {
        if (activeContractLiveData.getValue() == null) {
            loadActiveContract();
        }
        return activeContractLiveData;
    }

    private void loadActiveContract() {
        Executors.newSingleThreadExecutor().execute(() -> {
            ContractEntity active = contractRepository.getActiveContractSync();
            activeContractLiveData.postValue(active);
        });
    }

    public void resetSaveSuccess() {
        saveSuccessLiveData.setValue(null);
    }
}
