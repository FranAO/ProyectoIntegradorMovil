package com.example.proyectointegrador.UI;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.method.PasswordTransformationMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.cardview.widget.CardView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.proyectointegrador.Config.ApiConfig;
import com.example.proyectointegrador.Database.DataSyncManager;
import com.example.proyectointegrador.Models.Student;
import com.example.proyectointegrador.R;
import com.example.proyectointegrador.Services.ApiService;
import com.google.android.material.button.MaterialButton;

import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Executor;

public class Login extends AppCompatActivity {

    private EditText emailEditText, passwordEditText;
    private ImageView togglePasswordVisibility;
    private MaterialButton loginButton;

    private Executor executor;
    private BiometricPrompt biometricPrompt;
    private BiometricPrompt.PromptInfo promptInfo;

    private SharedPreferences prefs;
    private static final String PREFS_NAME = "MiAppPrefs";
    private static final String BIOMETRIC_USER_ID = "BIOMETRIC_USER_ID";
    private static final String LOGGED_IN_USER_EMAIL = "LOGGED_IN_USER_EMAIL";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        togglePasswordVisibility = findViewById(R.id.togglePasswordVisibility);
        loginButton = findViewById(R.id.loginButton);

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // Inicializar ApiService con el contexto
        ApiService.initialize(this);

        setupBiometricLogic();

        String biometricUserId = prefs.getString(BIOMETRIC_USER_ID, null);
        if (biometricUserId != null) {
            authenticateWithBiometrics();
        }

        loginButton.setOnClickListener(v -> handleLogin());

        togglePasswordVisibility.setOnClickListener(v -> handlePasswordToggle());
        
        // Long click en el logo para abrir configuración de servidor
        findViewById(R.id.logoImageView).setOnLongClickListener(v -> {
            showServerConfigDialog();
            return true;
        });
    }

    private void setupBiometricLogic() {
        executor = ContextCompat.getMainExecutor(this);

        biometricPrompt = new BiometricPrompt(Login.this, executor,
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                        super.onAuthenticationError(errorCode, errString);
                        Toast.makeText(getApplicationContext(), "Cancelado", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                        super.onAuthenticationSucceeded(result);
                        String userId = prefs.getString(BIOMETRIC_USER_ID, null);
                        String role = prefs.getString("USER_ROLE", "student");
                        Toast.makeText(getApplicationContext(), "Bienvenido " + userId, Toast.LENGTH_SHORT).show();
                        saveLoggedInUser(userId);
                        goToMain(role);
                    }

                    @Override
                    public void onAuthenticationFailed() {
                        super.onAuthenticationFailed();
                    }
                });

        promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Inicio de sesión biométrico")
                .setSubtitle("Inicia sesión usando tu huella dactilar")
                .setNegativeButtonText("Usar contraseña")
                .build();
    }

    private void authenticateWithBiometrics() {
        BiometricManager biometricManager = BiometricManager.from(this);
        int authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG;

        if (biometricManager.canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS) {
            biometricPrompt.authenticate(promptInfo);
        } else {
            Toast.makeText(this, "Huella no disponible, usa tu contraseña.", Toast.LENGTH_SHORT).show();
        }
    }

    private void handleLogin() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Por favor, completa todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(() -> {
            try {
                String apiUrl = ApiConfig.getApiUrl(Login.this, "/Auth/login");
                
                URL url = new URL(apiUrl);
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("POST");
                con.setRequestProperty("Content-Type", "application/json");
                con.setDoOutput(true);
                con.setConnectTimeout(10000);
                con.setReadTimeout(10000);

                JSONObject loginData = new JSONObject();
                loginData.put("email", email);
                loginData.put("password", password);


                con.getOutputStream().write(loginData.toString().getBytes());

                int responseCode = con.getResponseCode();
                
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    // Leer la respuesta
                    java.io.BufferedReader reader = new java.io.BufferedReader(
                            new java.io.InputStreamReader(con.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();
                    
                    String responseBody = response.toString();
                    
                    // Parsear el token
                    JSONObject jsonResponse = new JSONObject(responseBody);
                    String token = jsonResponse.optString("token");
                    
                    if (token != null && !token.isEmpty()) {
                        // Guardar el token
                        prefs.edit().putString("AUTH_TOKEN", token).apply();
                    }
                    
                    // Parsear respuesta para obtener el rol
                    String role = jsonResponse.optString("role", "student");
                    
                    // Guardar rol y email en SharedPreferences
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putString("USER_ROLE", role);
                    editor.putString(LOGGED_IN_USER_EMAIL, email);
                    editor.apply();
                    
                    // Sincronizar según el rol
                    if (role != null && role.equalsIgnoreCase("driver")) {
                        // Para drivers, no necesitamos sincronizar student
                        // Los datos del driver se cargarán en MainActivity_C
                        runOnUiThread(() -> {
                            Toast.makeText(this, "Bienvenido Conductor", Toast.LENGTH_SHORT).show();
                            goToMain(role);
                        });
                    } else {
                        // Para students, sincronizar datos locales
                        DataSyncManager syncManager = new DataSyncManager(this);
                        Student student = syncManager.sincronizarEstudiantePorEmail(email);

                        runOnUiThread(() -> {
                            if (student != null) {
                                Toast.makeText(this, "Bienvenido Estudiante", Toast.LENGTH_SHORT).show();
                                goToMain(role);
                            } else {
                                Toast.makeText(this, "Error al sincronizar datos del estudiante", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                } else {
                    // Leer el error
                    java.io.BufferedReader reader = new java.io.BufferedReader(
                            new java.io.InputStreamReader(con.getErrorStream()));
                    StringBuilder errorResponse = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        errorResponse.append(line);
                    }
                    reader.close();
                    
                    String errorBody = errorResponse.toString();
                    
                    runOnUiThread(() -> Toast.makeText(this, "Credenciales incorrectas", Toast.LENGTH_SHORT).show());
                }
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "Error de conexión: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void handlePasswordToggle() {
        if (passwordEditText.getTransformationMethod() instanceof PasswordTransformationMethod) {
            passwordEditText.setTransformationMethod(null);
        } else {
            passwordEditText.setTransformationMethod(PasswordTransformationMethod.getInstance());
        }
        passwordEditText.setSelection(passwordEditText.length());
    }

    private void showServerConfigDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_server_config, null);
        
        RadioButton rbEmulator = dialogView.findViewById(R.id.rbEmulator);
        RadioButton rbPhysical = dialogView.findViewById(R.id.rbPhysical);
        CardView cardEmulator = dialogView.findViewById(R.id.cardEmulator);
        CardView cardPhysical = dialogView.findViewById(R.id.cardPhysical);
        LinearLayout ipInputContainer = dialogView.findViewById(R.id.ipInputContainer);
        EditText etCustomIp = dialogView.findViewById(R.id.etCustomIp);
        MaterialButton btnConfirm = dialogView.findViewById(R.id.btnConfirm);
        
        // Mostrar URL actual
        String currentUrl = ApiConfig.getBaseUrl(this);
        if (currentUrl.contains("10.0.2.2")) {
            rbEmulator.setChecked(true);
            rbPhysical.setChecked(false);
            ipInputContainer.setVisibility(View.GONE);
        } else {
            rbPhysical.setChecked(true);
            rbEmulator.setChecked(false);
            ipInputContainer.setVisibility(View.VISIBLE);
            // Extraer IP de la URL
            String ip = currentUrl.replace("http://", "").replace(":5090", "");
            etCustomIp.setText(ip);
        }
        
        // Listeners para cards
        cardEmulator.setOnClickListener(v -> {
            rbEmulator.setChecked(true);
            rbPhysical.setChecked(false);
            ipInputContainer.setVisibility(View.GONE);
        });
        
        cardPhysical.setOnClickListener(v -> {
            rbPhysical.setChecked(true);
            rbEmulator.setChecked(false);
            ipInputContainer.setVisibility(View.VISIBLE);
        });
        
        // Listeners para RadioButtons
        rbEmulator.setOnClickListener(v -> {
            rbPhysical.setChecked(false);
            ipInputContainer.setVisibility(View.GONE);
        });
        
        rbPhysical.setOnClickListener(v -> {
            rbEmulator.setChecked(false);
            ipInputContainer.setVisibility(View.VISIBLE);
        });
        
        AlertDialog dialog = builder.setView(dialogView).create();
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        
        btnConfirm.setOnClickListener(v -> {
            if (rbEmulator.isChecked()) {
                ApiConfig.useEmulator(this);
                Toast.makeText(this, "Configurado para Emulador", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            } else {
                String ip = etCustomIp.getText().toString().trim();
                if (!ip.isEmpty()) {
                    ApiConfig.usePhysicalDevice(this, ip);
                    Toast.makeText(this, "Configurado para: " + ip, Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                } else {
                    Toast.makeText(this, "Ingrese una IP válida", Toast.LENGTH_SHORT).show();
                }
            }
        });
        
        dialog.show();
    }

    private void saveLoggedInUser(String email) {
        prefs.edit().putString(LOGGED_IN_USER_EMAIL, email).apply();
    }

    private void goToMain(String role) {
        Intent intent;
        // Verificar rol y redirigir a la actividad correspondiente
        if (role != null && role.equalsIgnoreCase("driver")) {
            // Si es driver, ir a MainActivity_C (chofer)
            intent = new Intent(Login.this, MainActivity_C.class);
        } else {
            // Si es student o cualquier otro rol, ir a MainActivity (cliente)
            intent = new Intent(Login.this, MainActivity.class);
        }
        startActivity(intent);
        finish();
    }
}