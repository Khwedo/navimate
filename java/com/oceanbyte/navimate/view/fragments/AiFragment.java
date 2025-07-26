package com.oceanbyte.navimate.view.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.oceanbyte.navimate.R;
import com.oceanbyte.navimate.utils.KeyboardUtils;

public class AiFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Скрытие клавиатуры при тапе вне полей ввода

        return inflater.inflate(R.layout.fragment_ai, container, false);
    }
}