package com.oceanbyte.navimate.view.fragments;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.snackbar.Snackbar;
import com.oceanbyte.navimate.R;
import com.oceanbyte.navimate.models.ContractEntity;
import com.oceanbyte.navimate.utils.DateUtils;
import com.oceanbyte.navimate.utils.UserUtils;
import com.oceanbyte.navimate.viewmodels.ContractViewModel;
import com.oceanbyte.navimate.viewmodels.PositionViewModel;

import java.util.Calendar;
import java.util.Locale;

public class AddContractFragment extends Fragment {

    private ContractViewModel viewModel;
    private PositionViewModel positionViewModel;

    private EditText editVessel;
    private AutoCompleteTextView editPosition;
    private TextView editStart, editEnd;
    private Button btnSave;

    private String userUuid;
    private boolean checkingActiveContract = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add_contract, container, false);

        // ViewModel и UUID
        viewModel = new ViewModelProvider(this).get(ContractViewModel.class);
        positionViewModel = new ViewModelProvider(this).get(PositionViewModel.class);
        userUuid = UserUtils.getOrCreateUserUuid(requireContext());

        // UI
        editVessel = view.findViewById(R.id.editVessel);
        editPosition = view.findViewById(R.id.editPosition);
        editStart = view.findViewById(R.id.editStartDate);
        editEnd = view.findViewById(R.id.editEndDate);
        btnSave = view.findViewById(R.id.btnSaveContract);

        // Автозаполнение должностей
        setupPositionAutoComplete();

        // Выбор дат
        editStart.setOnClickListener(v -> showDatePicker(editStart));
        editEnd.setOnClickListener(v -> showDatePicker(editEnd));

        // Кнопка сохранения
        btnSave.setOnClickListener(v -> {
            if (!checkingActiveContract) {
                checkingActiveContract = true;
                btnSave.setEnabled(false);
                viewModel.loadActiveContract(userUuid);
            }
        });

        // Наблюдение за активным контрактом
        viewModel.getActiveContractLiveData().observe(getViewLifecycleOwner(), activeContract -> {
            if (!checkingActiveContract) return;
            checkingActiveContract = false;
            btnSave.setEnabled(true);

            if (activeContract != null && activeContract.endDate == null) {
                showDatePickerDialogToCloseOldContract(activeContract);
            } else {
                saveNewContract();
            }
        });

// ✅ Наблюдение за ошибками
        viewModel.getErrorMessageLiveData().observe(getViewLifecycleOwner(), errorMessage -> {
            if (errorMessage != null && !errorMessage.isEmpty()) {
                showSnackbar(errorMessage);
            }
        });

        return view;
    }

    /** Автозаполнение должностей через PositionViewModel */
    private void setupPositionAutoComplete() {
        positionViewModel.getAllPositionTitles().observe(getViewLifecycleOwner(), titles -> {
            if (titles != null && getContext() != null) {
                ArrayAdapter<String> adapter = new ArrayAdapter<>(
                        requireContext(),
                        android.R.layout.simple_dropdown_item_1line,
                        titles
                );
                editPosition.setAdapter(adapter);
                editPosition.setThreshold(1); // Показывать подсказки после 1 символа
            }
        });

    }

    /** Диалог выбора даты окончания старого контракта */
    private void showDatePickerDialogToCloseOldContract(ContractEntity activeContract) {
        Calendar calendar = Calendar.getInstance();

        DatePickerDialog dialog = new DatePickerDialog(
                requireContext(),
                (view, year, month, dayOfMonth) -> {
                    long selectedMillis = DateUtils.toMillis(year, month, dayOfMonth);
                    activeContract.endDate = selectedMillis;

                    viewModel.updateContract(activeContract, this::saveNewContract);
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        dialog.setTitle("Укажите дату окончания предыдущего контракта");
        dialog.setCancelable(false);
        dialog.show();
    }

    /** Сохраняет новый контракт после валидации */
    private void saveNewContract() {
        String vessel = editVessel.getText().toString().trim();
        String position = editPosition.getText().toString().trim();

        if (vessel.isEmpty() || position.isEmpty() || editStart.getText().toString().trim().isEmpty()) {
            showSnackbar("Заполните все обязательные поля");
            return;
        }

        Long startDate = parseDateFromTextView(editStart);
        if (startDate == null) {
            showSnackbar("Ошибка даты начала");
            return;
        }

        Long endDate = parseDateFromTextView(editEnd);
        if (editEnd.getText().length() > 0 && endDate == null) {
            showSnackbar("Ошибка даты окончания");
            return;
        }

        ContractEntity contract = new ContractEntity();
        contract.userUuid = userUuid;
        contract.vesselName = vessel;
        contract.position = position;
        contract.startDate = startDate;
        contract.endDate = endDate;

        viewModel.insertContract(contract, () -> {
            showSnackbar("Контракт сохранён");
            requireActivity().getOnBackPressedDispatcher().onBackPressed();
        });
    }

    /** Показывает DatePickerDialog и вставляет дату в TextView */
    private void showDatePicker(TextView target) {
        Calendar calendar = Calendar.getInstance();
        new DatePickerDialog(
                requireContext(),
                (view, year, month, dayOfMonth) -> {
                    long millis = DateUtils.toMillis(year, month, dayOfMonth);
                    target.setText(DateUtils.formatDate(millis));
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        ).show();
    }

    /** Показывает Snackbar с сообщением */
    private void showSnackbar(String message) {
        View root = getView();
        if (root != null) {
            Snackbar.make(root, message, Snackbar.LENGTH_SHORT).show();
        }
    }
    /** Парсит дату из TextView безопасно */
    @Nullable
    private Long parseDateFromTextView(TextView view) {
        String text = view.getText().toString().trim();
        return text.isEmpty() ? null : DateUtils.parseDate(text);
    }

}
