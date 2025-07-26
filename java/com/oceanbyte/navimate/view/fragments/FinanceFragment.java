package com.oceanbyte.navimate.view.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.View;

import androidx.fragment.app.Fragment;

import com.oceanbyte.navimate.utils.KeyboardUtils;


public class FinanceFragment extends Fragment {
    public FinanceFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Скрытие клавиатуры при тапе вне полей ввода

        return inflater.inflate(com.oceanbyte.navimate.R.layout.fragment_finance, container, false);


    }
}