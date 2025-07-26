package com.oceanbyte.navimate.auth;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Patterns;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import android.view.View;
import com.oceanbyte.navimate.R;
import com.oceanbyte.navimate.view.MainActivity;

public class LoginActivity extends AppCompatActivity {

    // Поля для ввода email и пароля, кнопка входа
    // FirebaseAuth для работы с аутентификацией

    private EditText loginEmail, loginPassword;
    private Button loginButton;
    private FirebaseAuth mAuth;
    private View progressBar; // Loading indicator

    // Helper for showing Toasts
    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    // onCreate вызывается при создании активности
    // Здесь мы инициализируем FirebaseAuth, настраиваем UI и обрабатываем события

    // Инициализация FirebaseAuth
    @SuppressLint("MissingInflatedId")
    @Override
    // Если пользователь уже авторизован и email подтверждён, сразу переходим в MainActivity
    // Иначе показываем форму логина
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();

        if (mAuth.getCurrentUser() != null && mAuth.getCurrentUser().isEmailVerified()) {
            startMainActivity(mAuth.getCurrentUser().getEmail());
            return;
        }

        setTitle(getString(R.string.login_title));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        loginEmail = findViewById(R.id.loginEmail);
        loginPassword = findViewById(R.id.loginPassword);
        loginButton = findViewById(R.id.loginButton);
        progressBar = findViewById(R.id.loginProgressBar);

        loginEmail.setContentDescription(getString(R.string.email_field_desc));
        loginPassword.setContentDescription(getString(R.string.password_field_desc));
        loginButton.setContentDescription(getString(R.string.login_button_desc));

        loginButton.setOnClickListener(v -> loginUser());
        findViewById(R.id.forgotPassword).setOnClickListener(v -> showPasswordResetDialog());
        findViewById(R.id.registerRedirect).setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
            finish();
        });
        setupKeyboardHideOnOutsideClick();
    }

    private void loginUser() {
        // Получаем введённые данные
        // Проверяем корректность email и пароля
        // Если всё верно, пытаемся авторизоваться через FirebaseAuth
        // При успехе переходим в MainActivity, иначе показываем ошибку
        String email = loginEmail.getText().toString().trim();
        String password = loginPassword.getText().toString().trim();

        loginEmail.setBackgroundResource(android.R.color.transparent);
        loginPassword.setBackgroundResource(android.R.color.transparent);

        if (email.isEmpty()) {
            loginEmail.setBackgroundResource(R.drawable.red_border);
            showToast(getString(R.string.error_email_empty));
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            loginEmail.setBackgroundResource(R.drawable.red_border);
            showToast(getString(R.string.error_email_invalid));
            return;
        }
        if (password.isEmpty()) {
            loginPassword.setBackgroundResource(R.drawable.red_border);
            showToast(getString(R.string.error_password_empty));
            return;
        }
        if (password.length() < 6) {
            loginPassword.setBackgroundResource(R.drawable.red_border);
            showToast(getString(R.string.error_password_short));
            return;
        }

        hideKeyboard(loginButton);
        progressBar.setVisibility(View.VISIBLE);
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    progressBar.setVisibility(View.GONE);
                    if (mAuth.getCurrentUser() != null && mAuth.getCurrentUser().isEmailVerified()) {
                        startMainActivity(email);
                    } else {
                        showToast(getString(R.string.error_email_not_verified));
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    showToast(getString(R.string.error_login_failed) + e.getMessage());
                });
    }

    private void showPasswordResetDialog() {
        // Показываем диалог для сброса пароля
        // Пользователь вводит email, на который будет отправлено письмо для сброса пароля
        if (mAuth.getCurrentUser() != null) {
            showToast(getString(R.string.error_already_logged_in));
            return;
        }
        EditText resetEmail = new EditText(this);
        resetEmail.setHint(getString(R.string.email_hint));
        resetEmail.setContentDescription(getString(R.string.email_field_desc));

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.reset_password_title))
                .setView(resetEmail)
                .setPositiveButton(getString(R.string.send), null)
                .setNegativeButton(getString(R.string.cancel), null)
                .show();

        // After showing, get the dialog and set up loading indicator and button disabling
        AlertDialog dialog = (AlertDialog) resetEmail.getParent().getParent();
        if (dialog != null) {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String email = resetEmail.getText().toString().trim();
                if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    showToast(getString(R.string.error_email_invalid));
                    return;
                }
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                // Show loading indicator (reuse ProgressBar if desired)
                progressBar.setVisibility(View.VISIBLE);
                mAuth.sendPasswordResetEmail(email)
                        .addOnSuccessListener(aVoid -> {
                            progressBar.setVisibility(View.GONE);
                            showToast(getString(R.string.reset_email_sent));
                            dialog.dismiss();
                        })
                        .addOnFailureListener(e -> {
                            progressBar.setVisibility(View.GONE);
                            showToast(getString(R.string.error_reset_failed) + e.getMessage());
                            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                        });
            });
        }
    }

    private void startMainActivity(String email) {
        // NOTE: For sensitive data, consider using encrypted storage in production
        hideKeyboard(loginButton);
        SharedPreferences prefs = getSharedPreferences("app_settings", MODE_PRIVATE);
        prefs.edit().putString("user_email", email).apply();
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void setupKeyboardHideOnOutsideClick() {
        // Настраиваем скрытие клавиатуры при клике вне полей ввода
        // Также скрываем клавиатуру при потере фокуса полями ввода
        // Убираем красную рамку при вводе
        loginEmail.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) hideKeyboard(v);
        });
        loginPassword.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) hideKeyboard(v);
        });
        findViewById(R.id.loginEmail).setOnClickListener(v -> hideKeyboard(v));
        findViewById(R.id.loginPassword).setOnClickListener(v -> hideKeyboard(v));
    }

    private void hideKeyboard(View v) {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        // Скрываем клавиатуру при клике вне полей ввода
        // Используем InputMethodManager для управления клавиатурой
        if (imm != null) imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
    }
}
