package com.oceanbyte.navimate.view.fragments.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.oceanbyte.navimate.R;
import com.oceanbyte.navimate.models.ContractEntity;

import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

/**
 * Адаптер для отображения списка контрактов в RecyclerView.
 */
public class ContractAdapter extends ListAdapter<ContractEntity, ContractAdapter.ContractViewHolder> {

    public interface OnContractClickListener {
        void onContractClicked(int contractId);
    }

    private final OnContractClickListener clickListener;

    public ContractAdapter(OnContractClickListener clickListener) {
        super(new DiffUtil.ItemCallback<ContractEntity>() {
            @Override
            public boolean areItemsTheSame(@NonNull ContractEntity oldItem, @NonNull ContractEntity newItem) {
                return oldItem.id == newItem.id;
            }

            @Override
            public boolean areContentsTheSame(@NonNull ContractEntity oldItem, @NonNull ContractEntity newItem) {
                return oldItem.vesselName.equals(newItem.vesselName)
                        && oldItem.position.equals(newItem.position)
                        && Objects.equals(oldItem.startDate, newItem.startDate)
                        && Objects.equals(oldItem.endDate, newItem.endDate);
            }
        });
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public ContractViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_contract, parent, false);
        return new ContractViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ContractViewHolder holder, int position) {
        holder.bind(getItem(position), clickListener);
    }

    static class ContractViewHolder extends RecyclerView.ViewHolder {

        private final TextView textContract;
        private final ImageView imgStatus;
        private final DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault());

        public ContractViewHolder(@NonNull View itemView) {
            super(itemView);
            textContract = itemView.findViewById(R.id.textContractDetails);
            imgStatus = itemView.findViewById(R.id.imgActiveStatus);
        }

        public void bind(ContractEntity contract, OnContractClickListener clickListener) {
            String start = contract.startDate != null ? dateFormat.format(new Date(contract.startDate)) : "—";
            String end = contract.endDate != null ? dateFormat.format(new Date(contract.endDate)) : "—";

            textContract.setText(contract.vesselName + " — " + contract.position +
                    "\n" + start + " ➝ " + end);

            imgStatus.setVisibility(View.VISIBLE);
            if (contract.endDate == null) {
                // Контракт активный
                imgStatus.setColorFilter(ContextCompat.getColor(itemView.getContext(), R.color.contract_active)); // зелёный
            } else {
                // Контракт завершён
                imgStatus.setColorFilter(ContextCompat.getColor(itemView.getContext(), R.color.contract_inactive)); // серый
            }

            itemView.setOnClickListener(v -> {
                if (clickListener != null) {
                    clickListener.onContractClicked(contract.id);
                }
            });
        }
    }
}
