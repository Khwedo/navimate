package com.oceanbyte.navimate.view;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.jakewharton.threetenabp.AndroidThreeTen;
import com.oceanbyte.navimate.R;
import com.oceanbyte.navimate.auth.LoginActivity;
import com.oceanbyte.navimate.utils.CompanySeeder;
import com.oceanbyte.navimate.utils.KeyboardUtils;
import com.oceanbyte.navimate.utils.LanguageUtils;
import com.oceanbyte.navimate.utils.NotificationPermissionHelper;
import com.oceanbyte.navimate.utils.PositionSeeder;
import com.oceanbyte.navimate.view.fragments.AddContractFragment;
import com.oceanbyte.navimate.view.fragments.AiFragment;
import com.oceanbyte.navimate.view.fragments.FinanceFragment;
import com.oceanbyte.navimate.view.fragments.HomeFragment;
import com.oceanbyte.navimate.view.fragments.SettingsFragment;
import com.oceanbyte.navimate.view.fragments.ViewReportsFragment;
import com.oceanbyte.navimate.viewmodels.SettingsViewModel;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView navView;
    private static final String KEY_SELECTED_NAV_ITEM = "selected_nav_item_id";
    private int selectedNavItemId = R.id.home;

    private final String TAG_HOME = "tag_home";
    private final String TAG_REPORTS = "tag_reports";
    private final String TAG_FINANCE = "tag_finance";
    private final String TAG_AI = "tag_ai";
    private final String TAG_SETTINGS = "tag_settings";

    private String currentTag = TAG_HOME;
    private final Map<String, Fragment> rootFragments = new HashMap<>();

    private ActivityResultLauncher<String> notificationPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        applySavedTheme();
        super.onCreate(savedInstanceState);
        Log.d("MainActivity", "onCreate called");

        AndroidThreeTen.init(this);

        notificationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        Toast.makeText(this, "Разрешение на уведомления предоставлено", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Уведомления отключены. Вы не получите напоминания.", Toast.LENGTH_LONG).show();
                    }
                }
        );
        NotificationPermissionHelper.requestNotificationPermissionIfNeeded(this, notificationPermissionLauncher);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            new AlertDialog.Builder(this)
                    .setTitle("Неподдерживаемая версия Android")
                    .setMessage("Для использования NaviMate требуется Android 8.0 (API 26) или выше.")
                    .setCancelable(false)
                    .setPositiveButton("Выход", (dialog, which) -> finish())
                    .show();
            return;
        }

        SharedPreferences prefs = getSharedPreferences("app_settings", MODE_PRIVATE);
        String email = prefs.getString("user_email", null);
        if (email == null || email.isEmpty()) {
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        LanguageUtils.applySavedLanguage(this);
        setContentView(R.layout.activity_main);
        KeyboardUtils.setupHideKeyboardOnTouch(this, findViewById(android.R.id.content));

        navView = findViewById(R.id.bottomNavigationView);

        selectedNavItemId = (savedInstanceState != null)
                ? savedInstanceState.getInt(KEY_SELECTED_NAV_ITEM, R.id.home)
                : loadSelectedNavItem();

        resetFragments();

        if (!prefs.getBoolean("positions_seeded", false)) {
            PositionSeeder.seed(getApplicationContext());
            prefs.edit().putBoolean("positions_seeded", true).apply();
        }
        CompanySeeder.seed(this);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_SELECTED_NAV_ITEM, selectedNavItemId);
    }

    private void applySavedTheme() {
        SharedPreferences prefs = getSharedPreferences("app_settings", MODE_PRIVATE);
        String theme = prefs.getString("app_theme", "system");

        switch (theme) {
            case "light":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case "dark":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            default:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
        }
    }

    private void saveSelectedNavItem(int itemId) {
        getSharedPreferences("app_state", MODE_PRIVATE)
                .edit()
                .putInt("selected_nav_item", itemId)
                .apply();
    }

    private int loadSelectedNavItem() {
        return getSharedPreferences("app_state", MODE_PRIVATE)
                .getInt("selected_nav_item", R.id.home);
    }

    @Override
    protected void onStart() {
        super.onStart();
        checkActiveContract();
    }

    private void checkActiveContract() {
        SettingsViewModel settingsViewModel = new ViewModelProvider(this).get(SettingsViewModel.class);
        settingsViewModel.getActiveContract().observe(this, contract -> {
            if (contract == null) {
                showAddContractDialog();
            }
        });
    }

    private void showAddContractDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Добавьте активный контракт")
                .setMessage("Чтобы продолжить работу, нужно создать активный контракт.")
                .setPositiveButton("Хорошо", (dialog, which) -> openAddContractFragment())
                .setCancelable(false)
                .show();
    }

    private void openAddContractFragment() {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainer, new AddContractFragment())
                .addToBackStack(null)
                .commit();
    }

    private void resetFragments() {
        FragmentManager fm = getSupportFragmentManager();

        for (Fragment fragment : fm.getFragments()) {
            fm.beginTransaction().remove(fragment).commitNowAllowingStateLoss();
        }

        rootFragments.clear();
        rootFragments.put(TAG_HOME, new HomeFragment());
        rootFragments.put(TAG_REPORTS, new ViewReportsFragment());
        rootFragments.put(TAG_FINANCE, new FinanceFragment());
        rootFragments.put(TAG_AI, new AiFragment());
        rootFragments.put(TAG_SETTINGS, new SettingsFragment());

        FragmentTransaction transaction = fm.beginTransaction();
        transaction.add(R.id.fragmentContainer, rootFragments.get(TAG_HOME), TAG_HOME).hide(rootFragments.get(TAG_HOME));
        transaction.add(R.id.fragmentContainer, rootFragments.get(TAG_REPORTS), TAG_REPORTS).hide(rootFragments.get(TAG_REPORTS));
        transaction.add(R.id.fragmentContainer, rootFragments.get(TAG_FINANCE), TAG_FINANCE).hide(rootFragments.get(TAG_FINANCE));
        transaction.add(R.id.fragmentContainer, rootFragments.get(TAG_AI), TAG_AI).hide(rootFragments.get(TAG_AI));
        transaction.add(R.id.fragmentContainer, rootFragments.get(TAG_SETTINGS), TAG_SETTINGS).hide(rootFragments.get(TAG_SETTINGS));

        String selectedTag;
        int itemId = selectedNavItemId;

        if (itemId == R.id.reports) {
            selectedTag = TAG_REPORTS;
        } else if (itemId == R.id.finance) {
            selectedTag = TAG_FINANCE;
        } else if (itemId == R.id.ai) {
            selectedTag = TAG_AI;
        } else if (itemId == R.id.settings) {
            selectedTag = TAG_SETTINGS;
        } else {
            selectedTag = TAG_HOME;
        }

        currentTag = selectedTag;
        transaction.show(rootFragments.get(currentTag));
        transaction.commitNow();

        navView.setSelectedItemId(selectedNavItemId);

        navView.setOnItemSelectedListener(item -> {
            selectedNavItemId = item.getItemId();
            saveSelectedNavItem(selectedNavItemId);

            String newTag;
            int id = item.getItemId();

            if (id == R.id.reports) {
                newTag = TAG_REPORTS;
            } else if (id == R.id.finance) {
                newTag = TAG_FINANCE;
            } else if (id == R.id.ai) {
                newTag = TAG_AI;
            } else if (id == R.id.settings) {
                newTag = TAG_SETTINGS;
            } else {
                newTag = TAG_HOME;
            }

            if (!newTag.equals(currentTag)) {
                fm.beginTransaction()
                        .hide(rootFragments.get(currentTag))
                        .show(rootFragments.get(newTag))
                        .commit();
                fm.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                currentTag = newTag;
            }

            return true;
        });
    }
}
