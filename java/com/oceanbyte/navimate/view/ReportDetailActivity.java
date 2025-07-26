package com.oceanbyte.navimate.view;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.*;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.snackbar.Snackbar;
import com.oceanbyte.navimate.R;
import com.oceanbyte.navimate.models.JobReport;
import com.oceanbyte.navimate.utils.KeyboardUtils;
import com.oceanbyte.navimate.utils.ReportExporter;
import com.oceanbyte.navimate.viewmodels.ReportDetailViewModel;

import java.io.File;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ReportDetailActivity extends AppCompatActivity {

    private TextView textDate, textEquipment, textJobTitle;
    private LinearLayout beforePhotosContainer, afterPhotosContainer;
    private View btnExportPdf, btnExportWord, btnDelete;
    private View rootLayout;

    private ReportDetailViewModel viewModel;
    private JobReport report;
    private DateFormat dateFormat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_detail);
        KeyboardUtils.setupHideKeyboardOnTouch(this, findViewById(android.R.id.content));

        dateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault());
        viewModel = new ViewModelProvider(this).get(ReportDetailViewModel.class);

        initViews();
        observeViewModel();

        long reportId = getIntent().getLongExtra("reportId", -1);
        if (reportId != -1) viewModel.loadReport(reportId);
    }

    private void initViews() {
        rootLayout = findViewById(R.id.rootLayout);
        textDate = findViewById(R.id.textDate);
        textEquipment = findViewById(R.id.textEquipment);
        textJobTitle = findViewById(R.id.textJobTitle);
        beforePhotosContainer = findViewById(R.id.beforePhotosContainer);
        afterPhotosContainer = findViewById(R.id.afterPhotosContainer);
        btnExportPdf = findViewById(R.id.btnExportPdf);
        btnExportWord = findViewById(R.id.btnExportWord);
        btnDelete = findViewById(R.id.btnDelete);

        btnExportPdf.setOnClickListener(v -> {
            if (report != null) exportPdf();
        });

        btnExportWord.setOnClickListener(v -> {
            if (report != null) exportWord();
        });

        btnDelete.setOnClickListener(v -> confirmDelete());
    }

    private void observeViewModel() {
        viewModel.getReportLiveData().observe(this, this::displayReport);

        viewModel.getErrorMessage().observe(this, this::showSnackbar);

        viewModel.getIsLoading().observe(this, isLoading -> {
            btnDelete.setEnabled(!isLoading);
            btnExportPdf.setEnabled(!isLoading);
            btnExportWord.setEnabled(!isLoading);
        });
    }

    private void displayReport(JobReport report) {
        if (report == null) {
            showSnackbar(getString(R.string.report_not_found));
            finish();
            return;
        }

        this.report = report;
        textDate.setText(getString(R.string.report_date, dateFormat.format(report.reportDate)));
        textEquipment.setText(getString(R.string.report_equipment, report.equipmentName));
        textJobTitle.setText(getString(R.string.report_job_title, report.jobTitle));

        loadPhotos(report.beforePhotos, beforePhotosContainer, getString(R.string.label_before_photos));
        loadPhotos(report.afterPhotos, afterPhotosContainer, getString(R.string.label_after_photos));
    }

    private void loadPhotos(List<String> photoPaths, LinearLayout container, String label) {
        container.removeAllViews();

        if (photoPaths == null || photoPaths.isEmpty()) {
            TextView placeholder = new TextView(this);
            placeholder.setText(getString(R.string.no_photos, label));
            placeholder.setPadding(24, 24, 24, 24);
            container.addView(placeholder);
            return;
        }

        for (int i = 0; i < photoPaths.size(); i++) {
            String path = photoPaths.get(i);
            int index = i;

            File photoFile = new File(path);
            ImageView imageView = new ImageView(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(200, 200);
            params.setMargins(16, 16, 16, 16);
            imageView.setLayoutParams(params);
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);

            if (photoFile.exists()) {
                imageView.setImageURI(Uri.fromFile(photoFile));
                int finalIndex = index;
                imageView.setOnClickListener(v -> {
                    Intent intent = new Intent(this, PhotoViewerActivity.class);
                    intent.putStringArrayListExtra("photoPaths", new ArrayList<>(photoPaths));
                    intent.putExtra("startPosition", finalIndex);
                    startActivity(intent);
                });
            } else {
                imageView.setImageResource(R.drawable.placeholder);
            }

            container.addView(imageView);
        }
    }

    private void exportPdf() {
        try {
            File pdfFile = ReportExporter.exportToPdf(this, report);
            showSnackbar(getString(R.string.pdf_exported, pdfFile.getName()));
        } catch (Exception e) {
            e.printStackTrace();
            showSnackbar(getString(R.string.error_export_pdf));
        }
    }

    private void exportWord() {
        try {
            File wordFile = ReportExporter.exportToWord(this, report);
            showSnackbar(getString(R.string.word_exported, wordFile.getName()));
        } catch (Exception e) {
            e.printStackTrace();
            showSnackbar(getString(R.string.error_export_word));
        }
    }

    private void confirmDelete() {
        if (report != null) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.confirm_delete_title)
                    .setMessage(R.string.confirm_delete_message)
                    .setPositiveButton(R.string.delete, (dialog, which) -> {
                        viewModel.deleteReport(report, () -> runOnUiThread(() -> {
                            showSnackbar(getString(R.string.report_deleted));
                            setResult(RESULT_OK);
                            finish();
                        }));
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .show();
        }
    }

    private void showSnackbar(String message) {
        if (rootLayout != null) {
            Snackbar.make(rootLayout, message, Snackbar.LENGTH_LONG).show();
        }
    }
}
