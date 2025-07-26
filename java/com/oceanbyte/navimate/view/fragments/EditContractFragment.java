package com.oceanbyte.navimate.view.fragments;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.snackbar.Snackbar;
import com.oceanbyte.navimate.R;
import com.oceanbyte.navimate.models.ContractEntity;
import com.oceanbyte.navimate.utils.DateUtils;
import com.oceanbyte.navimate.viewmodels.ContractViewModel;
import com.oceanbyte.navimate.viewmodels.PositionViewModel;

import java.util.Calendar;

public class EditContractFragment extends Fragment {

    private static final String ARG_CONTRACT_ID = "contract_id";

    private EditText editVessel;
    private AutoCompleteTextView editPosition;
    private TextView editStartDate, editEndDate;
    private Button btnSave, btnDelete;

    private ContractViewModel contractViewModel;
    private PositionViewModel positionViewModel;

    private ContractEntity contract;
    private int contractId = -1;

    private Long startDateMillis = null;
    private Long endDateMillis = null;

    public static EditContractFragment newInstance(int contractId) {
        EditContractFragment fragment = new EditContractFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_CONTRACT_ID, contractId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_edit_contract, container, false);

        // Инициализация UI
        editVessel = view.findViewById(R.id.editVessel);
        editPosition = view.findViewById(R.id.editPosition);
        editStartDate = view.findViewById(R.id.editStartDate);
        editEndDate = view.findViewById(R.id.editEndDate);
        btnSave = view.findViewById(R.id.btnSaveContract);
        btnDelete = view.findViewById(R.id.btnDeleteContract);

        // Инициализация ViewModel'ов
        contractViewModel = new ViewModelProvider(this).get(ContractViewModel.class);
        positionViewModel = new ViewModelProvider(this).get(PositionViewModel.class);

        if (getArguments() != null && getArguments().containsKey(ARG_CONTRACT_ID)) {
            contractId = getArguments().getInt(ARG_CONTRACT_ID);
            contractViewModel.getContractById(contractId, loaded -> {
                contract = loaded;
                bindContract();
            });
        }

        setupDatePickers();
        setupPositionAutoComplete();

// Наблюдение за ошибками
        contractViewModel.getErrorMessageLiveData().observe(getViewLifecycleOwner(), errorMessage -> {
            if (errorMessage != null && !errorMessage.isEmpty()) {
                showSnackbar(errorMessage);
            }
        });

        btnSave.setOnClickListener(v -> saveChanges());
        btnDelete.setOnClickListener(v -> confirmDeletion());

        return view;
    }

    private void bindContract() {
        if (contract == null) return;

        editVessel.setText(contract.vesselName);
        editPosition.setText(contract.position);

        if (contract.startDate != null) {
            startDateMillis = contract.startDate;
            editStartDate.setText(DateUtils.formatDate(contract.startDate));
        }

        if (contract.endDate != null) {
            endDateMillis = contract.endDate;
            editEndDate.setText(DateUtils.formatDate(contract.endDate));
        }
    }

    private void setupDatePickers() {
        editStartDate.setOnClickListener(v -> showDatePicker((formatted, millis) -> {
            startDateMillis = millis;
            editStartDate.setText(formatted);
        }));

        editEndDate.setOnClickListener(v -> showDatePicker((formatted, millis) -> {
            endDateMillis = millis;
            editEndDate.setText(formatted);
        }));
    }

    private void setupPositionAutoComplete() {
        positionViewModel.getAllPositionTitles().observe(getViewLifecycleOwner(), titles -> {
            if (titles != null) {
                editPosition.setAdapter(new ArrayAdapter<>(
                        requireContext(),
                        android.R.layout.simple_dropdown_item_1line,
                        titles
                ));
            }
        });
    }

    private void showDatePicker(OnDateSelectedListener listener) {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog dialog = new DatePickerDialog(
                requireContext(),
                (view, year, month, dayOfMonth) -> {
                    Calendar selected = Calendar.getInstance();
                    selected.set(year, month, dayOfMonth);
                    listener.onDateSelected(DateUtils.formatDate(selected.getTimeInMillis()), selected.getTimeInMillis());
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        dialog.show();
    }

    private void saveChanges() {
        if (contract == null) {
            Snackbar.make(requireView(), "Контракт ещё загружается. Подождите...", Snackbar.LENGTH_SHORT).show();
            return;
        }

        String vesselName = editVessel.getText().toString().trim();
        String position = editPosition.getText().toString().trim();

        if (vesselName.isEmpty() || position.isEmpty() || startDateMillis == null) {
            showSnackbar("Пожалуйста, заполните обязательные поля");
            return;
        }

        // Проверка на пересечение по дате с другим контрактом
        contractViewModel.getActiveContract(contract.userUuid, activeContract -> {
            if (activeContract != null && activeContract.id != contract.id && activeContract.endDate != null) {
                if (startDateMillis < activeContract.endDate) {
                    showSnackbar("Дата начала контракта не может быть раньше окончания другого активного контракта.");
                    return;
                }
            }

            // Обновление полей и сохранение
            contract.vesselName = vesselName;
            contract.position = position;
            contract.startDate = startDateMillis;
            contract.endDate = endDateMillis;

            contractViewModel.updateContract(contract, () -> {
                showSnackbar("Контракт обновлён");
                requireActivity().getOnBackPressedDispatcher().onBackPressed();
            });
        });
    }

    // Новый метод с диалогом подтверждения
    private void confirmDeletion() {
        if (contract == null) return;

        new AlertDialog.Builder(requireContext())
                .setTitle("Удаление контракта")
                .setMessage("Вы уверены, что хотите удалить этот контракт?")
                .setPositiveButton("Да", (dialog, which) -> deleteContract())
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void deleteContract() {
        contractViewModel.deleteContract(contract, () -> {
            showSnackbar("Контракт удалён");
            requireActivity().getOnBackPressedDispatcher().onBackPressed();
        });
    }

    private void showSnackbar(String message) {
        View view = getView();
        if (view != null) {
            Snackbar.make(view, message, Snackbar.LENGTH_SHORT).show();
        }
    }

    private interface OnDateSelectedListener {
        void onDateSelected(String formatted, long millis);
    }
}
