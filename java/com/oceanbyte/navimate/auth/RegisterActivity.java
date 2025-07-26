package com.oceanbyte.navimate.auth;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.oceanbyte.navimate.R;
import com.oceanbyte.navimate.database.AppDatabase;
import com.oceanbyte.navimate.models.UserProfile;

import java.util.concurrent.Executors;

public class RegisterActivity extends AppCompatActivity {

    private EditText nameEditText, emailEditText, passwordEditText, confirmPasswordEditText;
    private Button registerButton;
    private FirebaseAuth mAuth;
    private View registerProgressBar;

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(com.oceanbyte.navimate.R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        nameEditText = findViewById(R.id.nameEditText);
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        confirmPasswordEditText = findViewById(R.id.confirmPasswordEditText);
        registerButton = findViewById(R.id.registerButton);
        registerProgressBar = findViewById(R.id.registerProgressBar);

        registerButton.setOnClickListener(v -> registerUser());
    }

    private void setInputsEnabled(boolean enabled) {
        nameEditText.setEnabled(enabled);
        emailEditText.setEnabled(enabled);
        passwordEditText.setEnabled(enabled);
        confirmPasswordEditText.setEnabled(enabled);
        registerButton.setEnabled(enabled);
    }

    private void registerUser() {
        String name = nameEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        String confirmPassword = confirmPasswordEditText.getText().toString().trim();

        if (name.isEmpty()) {
            nameEditText.setError(getString(R.string.error_name_empty));
            return;
        }
        if (email.isEmpty()) {
            emailEditText.setError(getString(R.string.error_email_empty));
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEditText.setError(getString(R.string.error_email_invalid));
            return;
        }
        if (password.isEmpty()) {
            passwordEditText.setError(getString(R.string.error_password_empty));
            return;
        }
        if (password.length() < 6) {
            passwordEditText.setError(getString(R.string.error_password_short));
            return;
        }
        if (!password.equals(confirmPassword)) {
            confirmPasswordEditText.setError(getString(R.string.error_password_mismatch));
            return;
        }

        setInputsEnabled(false);
        registerProgressBar.setVisibility(View.VISIBLE);

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    registerProgressBar.setVisibility(View.GONE);
                    setInputsEnabled(true);
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            String uid = user.getUid();
                            Executors.newSingleThreadExecutor().execute(() -> {
                                UserProfile profile = new UserProfile(uid, name);
                                profile.id = 1;
                                AppDatabase.getInstance(getApplicationContext()).userProfileDao().insertOrUpdate(profile);
                            });
                            user.sendEmailVerification().addOnCompleteListener(verifyTask -> {
                                if (verifyTask.isSuccessful()) {
                                    showToast(getString(R.string.verify_email_message));
                                    startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                                    finish();
                                } else {
                                    showToast(getString(R.string.error_verification_failed));
                                }
                            });
                        }
                    } else {
                        String errorMsg = getString(R.string.error_registration_failed);
                        Exception ex = task.getException();
                        if (ex != null && ex.getMessage() != null) {
                            errorMsg += ex.getMessage();
                        }
                        showToast(errorMsg);
                    }
                });

    }
}