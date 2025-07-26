package com.oceanbyte.navimate.view.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.*;
import android.widget.*;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.oceanbyte.navimate.R;
import com.oceanbyte.navimate.auth.LoginActivity;
import com.oceanbyte.navimate.models.UserProfile;
import com.oceanbyte.navimate.utils.ExportUtils;
import com.oceanbyte.navimate.utils.KeyboardUtils;
import com.oceanbyte.navimate.utils.LanguageUtils;
import com.oceanbyte.navimate.viewmodels.SettingsViewModel;

import java.io.*;

public class SettingsFragment extends Fragment {

    private Spinner languageSpinner, themeSpinner;
    private Button btnContracts, btnLogout, btnChangePassword;
    private Switch switchPush;
    private TextView textFullName, emailText, textTemplateStatus;
    private View rootView;

    private SettingsViewModel viewModel;
    private SharedPreferences prefs;

    private final String[] languages = {"Русский", "English"};
    private final String[] themes = {"Светлая", "Тёмная", "Системная"};
    private static final String TEMPLATE_NAME = "template.docx";

    private ActivityResultLauncher<Intent> templatePickerLauncher;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_settings, container, false);

        initViews(rootView);
        setupSpinners();
        setupEmailAndPush();
        setupListeners();

        viewModel = new ViewModelProvider(requireActivity()).get(SettingsViewModel.class);

        observeViewModel();
        viewModel.loadCachedUserProfile();

        KeyboardUtils.setupHideKeyboardOnTouch(requireActivity(), rootView);

        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null && !message.isEmpty()) {
                Snackbar.make(rootView, message, Snackbar.LENGTH_LONG).show();
            }
        });

        templatePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        String mimeType = requireContext().getContentResolver().getType(uri);
                        if (!"application/vnd.openxmlformats-officedocument.wordprocessingml.document".equals(mimeType)) {
                            Snackbar.make(rootView, getString(R.string.only_docx_allowed), Snackbar.LENGTH_LONG).show();
                            return;
                        }
                        saveTemplateToStorage(uri);
                    }
                });

        rootView.findViewById(R.id.btnUploadTemplate).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.setType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            templatePickerLauncher.launch(intent);
        });

        rootView.findViewById(R.id.btnDeleteTemplate).setOnClickListener(v -> {
            File file = new File(ExportUtils.getTemplateDirectory(requireContext()), TEMPLATE_NAME);
            if (file.exists()) {
                boolean deleted = file.delete();
                showSnackbar(deleted ? getString(R.string.template_deleted) : getString(R.string.template_delete_error));
            } else {
                showSnackbar(getString(R.string.template_not_found));
            }
            updateTemplateStatus();
        });

        updateTemplateStatus();

        return rootView;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE);
    }

    private void initViews(View view) {
        textFullName = view.findViewById(R.id.textFullName);
        btnContracts = view.findViewById(R.id.btnContracts);
        languageSpinner = view.findViewById(R.id.languageSpinner);
        themeSpinner = view.findViewById(R.id.themeSpinner);
        switchPush = view.findViewById(R.id.switchPush);
        emailText = view.findViewById(R.id.textUserEmail);
        btnLogout = view.findViewById(R.id.btnLogout);
        btnChangePassword = view.findViewById(R.id.btnChangePassword);
        textTemplateStatus = view.findViewById(R.id.textTemplateStatus);
    }

    private void setupSpinners() {
        ArrayAdapter<String> langAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, languages);
        langAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        languageSpinner.setAdapter(langAdapter);
        String currentLang = prefs.getString("app_language", "ru");
        languageSpinner.setSelection(currentLang.equals("ru") ? 0 : 1);
        languageSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                String selectedLang = (pos == 0) ? "ru" : "en";
                String current = LanguageUtils.getSavedLanguage(requireContext());
                if (!selectedLang.equals(current)) {
                    LanguageUtils.saveLanguage(requireContext(), selectedLang);
                    LanguageUtils.changeAppLanguage(requireActivity(), selectedLang);
                }
            }
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        ArrayAdapter<String> themeAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, themes);
        themeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        themeSpinner.setAdapter(themeAdapter);
        int themeIndex = prefs.getInt("theme_index", 0);
        themeSpinner.setSelection(themeIndex);
        themeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                prefs.edit().putInt("theme_index", pos).apply();
                switch (pos) {
                    case 0: AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO); break;
                    case 1: AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES); break;
                    case 2: AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM); break;
                }
            }
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        btnContracts.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragmentContainer, new ContractListFragment())
                    .addToBackStack(null)
                    .commit();
        });
    }

    private void observeViewModel() {
        viewModel.getUserProfile().observe(getViewLifecycleOwner(), profile -> {
            if (profile != null) {
                textFullName.setText(profile.fullName);
            }
        });
    }

    private void setupEmailAndPush() {
        String email = prefs.getString("user_email", null);
        if (email != null) emailText.setText(email);

        boolean pushEnabled = prefs.getBoolean("push_enabled", true);
        switchPush.setChecked(pushEnabled);
        switchPush.setOnCheckedChangeListener((button, checked) ->
                prefs.edit().putBoolean("push_enabled", checked).apply()
        );
    }

    private void setupListeners() {
        textFullName.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragmentContainer, new EditNameFragment())
                    .addToBackStack("EditName")
                    .commit();
        });

        btnLogout.setOnClickListener(v -> {
            new AlertDialog.Builder(requireContext())
                    .setTitle(R.string.logout)
                    .setMessage(R.string.logout_confirm)
                    .setPositiveButton(R.string.yes, (dialog, which) -> logout())
                    .setNegativeButton(R.string.cancel, null)
                    .show();
        });

        btnChangePassword.setOnClickListener(v -> resetPassword());
    }

    private void resetPassword() {
        String email = prefs.getString("user_email", null);
        if (email != null) {
            FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                    .addOnSuccessListener(aVoid -> showSnackbar(getString(R.string.message_sended)))
                    .addOnFailureListener(e -> showSnackbar(getString(R.string.error) + e.getMessage()));
        }
    }

    private void logout() {
        FirebaseAuth.getInstance().signOut();
        prefs.edit().clear().apply();
        Intent intent = new Intent(requireContext(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
    }

    private void showSnackbar(String message) {
        Snackbar.make(rootView, message, Snackbar.LENGTH_SHORT).show();
    }

    private void saveTemplateToStorage(Uri uri) {
        try (InputStream in = requireContext().getContentResolver().openInputStream(uri)) {
            File dir = ExportUtils.getTemplateDirectory(requireContext());
            File outFile = new File(dir, TEMPLATE_NAME);
            try (OutputStream out = new FileOutputStream(outFile)) {
                byte[] buffer = new byte[4096];
                int length;
                while ((length = in.read(buffer)) > 0) {
                    out.write(buffer, 0, length);
                }
            }
            showSnackbar(getString(R.string.template_loaded));
            updateTemplateStatus();
        } catch (Exception e) {
            e.printStackTrace();
            showSnackbar(getString(R.string.template_delete_error));
        }
    }

    private void updateTemplateStatus() {
        File file = new File(ExportUtils.getTemplateDirectory(requireContext()), TEMPLATE_NAME);
        textTemplateStatus.setText(file.exists()
                ? getString(R.string.template_loaded_status)
                : getString(R.string.template_not_loaded));
    }
}
