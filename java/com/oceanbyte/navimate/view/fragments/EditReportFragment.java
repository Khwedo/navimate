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
import com.google.android.flexbox.FlexboxLayout;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.google.android.material.snackbar.Snackbar;
import com.oceanbyte.navimate.R;
import com.oceanbyte.navimate.models.JobReport;
import com.oceanbyte.navimate.utils.KeyboardUtils;
import com.oceanbyte.navimate.utils.PhotoOpener;
import com.oceanbyte.navimate.viewmodels.EditReportViewModel;
import androidx.appcompat.widget.Toolbar;


import java.io.File;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class EditReportFragment extends Fragment {



    private EditText editJobTitle, editEquipment, editNote;

    private FlexboxLayout containerBefore, containerAfter;
    private Button btnSave, btnDelete, btnSetReminder, btnClearReminder;
    private AlertDialog loadingDialog;
    private TextView textReminderTime;
    private long reminderTimestamp = 0L;

    private EditReportViewModel viewModel;

    private ActivityResultLauncher<String> galleryLauncher;
    private ActivityResultLauncher<Uri> cameraLauncher;
    private Uri cameraImageUri;

    private boolean isBeforePhoto = true;
    private long reportId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
View view = inflater.inflate(R.layout.fragment_edit_report, container, false);

Toolbar toolbar = view.findViewById(R.id.toolbarEditReport);
        toolbar.setNavigationOnClickListener(v -> requireActivity().onBackPressed());

        KeyboardUtils.setupHideKeyboardOnTouch(requireActivity(), view);



        viewModel = new ViewModelProvider(this).get(EditReportViewModel.class);

        editJobTitle = view.findViewById(R.id.editJobTitle);
        editEquipment = view.findViewById(R.id.editEquipment);
        containerBefore = view.findViewById(R.id.containerBeforePhotos);
        containerAfter = view.findViewById(R.id.containerAfterPhotos);
        editNote = view.findViewById(R.id.editNote);
        btnSetReminder = view.findViewById(R.id.btnSetReminder);
        textReminderTime = view.findViewById(R.id.textReminderTime);
        btnClearReminder = view.findViewById(R.id.btnClearReminder);

        btnSave = view.findViewById(R.id.btnSaveReport);
        btnDelete = view.findViewById(R.id.btnDeleteReport);


        btnSetReminder.setOnClickListener(v -> showTimePickerDialog());

        btnClearReminder.setOnClickListener(v -> {
            viewModel.setReminderTime(0L);
            textReminderTime.setText("Не установлено");
            showSnackbar("Напоминание удалено");
        });

        // Запуск из галереи
        galleryLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                viewModel.compressAndAddPhoto(requireContext(), uri, isBeforePhoto);
            }
        });

        // Запуск с камеры
        cameraLauncher = registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
            if (success && cameraImageUri != null) {
                viewModel.compressAndAddPhoto(requireContext(), cameraImageUri, isBeforePhoto);
            }
        });

        view.findViewById(R.id.btnAddBeforePhoto).setOnClickListener(v -> showPhotoSourceDialog(true));
        view.findViewById(R.id.btnAddAfterPhoto).setOnClickListener(v -> showPhotoSourceDialog(false));

        btnSave.setOnClickListener(v -> {
            String jobTitle = editJobTitle.getText().toString().trim();
            String equipment = editEquipment.getText().toString().trim();
            String userNote = editNote.getText().toString().trim();

            if (jobTitle.isEmpty() || equipment.isEmpty()) {
                showSnackbar("Заполните все поля");
            } else {
                viewModel.setJobTitle(jobTitle);
                viewModel.setEquipmentName(equipment);
                viewModel.setUserNote(userNote);
                JobReport currentReport = viewModel.getReportLiveData().getValue();
                if (currentReport != null) {
                    viewModel.updateReport(reportId, currentReport.contractId);
                } else {
                    showSnackbar("Ошибка: отчёт не загружен");
                }
            }
        });

        btnDelete.setOnClickListener(v -> confirmDelete());

        observeViewModel();

        if (getArguments() != null) {
            reportId = getArguments().getLong("reportId", -1);
            if (reportId != -1) {
                viewModel.loadReportById(reportId);
            }
        }


        return view;
    }

    private void showPhotoSourceDialog(boolean isBefore) {
        this.isBeforePhoto = isBefore;

        int photoCount = isBefore
                ? viewModel.getBeforePhotos().getValue().size()
                : viewModel.getAfterPhotos().getValue().size();

        if (photoCount >= 6) {
            showSnackbar("Максимум 6 фото " + (isBefore ? "'до'" : "'после'"));
            return;
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("Добавить фото")
                .setItems(new String[]{"Сделать фото", "Выбрать из галереи"}, (dialog, which) -> {
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
        viewModel.getReportLiveData().observe(getViewLifecycleOwner(), report -> {
            if (report != null) {
                editJobTitle.setText(report.jobTitle);
                editEquipment.setText(report.equipmentName);
                editNote.setText(report.userNote);

            }
        });

        viewModel.getBeforePhotos().observe(getViewLifecycleOwner(), this::updateBeforePhotos);
        viewModel.getAfterPhotos().observe(getViewLifecycleOwner(), this::updateAfterPhotos);

        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), this::showSnackbar);

        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            if (!isAdded() || getActivity() == null) return;
            if (Boolean.TRUE.equals(isLoading)) {
                showLoadingDialog();
            } else {
                hideLoadingDialog();
            }
        });

        viewModel.getReminderTime().observe(getViewLifecycleOwner(), timeMillis -> {
            if (timeMillis != null && timeMillis > 0) {
                Calendar cal = Calendar.getInstance();
                cal.setTimeInMillis(timeMillis);
                DateFormat timeFormat = android.text.format.DateFormat.getTimeFormat(requireContext());
                textReminderTime.setText("Напомнить в " + timeFormat.format(cal.getTime()));
            } else {
                textReminderTime.setText("Не установлено");
            }
        });

        viewModel.getUpdateSuccess().observe(getViewLifecycleOwner(), success -> {
            if (Boolean.TRUE.equals(success)) {
                showSnackbar("Отчёт обновлён");
                viewModel.clearUpdateSuccess(); // сброс после использования
                requireActivity().getSupportFragmentManager().popBackStack();
            }
        });

        viewModel.getDeleteSuccess().observe(getViewLifecycleOwner(), deleted -> {
            if (Boolean.TRUE.equals(deleted)) {
                showSnackbar("Отчёт удалён");
                viewModel.clearDeleteSuccess(); // сброс после использования
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

            placeholder.setOnClickListener(v -> showPhotoSourceDialog(isBefore));  // 👈 запуск диалога
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
            ArrayList<String> allPhotos = new ArrayList<>(isBefore ? viewModel.getBeforePhotos().getValue() : viewModel.getAfterPhotos().getValue());
            PhotoOpener.open(this, allPhotos, index);
        });

        imageView.setOnLongClickListener(v -> {
            if (isBefore) {
                viewModel.removeBeforePhoto(index);
            } else {
                viewModel.removeAfterPhoto(index);
            }
            return true;
        });
        return imageView;
    }

    private void confirmDelete() {
        JobReport report = viewModel.getReportLiveData().getValue();
        if (report == null) {
            showSnackbar("Отчёт не загружен");
            return;
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("Удалить отчёт?")
                .setMessage("Это действие нельзя отменить.")
                .setPositiveButton("Удалить", (dialog, which) -> viewModel.deleteReport(report))
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void showLoadingDialog() {
        if (!isAdded() || getActivity() == null || getView() == null) return;
        if (loadingDialog == null) {
            View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_loading, null);
            loadingDialog = new AlertDialog.Builder(requireContext())
                    .setView(dialogView)
                    .setCancelable(false)
                    .create();
        }
        if (!loadingDialog.isShowing()) {
            loadingDialog.show();
        }
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
    private void showTimePickerDialog() {
        Calendar now = Calendar.getInstance();

        TimePickerDialog dialog = new TimePickerDialog(requireContext(),
                (view, hourOfDay, minute) -> {
                    Calendar reminderCal = Calendar.getInstance();
                    reminderCal.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    reminderCal.set(Calendar.MINUTE, minute);
                    reminderCal.set(Calendar.SECOND, 0);
                    reminderCal.set(Calendar.MILLISECOND, 0);

                    // Если выбранное время уже прошло — перенести на следующий день
                    if (reminderCal.before(Calendar.getInstance())) {
                        reminderCal.add(Calendar.DAY_OF_MONTH, 1);
                    }

                    long reminderMillis = reminderCal.getTimeInMillis();
                    viewModel.setReminderTime(reminderMillis);

                    DateFormat timeFormat = android.text.format.DateFormat.getTimeFormat(requireContext());
                    textReminderTime.setText("Напомнить в " + timeFormat.format(reminderCal.getTime()));
                },
                now.get(Calendar.HOUR_OF_DAY),
                now.get(Calendar.MINUTE),
                true);

        dialog.show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (viewModel != null) {
            viewModel.discardTempPhotos(); // Очистка временных фото, если пользователь не сохранил
        }
    }



}
