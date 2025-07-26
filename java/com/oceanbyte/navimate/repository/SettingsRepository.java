package com.oceanbyte.navimate.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.oceanbyte.navimate.database.AppDatabase;
import com.oceanbyte.navimate.models.UserProfile;
import com.oceanbyte.navimate.utils.UserUtils;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SettingsRepository {

    private final AppDatabase db;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Context context;

    private UserProfile cachedProfile = null;

    public SettingsRepository(Context context) {
        this.context = context.getApplicationContext();
        db = AppDatabase.getInstance(context);
    }

    public interface ProfileCallback {
        void onProfileLoaded(UserProfile profile);
    }

    public interface SaveCallback {
        void onSaved();
    }

    public void loadProfile(ProfileCallback callback) {
        if (cachedProfile != null) {
            mainHandler.post(() -> callback.onProfileLoaded(cachedProfile));
            return;
        }

        executor.execute(() -> {
            UserProfile profile = db.userProfileDao().getById(1);
            if (profile == null) {
                profile = new UserProfile();
                profile.id = 1;
                profile.uuid = UserUtils.getUserUUID(context);
                profile.fullName = "";
                db.userProfileDao().insertOrUpdate(profile);
            }

            if (profile.fullName == null) {
                profile.fullName = "";
            }

            cachedProfile = profile;
            UserProfile finalProfile = profile;
            mainHandler.post(() -> callback.onProfileLoaded(finalProfile));
        });
    }

    public void saveProfile(String name, SaveCallback callback) {
        executor.execute(() -> {
            UserProfile profile = db.userProfileDao().getById(1);
            if (profile == null) {
                profile = new UserProfile();
                profile.id = 1;
                profile.uuid = UserUtils.getUserUUID(context);
            }

            profile.fullName = name;
            db.userProfileDao().insertOrUpdate(profile);
            cachedProfile = profile;
            mainHandler.post(callback::onSaved);
        });
    }

    public void clearCache() {
        cachedProfile = null;
    }

    public void ensureUserProfileExists() {
        executor.execute(() -> {
            UserProfile profile = db.userProfileDao().getById(1);
            if (profile == null) {
                UserProfile newProfile = new UserProfile();
                newProfile.id = 1;
                newProfile.uuid = UUID.randomUUID().toString();
                newProfile.fullName = "";
                db.userProfileDao().insertOrUpdate(newProfile);
                cachedProfile = newProfile;
            }
        });
    }
}
