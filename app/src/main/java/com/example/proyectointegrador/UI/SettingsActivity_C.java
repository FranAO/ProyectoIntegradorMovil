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
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

import com.example.proyectointegrador.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.concurrent.Executor;

public class SettingsActivity_C extends AppCompatActivity {

    private MaterialButton cardBackButton;
    private CardView logoutCard;
    private CardView changePasswordCard;
    private TextView tvUserName;
    private SwitchMaterial switchBiometric;

    private SharedPreferences prefs;
    private static final String PREFS_NAME = "MiAppPrefs";
    private static final String BIOMETRIC_USER_ID = "BIOMETRIC_USER_ID";
    private static final String LOGGED_IN_USER_EMAIL = "LOGGED_IN_USER_EMAIL";

    private Executor executor;
    private BiometricPrompt biometricPrompt;
    private BiometricPrompt.PromptInfo promptInfo;

    private String currentUserEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_settings_c);

        // Fixed: card_back_button is a MaterialButton, not a CardView
        cardBackButton = findViewById(R.id.card_back_button);
        logoutCard = (CardView) findViewById(R.id.logoutCard);
        changePasswordCard = (CardView) findViewById(R.id.changePasswordCard);
        tvUserName = findViewById(R.id.tv_user_name);
        switchBiometric = findViewById(R.id.switch_biometric);

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        currentUserEmail = prefs.getString(LOGGED_IN_USER_EMAIL, "chofer@correo.com");

        tvUserName.setText(currentUserEmail);
        loadSwitchState();
        setupBiometrics();

        cardBackButton.setOnClickListener(v -> {
            Intent intent = new Intent(SettingsActivity_C.this, MainActivity_C.class);
            startActivity(intent);
            finish();
        });

        changePasswordCard.setOnClickListener(v -> {
            showChangePasswordDialog();
        });

        logoutCard.setOnClickListener(v -> {
            promptLogout();
        });

        switchBiometric.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                authenticateToEnable();
            } else {
                saveBiometricLink(null);
                Toast.makeText(this, "Huella deshabilitada", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadSwitchState() {
        String savedUser = prefs.getString(BIOMETRIC_USER_ID, null);
        switchBiometric.setChecked(savedUser != null && savedUser.equals(currentUserEmail));
    }

    private void setupBiometrics() {
        executor = ContextCompat.getMainExecutor(this);

        biometricPrompt = new BiometricPrompt(SettingsActivity_C.this, executor,
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                        super.onAuthenticationError(errorCode, errString);
                        Toast.makeText(SettingsActivity_C.this, "Error: " + errString, Toast.LENGTH_SHORT).show();
                        switchBiometric.setChecked(false);
                    }

                    @Override
                    public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                        super.onAuthenticationSucceeded(result);
                        Toast.makeText(SettingsActivity_C.this, "¡Huella habilitada!", Toast.LENGTH_SHORT).show();
                        saveBiometricLink(currentUserEmail);
                    }

                    @Override
                    public void onAuthenticationFailed() {
                        super.onAuthenticationFailed();
                        Toast.makeText(SettingsActivity_C.this, "Fallo de autenticación", Toast.LENGTH_SHORT).show();
                        switchBiometric.setChecked(false);
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

        if (savedUser != null && !savedUser.equals(currentUserEmail)) {

            new AlertDialog.Builder(this)
                    .setTitle("Advertencia")
                    .setMessage("La huella ya está enlazada a otro usuario (" + savedUser
                            + "). ¿Deseas reemplazarla y enlazarla a " + currentUserEmail + "?")
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

    private void saveBiometricLink(String email) {
        SharedPreferences.Editor editor = prefs.edit();
        if (email == null) {
            editor.remove(BIOMETRIC_USER_ID);
        } else {
            editor.putString(BIOMETRIC_USER_ID, email);
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

    private void showChangePasswordDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        android.view.View view = getLayoutInflater().inflate(R.layout.dialog_change_password, null);

        com.google.android.material.textfield.TextInputEditText etCurrentPassword = view
                .findViewById(R.id.etCurrentPassword);
        com.google.android.material.textfield.TextInputEditText etNewPassword = view.findViewById(R.id.etNewPassword);
        com.google.android.material.textfield.TextInputEditText etConfirmPassword = view
                .findViewById(R.id.etConfirmPassword);

        builder.setView(view);
        android.app.AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        view.findViewById(R.id.btnCancel).setOnClickListener(v -> dialog.dismiss());
        view.findViewById(R.id.btnConfirm).setOnClickListener(v -> {
            String currentPassword = etCurrentPassword.getText().toString().trim();
            String newPassword = etNewPassword.getText().toString().trim();
            String confirmPassword = etConfirmPassword.getText().toString().trim();

            if (currentPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Por favor completa todos los campos", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!newPassword.equals(confirmPassword)) {
                Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show();
                return;
            }

            if (newPassword.length() < 6) {
                Toast.makeText(this, "La contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show();
                return;
            }

            // Llamar al método para cambiar la contraseña
            cambiarContrasena(currentUserEmail, currentPassword, newPassword);
            dialog.dismiss();
        });

        dialog.show();
    }

    private void cambiarContrasena(String email, String currentPassword, String newPassword) {
        new Thread(() -> {
            try {
                android.util.Log.d("SettingsActivity_C", "🔐 Iniciando cambio de contraseña para: " + email);

                String apiUrl = com.example.proyectointegrador.Config.ApiConfig.getApiUrl(this,
                        "/Auth/change-password");

                android.util.Log.d("SettingsActivity_C", "🌐 URL: " + apiUrl);

                java.net.URL url = new java.net.URL(apiUrl);
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                org.json.JSONObject jsonBody = new org.json.JSONObject();
                jsonBody.put("email", email);
                jsonBody.put("currentPassword", currentPassword);
                jsonBody.put("newPassword", newPassword);

                android.util.Log.d("SettingsActivity_C", "📤 Request body: " + jsonBody.toString());

                java.io.OutputStream os = conn.getOutputStream();
                os.write(jsonBody.toString().getBytes());
                os.flush();
                os.close();

                int responseCode = conn.getResponseCode();
                android.util.Log.d("SettingsActivity_C", "📡 Response code: " + responseCode);

                // Leer el cuerpo de la respuesta
                String responseBody = "";
                try {
                    java.io.BufferedReader br;
                    if (responseCode >= 200 && responseCode < 300) {
                        br = new java.io.BufferedReader(new java.io.InputStreamReader(conn.getInputStream()));
                    } else {
                        br = new java.io.BufferedReader(new java.io.InputStreamReader(conn.getErrorStream()));
                    }
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        sb.append(line);
                    }
                    br.close();
                    responseBody = sb.toString();
                    android.util.Log.d("SettingsActivity_C", "📥 Response body: " + responseBody);
                } catch (Exception e) {
                    android.util.Log.e("SettingsActivity_C", "⚠️ Error leyendo response body: " + e.getMessage());
                }

                final String finalResponseBody = responseBody;
                runOnUiThread(() -> {
                    if (responseCode == 200) {
                        Toast.makeText(this, "Contraseña cambiada exitosamente", Toast.LENGTH_LONG).show();
                    } else if (responseCode == 400 || responseCode == 401) {
                        Toast.makeText(this, "Error: Contraseña actual incorrecta", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Error " + responseCode + ": " + finalResponseBody,
                                Toast.LENGTH_LONG).show();
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                android.util.Log.e("SettingsActivity_C", "❌ Error cambiando contraseña: " + e.getMessage());
                runOnUiThread(() -> {
                    Toast.makeText(this, "Error de conexión: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void performLogout() {

        prefs.edit()
                .remove(LOGGED_IN_USER_EMAIL)
                .apply();

        Intent intent = new Intent(this, Login.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}