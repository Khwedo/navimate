package com.oceanbyte.navimate.view.fragments;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.*;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.snackbar.Snackbar;
import com.oceanbyte.navimate.R;
import com.oceanbyte.navimate.models.UserProfile;
import com.oceanbyte.navimate.utils.KeyboardUtils;
import com.oceanbyte.navimate.utils.UserUtils;
import com.oceanbyte.navimate.viewmodels.SettingsViewModel;

public class EditNameFragment extends Fragment {

    private EditText editName;
    private Button btnSave;
    private View rootView;
    private SettingsViewModel viewModel;

    private String currentName = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_edit_name, container, false);
        editName = rootView.findViewById(R.id.editName);
        btnSave = rootView.findViewById(R.id.btnSave);

        viewModel = new ViewModelProvider(requireActivity()).get(SettingsViewModel.class);

        observeViewModel();
        KeyboardUtils.setupHideKeyboardOnTouch(requireActivity(), rootView);

        btnSave.setOnClickListener(v -> saveName());

        return rootView;
    }

    private void observeViewModel() {
        viewModel.getUserProfile().observe(getViewLifecycleOwner(), profile -> {
            if (profile != null && TextUtils.isEmpty(editName.getText().toString())) {
                currentName = profile.fullName;
                editName.setText(currentName);
            }
        });

        viewModel.getSaveSuccess().observe(getViewLifecycleOwner(), success -> {
            if (Boolean.TRUE.equals(success)) {
                Snackbar.make(rootView, getString(R.string.profile_saved), Snackbar.LENGTH_SHORT).show();
                // Сбросить флаг, чтобы повторный вход не срабатывал автоматически
                viewModel.resetSaveSuccess();
                requireActivity().getSupportFragmentManager().popBackStack();
            }
        });

        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), msg -> {
            if (msg != null && !msg.isEmpty()) {
                Snackbar.make(rootView, msg, Snackbar.LENGTH_SHORT).show();
            }
        });
    }

    private void saveName() {
        String newName = editName.getText().toString().trim();

        if (TextUtils.isEmpty(newName)) {
            editName.setError(getString(R.string.error_empty_name));
            return;
        }

        if (newName.equals(currentName)) {
            Snackbar.make(rootView, getString(R.string.no_changes), Snackbar.LENGTH_SHORT).show();
            return;
        }

        String uuid = UserUtils.getUserUUID(requireContext());
        if (uuid == null || uuid.isEmpty()) {
            Snackbar.make(rootView, getString(R.string.error_uuid_missing), Snackbar.LENGTH_LONG).show();
            return;
        }

        UserProfile updatedProfile = new UserProfile(uuid, newName);
        viewModel.saveUserProfile(updatedProfile);
    }
}
