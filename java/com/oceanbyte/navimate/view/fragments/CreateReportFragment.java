package com.oceanbyte.navimate.view.fragments;

import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.ContentValues;
import android.icu.util.Calendar;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.*;
import android.widget.*;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.google.android.flexbox.FlexboxLayout;
import com.google.android.material.snackbar.Snackbar;
import com.oceanbyte.navimate.R;
import com.oceanbyte.navimate.utils.KeyboardUtils;
import com.oceanbyte.navimate.utils.PhotoOpener;
import com.oceanbyte.navimate.utils.UserUtils;
import com.oceanbyte.navimate.viewmodels.CreateReportViewModel;
import com.oceanbyte.navimate.models.ContractEntity;

import java.io.File;
import java.text.DateFormat;
import java.util.*;

public class CreateReportFragment extends Fragment {

    private EditText editJobTitle, editEquipment, editNote;
    private FlexboxLayout containerBefore, containerAfter;
    private Button btnSave, btnSetReminder, btnClearReminder;
    private TextView textReminderTime;
    private AlertDialog loadingDialog;
    private long reminderTimestamp = 0L;

    private CreateReportViewModel viewModel;

    private ActivityResultLauncher<String> galleryLauncher;
    private ActivityResultLauncher<Uri> cameraLauncher;
    private Uri cameraImageUri;
    private boolean isBeforePhoto = true;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_create_report, container, false);
        KeyboardUtils.setupHideKeyboardOnTouch(requireActivity(), view);

        viewModel = new ViewModelProvider(this).get(CreateReportViewModel.class);

        editJobTitle = view.findViewById(R.id.editJobTitle);
        editEquipment = view.findViewById(R.id.editEquipment);
        editNote = view.findViewById(R.id.editNote);
        containerBefore = view.findViewById(R.id.containerBeforePhotos);
        containerAfter = view.findViewById(R.id.containerAfterPhotos);
        btnSetReminder = view.findViewById(R.id.btnSetReminder);
        textReminderTime = view.findViewById(R.id.textReminderTime);
        btnClearReminder = view.findViewById(R.id.btnClearReminder);
        btnSave = view.findViewById(R.id.btnSaveReport);

        btnSetReminder.setOnClickListener(v -> showTimePickerDialog());

        btnClearReminder.setOnClickListener(v -> {
            viewModel.setReminderTime(0L);
            textReminderTime.setText(R.string.reminder_not_set);
            showSnackbar(getString(R.string.reminder_deleted));
        });
        viewModel.loadActiveContract(UserUtils.getUserUUID(requireContext()));
        btnSave.setOnClickListener(v -> {
            String jobTitle = editJobTitle.getText().toString().trim();
            String equipment = editEquipment.getText().toString().trim();
            String userNote = editNote.getText().toString().trim();

            if (jobTitle.isEmpty() || equipment.isEmpty()) {
                showSnackbar(getString(R.string.fill_all_fields));
                return;
            }

            ContractEntity contract = viewModel.getActiveContractLiveData().getValue();
            if (contract == null) {
                showSnackbar(getString(R.string.contract_not_loaded));
                return;
            }

            viewModel.setJobDetails(jobTitle, equipment);
            viewModel.setUserNote(userNote);
            viewModel.saveReport();
        });

        view.findViewById(R.id.btnAddBeforePhoto).setOnClickListener(v -> showPhotoSourceDialog(true));
        view.findViewById(R.id.btnAddAfterPhoto).setOnClickListener(v -> showPhotoSourceDialog(false));

        galleryLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                viewModel.compressAndAddPhoto(requireContext(), uri, isBeforePhoto);
            }
        });

        cameraLauncher = registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
            if (success && cameraImageUri != null) {
                viewModel.compressAndAddPhoto(requireContext(), cameraImageUri, isBeforePhoto);
            }
        });

        observeViewModel();

        return view;
    }

    private void showPhotoSourceDialog(boolean isBefore) {
        this.isBeforePhoto = isBefore;
        int photoCount = isBefore
                ? viewModel.getBeforePhotos().getValue().size()
                : viewModel.getAfterPhotos().getValue().size();

        if (photoCount >= 6) {
            showSnackbar(getString(R.string.max_6_photos));
            return;
        }

        if (!isAdded()) return;

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.add_photo)
                .setItems(new String[]{
                        getString(R.string.take_photo),
                        getString(R.string.choose_from_gallery)
                }, (dialog, which) -> {
                    if (which == 0) openCamera();
                    else galleryLauncher.launch("image/*");
                })
                .show();
    }

    private void openCamera() {
        String fileName = "photo_" + System.currentTimeMillis() + ".jpg";
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        cameraImageUri = requireContext().getContentResolver()
                .insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        cameraLauncher.launch(cameraImageUri);
    }

    private void observeViewModel() {
        viewModel.getBeforePhotos().observe(getViewLifecycleOwner(), this::updateBeforePhotos);
        viewModel.getAfterPhotos().observe(getViewLifecycleOwner(), this::updateAfterPhotos);

        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            if (!isAdded()) return;
            btnSave.setEnabled(!Boolean.TRUE.equals(isLoading));
            if (Boolean.TRUE.equals(isLoading)) {
                showLoadingDialog();
            } else {
                hideLoadingDialog();
            }
        });

        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), this::showSnackbar);

        viewModel.getReminderTime().observe(getViewLifecycleOwner(), timeMillis -> {
            if (timeMillis != null && timeMillis > 0) {
                Calendar cal = Calendar.getInstance();
                cal.setTimeInMillis(timeMillis);
                DateFormat timeFormat = android.text.format.DateFormat.getTimeFormat(requireContext());
                textReminderTime.setText(getString(R.string.remind_at, timeFormat.format(cal.getTime())));
            } else {
                textReminderTime.setText(R.string.reminder_not_set);
            }
        });

        viewModel.getSaveCompleted().observe(getViewLifecycleOwner(), success -> {
            if (Boolean.TRUE.equals(success)) {
                showSnackbar("Отчёт сохранён");
                requireActivity().getSupportFragmentManager().popBackStack();
            }
        });
    }

    private void updateBeforePhotos(List<String> photos) {
        updatePhotoContainer(containerBefore, photos, true);
    }

    private void updateAfterPhotos(List<String> photos) {
        updatePhotoContainer(containerAfter, photos, false);
    }

    private void updatePhotoContainer(FlexboxLayout container, List<String> photos, boolean isBefore) {
        container.removeAllViews();
        if (photos.isEmpty()) {
            ImageView placeholder = new ImageView(requireContext());
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(200, 200);
            params.setMargins(16, 16, 16, 16);
            placeholder.setLayoutParams(params);
            placeholder.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            placeholder.setImageResource(R.drawable.placeholder);
            placeholder.setOnClickListener(v -> showPhotoSourceDialog(isBefore));
            container.addView(placeholder);
            return;
        }

        for (int i = 0; i < photos.size(); i++) {
            String path = photos.get(i);
            ImageView imageView = createThumbnailView(path, isBefore, i);
            container.addView(imageView);
        }
    }

    private ImageView createThumbnailView(String path, boolean isBefore, int index) {
        ImageView imageView = new ImageView(requireContext());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(200, 200);
        params.setMargins(16, 16, 16, 16);
        imageView.setLayoutParams(params);
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);

        Glide.with(this).load(new File(path)).into(imageView);
        imageView.setOnClickListener(v -> {
            ArrayList<String> allPhotos = new ArrayList<>(isBefore
                    ? viewModel.getBeforePhotos().getValue()
                    : viewModel.getAfterPhotos().getValue());
            PhotoOpener.open(this, allPhotos, index);
        });

        imageView.setOnLongClickListener(v -> {
            if (isBefore) viewModel.removeBeforePhoto(index);
            else viewModel.removeAfterPhoto(index);
            return true;
        });
        return imageView;
    }

    private void showTimePickerDialog() {
        Calendar now = Calendar.getInstance();
        TimePickerDialog dialog = new TimePickerDialog(requireContext(),
                (view, hourOfDay, minute) -> {
                    Calendar reminderCal = Calendar.getInstance();
                    reminderCal.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    reminderCal.set(Calendar.MINUTE, minute);
                    reminderCal.set(Calendar.SECOND, 0);
                    reminderCal.set(Calendar.MILLISECOND, 0);

                    if (reminderCal.before(Calendar.getInstance())) {
                        reminderCal.add(Calendar.DAY_OF_MONTH, 1);
                    }

                    long reminderMillis = reminderCal.getTimeInMillis();
                    viewModel.setReminderTime(reminderMillis);
                },
                now.get(Calendar.HOUR_OF_DAY),
                now.get(Calendar.MINUTE),
                true);
        dialog.show();
    }

    private void showLoadingDialog() {
        if (!isAdded()) return;
        if (loadingDialog == null) {
            View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_loading, null);
            loadingDialog = new AlertDialog.Builder(requireContext())
                    .setView(dialogView)
                    .setCancelable(false)
                    .create();
        }
        if (!loadingDialog.isShowing()) loadingDialog.show();
    }

    private void hideLoadingDialog() {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.dismiss();
        }
    }

    private void showSnackbar(String message) {
        if (getView() != null) {
            Snackbar.make(getView(), message, Snackbar.LENGTH_LONG).show();
        }
    }

    @Override
    public void onDestroyView() {
        if (viewModel != null) viewModel.discardTempPhotos();
        super.onDestroyView();
    }
}
