package com.oceanbyte.navimate.view.fragments;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.*;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.view.ActionMode;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.oceanbyte.navimate.R;
import com.oceanbyte.navimate.models.ContractEntity;
import com.oceanbyte.navimate.models.JobReport;
import com.oceanbyte.navimate.models.ReportListItem;
import com.oceanbyte.navimate.models.UserProfile;
import com.oceanbyte.navimate.utils.ExportUtils;
import com.oceanbyte.navimate.utils.KeyboardUtils;
import com.oceanbyte.navimate.utils.ReminderUtils;
import com.oceanbyte.navimate.utils.ReportUtils;
import com.oceanbyte.navimate.view.fragments.adapters.ReportAdapter;
import com.oceanbyte.navimate.viewmodels.ReportViewModel;
import com.oceanbyte.navimate.viewmodels.SettingsViewModel;

import java.util.*;

public class ViewReportsFragment extends Fragment implements ReportAdapter.OnReportClickListener {

    private EditText searchField;
    private RecyclerView recyclerView;
    private ReportAdapter adapter;
    private FloatingActionButton fabAddReport;

    private Button btnWeekFilter;
    private TextView textEmpty;

    private ReportViewModel reportViewModel;
    private SettingsViewModel settingsViewModel;
    private SwipeRefreshLayout swipeRefreshLayout;

    private final List<JobReport> allReports = new ArrayList<>();
    private final List<ReportListItem> groupedItems = new ArrayList<>();
    private final Set<String> selectedWeeks = new HashSet<>();
    private final Set<String> allAvailableWeeks = new HashSet<>();

    private ContractEntity activeContract;
    private UserProfile currentProfile;
    private ActionMode actionMode;
    private boolean isInitialLoadDone = false;

    private static final String KEY_SELECTED_IDS = "selected_ids";
    private static final String KEY_SEARCH_QUERY = "search_query";
    /**
     * Поддержка ленивой загрузки
     */
    private boolean isLoadingMore = false;
    private boolean isLastPage = false;
    private int currentPage = 0;
    private int currentOffset = 0;
    private static final int PAGE_SIZE = 15;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_view_reports, container, false);
        KeyboardUtils.setupHideKeyboardOnTouch(requireActivity(), view);

        initViews(view);
        setupRecyclerView();
        setupViewModels();
        setupListeners();

        swipeRefreshLayout = view.findViewById(R.id.swipeRefresh);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            refreshReports();
        });

        // Restore state after rotation
        if (savedInstanceState != null) {
            restoreInstanceState(savedInstanceState);
        }

        return view;
    }

    private void initViews(View view) {
        searchField = view.findViewById(R.id.searchInput);
        recyclerView = view.findViewById(R.id.reportRecyclerView);
        fabAddReport = view.findViewById(R.id.fabAddReport);
        btnWeekFilter = view.findViewById(R.id.btnWeekFilter);
        textEmpty = view.findViewById(R.id.textEmptyReports);
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new ReportAdapter(groupedItems, this); // Устанавливается вертикальный список.

        recyclerView.setAdapter(adapter); // Передаем список отчетов и этот фрагмент, как обработчик кликов.
        recyclerView.setHasFixedSize(true);

        adapter.setOnAddReportClickListener(weekTitle -> openCreateReport());
        adapter.setOnReportLongClickListener(() -> {
            if (actionMode == null) {
                actionMode = ((AppCompatActivity) requireActivity()).startSupportActionMode(actionModeCallback);
            }
            updateActionModeTitle();
        });
        adapter.setOnSelectionChangedListener(count -> {
            if (count == 0) {
                if (actionMode != null) {
                    actionMode.finish();
                    actionMode = null;
                }
            } else {
                if (actionMode == null) {
                    actionMode = ((AppCompatActivity) requireActivity()).startSupportActionMode(actionModeCallback);
                }
                actionMode.setTitle(count + " " + getString(R.string.selected));
            }
        });
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {  // Подключаем адаптер к списку.
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (layoutManager == null) return;

                int visibleItemCount = layoutManager.getChildCount();
                int totalItemCount = layoutManager.getItemCount();
                int firstVisibleItem = layoutManager.findFirstVisibleItemPosition();

                if (!isLoadingMore && !isLastPage && (visibleItemCount + firstVisibleItem) >= totalItemCount - 4) {
                    loadMoreReports(); // 👈 теперь всегда загружаем из БД при скролле
                }
            }
        });

    }

    private void setupViewModels() {  // Это наши ViewModel — посредники между экраном и логикой (базой данных).
        settingsViewModel = new ViewModelProvider(this).get(SettingsViewModel.class);  // подписка на: активный контракт;
        reportViewModel = new ViewModelProvider(this).get(ReportViewModel.class);  // подписка на: профиль пользователя.

        settingsViewModel.getUserProfile().observe(getViewLifecycleOwner(), profile -> currentProfile = profile);

        settingsViewModel.getActiveContract().observe(getViewLifecycleOwner(), contract -> {
            // ✅ Загружаем только при изменении контракта
            if (contract != null && (activeContract == null || activeContract.id != contract.id)) {
                activeContract = contract;

                currentOffset = 0;
                currentPage = 0;
                isLastPage = false;
                isLoadingMore = false;
                isInitialLoadDone = false;
                allReports.clear();
                groupedItems.clear();
                adapter.setItems(new ArrayList<>());
                loadMoreReports();
            } else if (contract == null) {
                textEmpty.setVisibility(View.VISIBLE);
            }
        });
    }


    private void setupListeners() {
        searchField.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterReports(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        fabAddReport.setOnClickListener(v -> openCreateReport());


        btnWeekFilter.setOnClickListener(v -> showWeekFilterDialog());

    }

    private void updateGroupedItems(List<JobReport> reports) {
        groupedItems.clear();
        List<ReportListItem> grouped = ReportUtils.groupReportsByWeek(reports, Locale.getDefault());

        allAvailableWeeks.clear();
        for (ReportListItem item : grouped) {
            if (item.isHeader) allAvailableWeeks.add(item.header);
        }

        if (selectedWeeks.isEmpty()) {
            groupedItems.addAll(grouped);
        } else {
            for (ReportListItem item : grouped) {
                if (item.isHeader && selectedWeeks.contains(item.header)) {
                    groupedItems.add(item);
                } else if (!item.isHeader && !groupedItems.isEmpty()) {
                    groupedItems.add(item);
                }
            }
        }

        adapter.setItems(groupedItems);
    }

    private void scrollToCurrentWeek() {
        int index = adapter.getFirstCurrentWeekIndex();
        if (index >= 0) recyclerView.scrollToPosition(index);
    }

    private void filterReports(String query) {
        List<JobReport> filteredSource = new ArrayList<>();
        if (selectedWeeks.isEmpty()) {
            filteredSource.addAll(allReports);
        } else {
            for (JobReport report : allReports) {
                String week = ReportUtils.getWeekTitle(report.reportDate);
                if (selectedWeeks.contains(week)) {
                    filteredSource.add(report);
                }
            }
        }

        if (query.isEmpty()) {
            updateGroupedItems(filteredSource);
        } else {
            List<JobReport> result = new ArrayList<>();
            for (JobReport report : filteredSource) {
                if ((report.jobTitle != null && report.jobTitle.toLowerCase().contains(query.toLowerCase()))
                        || (report.equipmentName != null && report.equipmentName.toLowerCase().contains(query.toLowerCase()))) {
                    result.add(report);
                }
            }
            updateGroupedItems(result);
        }
    }

    private void openCreateReport() {
        requireActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainer, new CreateReportFragment())
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onReportClick(JobReport report) {
        if (actionMode != null) {
            adapter.toggleSelection(report.id);
            updateActionModeTitle();
            if (!adapter.hasSelection()) actionMode.finish();
        } else {
            if (report == null || !isAdded()) return;

            EditReportFragment fragment = new EditReportFragment();
            Bundle args = new Bundle();
            args.putLong("reportId", report.id);
            fragment.setArguments(args);

            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainer, fragment)
                    .addToBackStack(null)
                    .commit();
        }
    }
    private void updateActionModeTitle() {
        if (actionMode != null) {
            int count = adapter.getSelectedReports().size();
            actionMode.setTitle(count + " " + getString(R.string.selected));
        }
    }

    private void showExportDialog(List<JobReport> reports) {
        String[] options = {
                getString(R.string.export_to_pdf),
                getString(R.string.export_to_word),
                getString(R.string.export_to_both_formats)
        };

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.choose_export_format)
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0: ExportUtils.exportToPdf(requireContext(), reports, currentProfile); break;
                        case 1: ExportUtils.exportToDocx(requireContext(), reports, currentProfile, activeContract); break;
                        case 2:
                            ExportUtils.exportToPdf(requireContext(), reports, currentProfile);
                            ExportUtils.exportToDocx(requireContext(), reports, currentProfile, activeContract);
                            break;
                    }
                    showSnackbar(getString(R.string.finish_export));
                })
                .show();
    }

    private void showWeekFilterDialog() {
        String[] allWeeksArray = allAvailableWeeks.toArray(new String[0]);
        boolean[] checkedItems = new boolean[allWeeksArray.length];
        for (int i = 0; i < allWeeksArray.length; i++) {
            checkedItems[i] = selectedWeeks.contains(allWeeksArray[i]);
        }

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.choose_weeks)
                .setMultiChoiceItems(allWeeksArray, checkedItems, (dialog, which, isChecked) -> {
                    if (isChecked) selectedWeeks.add(allWeeksArray[which]);
                    else selectedWeeks.remove(allWeeksArray[which]);
                })
                .setPositiveButton(R.string.apply, (dialog, which) -> filterReports(searchField.getText().toString()))
                .setNegativeButton(R.string.reset, (dialog, which) -> {
                    selectedWeeks.clear();
                    filterReports(searchField.getText().toString());
                })
                .show();
    }

    private final ActionMode.Callback actionModeCallback = new ActionMode.Callback() {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mode.getMenuInflater().inflate(R.menu.menu_reports_actionmode, menu);
            return true;
        }

        @Override public boolean onPrepareActionMode(ActionMode mode, Menu menu) { return false; }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            int id = item.getItemId();

            if (id == R.id.menu_delete) {
                List<JobReport> toDelete = adapter.getSelectedReports();

                if (toDelete.isEmpty()) {
                    showSnackbar(getString(R.string.no_reports_selected));
                    return true;
                }

                new AlertDialog.Builder(requireContext())
                        .setTitle("Удалить отчёт?")
                        .setMessage("Вы уверены, что хотите удалить этот отчёт?")
                        .setPositiveButton("Удалить", (dialog, which) -> {
                            for (JobReport report : toDelete) {
                                reportViewModel.deleteReport(report);  // теперь у нас есть доступ к reminderTime и report.id
                            }
                            if (actionMode != null) actionMode.finish();
                        })
                        .setNegativeButton("Отмена", null)
                        .show();

                return true;

            } else if (id == R.id.menu_export) {
                List<JobReport> selectedReports = adapter.getSelectedReports();
                if (selectedReports.isEmpty()) {
                    showSnackbar(getString(R.string.export_empty));
                } else if (currentProfile == null) {
                    showSnackbar(getString(R.string.profile_settings_empty));
                } else {
                    showExportDialog(selectedReports);
                }
                return true;
            }

            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            adapter.clearSelections();
            actionMode = null;
        }
    };

    private void restoreInstanceState(Bundle savedInstanceState) {
        if (adapter != null) {
            long[] selectedArray = savedInstanceState.getLongArray(KEY_SELECTED_IDS);
            if (selectedArray != null && selectedArray.length > 0) {
                Set<Long> ids = new HashSet<>();
                for (long id : selectedArray) ids.add(id);
                adapter.restoreSelections(ids);
                actionMode = ((AppCompatActivity) requireActivity()).startSupportActionMode(actionModeCallback);
                updateActionModeTitle();
            }
        }

        String query = savedInstanceState.getString(KEY_SEARCH_QUERY, "");
        if (!query.isEmpty()) {
            searchField.setText(query);
            filterReports(query);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (adapter != null) {
            Set<Long> selected = adapter.getSelectedIds();
            long[] selectedArray = new long[selected.size()];
            int i = 0;
            for (Long id : selected) selectedArray[i++] = id;
            outState.putLongArray(KEY_SELECTED_IDS, selectedArray);
        }

        if (searchField != null) {
            outState.putString(KEY_SEARCH_QUERY, searchField.getText().toString());
        }
    }

    private void showSnackbar(String message) {
        if (getView() != null) {
            Snackbar.make(getView(), message, Snackbar.LENGTH_LONG).show();
        }
    }
    @Override
    public void onDestroyView() {
        if (actionMode != null) {
            actionMode.finish();
            actionMode = null;
        }
        super.onDestroyView();
    }

    private void refreshReports() {
        swipeRefreshLayout.setRefreshing(true);

        currentOffset = 0;
        currentPage = 0;
        isLastPage = false;
        isLoadingMore = false;
        isInitialLoadDone = false;
        allReports.clear();
        groupedItems.clear();
        adapter.setItems(new ArrayList<>());

        if (activeContract != null) {
            loadMoreReports(); // теперь всё в одном
        }

        swipeRefreshLayout.setRefreshing(false); // оставить здесь безопасно
    }

    private void loadMoreReports() { // Загружает отчеты с offset'ом и количеством штук.
        if (activeContract == null || isLoadingMore || isLastPage) return;

        isLoadingMore = true;

        reportViewModel.getReportsPaged(activeContract.id, currentOffset, PAGE_SIZE, result -> {
            requireActivity().runOnUiThread(() -> {
                if (result == null || result.isEmpty()) {
                    isLastPage = true;
                } else {
                    allReports.addAll(result);
                    currentOffset += result.size();

                    List<ReportListItem> groupedChunk = ReportUtils.groupReportsByWeek(result, Locale.getDefault());

                    Set<String> existingHeaders = new HashSet<>();
                    for (ReportListItem item : groupedItems) {
                        if (item.isHeader) existingHeaders.add(item.header);
                    }

                    List<ReportListItem> filteredChunk = new ArrayList<>();
                    for (ReportListItem item : groupedChunk) {
                        if (item.isHeader) {
                            if (!existingHeaders.contains(item.header)) {
                                filteredChunk.add(item);
                                existingHeaders.add(item.header);
                            }
                        } else {
                            filteredChunk.add(item);
                        }
                    }

                    groupedItems.addAll(filteredChunk);
                    adapter.appendItems(filteredChunk);
                    currentPage++;
                }

                // ✅ Показать сообщение, если отчётов нет вообще
                textEmpty.setVisibility(allReports.isEmpty() ? View.VISIBLE : View.GONE);

                isInitialLoadDone = true;
                isLoadingMore = false;
            });
        });
    }



    private void renderFirstPage(List<JobReport> firstBatch) {
        groupedItems.clear();

        List<ReportListItem> grouped = ReportUtils.groupReportsByWeek(firstBatch, Locale.getDefault());
        groupedItems.addAll(grouped);
        adapter.setItems(new ArrayList<>(groupedItems));

        currentPage = 1; // уже первая страница отображена
        isInitialLoadDone = true;
        isLoadingMore = false;

        scrollToCurrentWeek();
    }




}
