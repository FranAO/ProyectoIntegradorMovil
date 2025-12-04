package com.example.proyectointegrador.UI;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import com.example.proyectointegrador.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.concurrent.Executor;

public class SettingsActivity extends AppCompatActivity {

    private MaterialButton cardBackButton;
    private TextView tvUserEmail;
    private SwitchMaterial switchBiometric;
    private androidx.cardview.widget.CardView logoutCard;

    private SharedPreferences prefs;
    private static final String PREFS_NAME = "MiAppPrefs";
    private static final String BIOMETRIC_USER_ID = "BIOMETRIC_USER_ID";
    private static final String LOGGED_IN_USER_EMAIL = "LOGGED_IN_USER_EMAIL";

    private Executor executor;
    private BiometricPrompt biometricPrompt;
    private BiometricPrompt.PromptInfo promptInfo;

    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_settings);
        cardBackButton = findViewById(R.id.card_back_button);
        tvUserEmail = findViewById(R.id.tv_user_email);
        switchBiometric = findViewById(R.id.switch_biometric);
        logoutCard = findViewById(R.id.logoutCard);

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        currentUserId = prefs.getString(LOGGED_IN_USER_EMAIL, "usuario@ejemplo.com");

        tvUserEmail.setText(currentUserId);
        loadSwitchState();
        setupBiometrics();

        cardBackButton.setOnClickListener(v -> {
            Intent intent = new Intent(SettingsActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        });

        switchBiometric.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Evitar llamadas recursivas durante la carga del estado
            if (!buttonView.isPressed()) {
                return;
            }
            
            if (isChecked) {
                authenticateToEnable();
            } else {
                saveBiometricUser(null);
                Toast.makeText(this, "Huella deshabilitada", Toast.LENGTH_SHORT).show();
            }
        });

        logoutCard.setOnClickListener(v -> {
            promptLogout();
        });
    }

    private void loadSwitchState() {
        String savedUser = prefs.getString(BIOMETRIC_USER_ID, null);
        switchBiometric.setChecked(savedUser != null && savedUser.equals(currentUserId));
    }

    private void setupBiometrics() {
        executor = ContextCompat.getMainExecutor(this);

        biometricPrompt = new BiometricPrompt(SettingsActivity.this, executor,
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                        super.onAuthenticationError(errorCode, errString);
                        // Solo resetear el switch si NO es una cancelación del usuario
                        if (errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON && 
                            errorCode != BiometricPrompt.ERROR_USER_CANCELED) {
                            Toast.makeText(SettingsActivity.this, "Error: " + errString, Toast.LENGTH_SHORT).show();
                            switchBiometric.setChecked(false);
                        } else {
                            // Usuario canceló, mantener el estado anterior
                            loadSwitchState();
                        }
                    }

                    @Override
                    public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                        super.onAuthenticationSucceeded(result);
                        Toast.makeText(SettingsActivity.this, "Huella habilitada correctamente", Toast.LENGTH_SHORT).show();
                        saveBiometricUser(currentUserId);
                        // Asegurar que el switch esté en el estado correcto
                        switchBiometric.setChecked(true);
                    }

                    @Override
                    public void onAuthenticationFailed() {
                        super.onAuthenticationFailed();
                        Toast.makeText(SettingsActivity.this, "Huella no reconocida, intenta de nuevo", Toast.LENGTH_SHORT).show();
                        // No resetear el switch aquí, dar otra oportunidad al usuario
                    }
                });

        promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Confirmar para habilitar")
                .setSubtitle("Confirma tu huella para activar el inicio de sesión")
                .setNegativeButtonText("Cancelar")
                .build();
    }

    private void authenticateToEnable() {
        String savedUser = prefs.getString(BIOMETRIC_USER_ID, null);

        if (savedUser != null && !savedUser.equals(currentUserId)) {

            new AlertDialog.Builder(this)
                    .setTitle("Advertencia")
                    .setMessage("La huella ya está enlazada a otra cuenta (" + savedUser + "). ¿Deseas reemplazarla y enlazarla a " + currentUserId + "?")
                    .setPositiveButton("Reemplazar", (dialog, which) -> {
                        requestBiometricAuth();
                    })
                    .setNegativeButton("Cancelar", (dialog, which) -> {
                        switchBiometric.setChecked(false);
                    })
                    .show();

        } else {
            requestBiometricAuth();
        }
    }

    private void requestBiometricAuth() {
        BiometricManager biometricManager = BiometricManager.from(this);
        int authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG;

        if (biometricManager.canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS) {
            biometricPrompt.authenticate(promptInfo);
        } else {
            Toast.makeText(this, "No se puede usar la huella en este dispositivo.", Toast.LENGTH_SHORT).show();
            switchBiometric.setChecked(false);
        }
    }

    private void saveBiometricUser(String userId) {
        SharedPreferences.Editor editor = prefs.edit();
        if (userId == null) {
            editor.remove(BIOMETRIC_USER_ID);
        } else {
            editor.putString(BIOMETRIC_USER_ID, userId);
        }
        editor.apply();
    }

    private void promptLogout() {
        android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_logout_confirmation);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        
        // Configurar ancho y alto del diálogo
        android.view.WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
        params.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.90);
        params.height = (int) (getResources().getDisplayMetrics().heightPixels * 0.85);
        dialog.getWindow().setAttributes(params);

        com.google.android.material.button.MaterialButton btnCancel = dialog.findViewById(R.id.btnCancel);
        com.google.android.material.button.MaterialButton btnConfirm = dialog.findViewById(R.id.btnConfirm);

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnConfirm.setOnClickListener(v -> {
            dialog.dismiss();
            performLogout();
        });

        dialog.setCancelable(true);
        dialog.show();
    }

    private void performLogout() {
        prefs.edit()
                .remove(LOGGED_IN_USER_EMAIL)
                .apply();
        
        Toast.makeText(this, "Sesión cerrada", Toast.LENGTH_SHORT).show();
        
        Intent intent = new Intent(SettingsActivity.this, Login.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}