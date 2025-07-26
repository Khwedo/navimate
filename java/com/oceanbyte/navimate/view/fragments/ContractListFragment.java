package com.oceanbyte.navimate.view.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.oceanbyte.navimate.R;
import com.oceanbyte.navimate.models.ContractEntity;
import com.oceanbyte.navimate.utils.UserUtils;
import com.oceanbyte.navimate.view.fragments.adapters.ContractAdapter;
import com.oceanbyte.navimate.viewmodels.ContractViewModel;

import java.util.List;

public class ContractListFragment extends Fragment {

    private ContractViewModel viewModel;
    private ContractAdapter adapter;
    private RecyclerView recyclerView;
    private TextView textEmpty;
    private Button btnAddContract;
    private String userUuid;
    private SwipeRefreshLayout swipeRefreshLayout;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_contract_list, container, false);

        initViews(view);
        setupRecyclerView();
        initViewModel();
        observeViewModelEvents();
        swipeRefreshLayout.setOnRefreshListener(() -> {
            if (userUuid != null) {
                viewModel.refreshContracts();  // 💡 метод уже есть
                observeViewModelEvents(); // 💡 пересоздаёт подписку
            }
            swipeRefreshLayout.setRefreshing(false); // ⏹ остановить индикатор
        });
        btnAddContract.setOnClickListener(v -> openAddContractFragment());

        return view;
    }

    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.recyclerContracts);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefresh);
        textEmpty = view.findViewById(R.id.textEmptyState);
        btnAddContract = view.findViewById(R.id.btnAddNewContract);

        // Дополнительно — если список пуст, TextView работает как кнопка
        textEmpty.setOnClickListener(v -> openAddContractFragment());
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new ContractAdapter(this::openEditContractFragment);
        recyclerView.setAdapter(adapter);
    }

    private void initViewModel() {
        viewModel = new ViewModelProvider(this).get(ContractViewModel.class);
        userUuid = UserUtils.getOrCreateUserUuid(requireContext());

        if (userUuid == null || userUuid.isEmpty()) {
            textEmpty.setText(getString(R.string.error_no_uuid));
            textEmpty.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        }
    }

    private void observeViewModelEvents() {
        if (userUuid != null) {
            viewModel.getContractsLive(userUuid).observe(getViewLifecycleOwner(), this::updateUI);
        }
    }

    private void updateUI(List<ContractEntity> contracts) {
        boolean isEmpty = contracts == null || contracts.isEmpty();
        recyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        textEmpty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        adapter.submitList(contracts);
    }

    private void openAddContractFragment() {
        FragmentTransaction transaction = requireActivity().getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragmentContainer, new AddContractFragment());
        transaction.addToBackStack(null);
        transaction.commit();
    }

    private void openEditContractFragment(int contractId) {
        EditContractFragment fragment = EditContractFragment.newInstance(contractId);
        FragmentTransaction transaction = requireActivity().getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragmentContainer, fragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }
}
