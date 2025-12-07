package com.example.proyectointegrador.UI;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.Image;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.proyectointegrador.Config.ApiConfig;
import com.example.proyectointegrador.R;
import com.google.android.material.button.MaterialButton;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CameraActivity extends BaseNavigationActivity_C {
    private static final String TAG = "CameraActivity";
    private static final int CAMERA_PERMISSION_CODE = 100;
    private static final long RESCAN_DELAY_MS = 3000; // 3 segundos antes de permitir escanear de nuevo
    private PreviewView previewView;
    private TextView tvResultado;
    private ExecutorService cameraExecutor;
    private BarcodeScanner scanner;
    private boolean isScanning = true;
    private boolean isProcessing = false; // Evitar procesamiento múltiple
    private String lastScannedQR = ""; // Evitar procesar el mismo QR múltiples veces
    private long lastScanTime = 0; // Timestamp del último escaneo

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_camera);

        setupNavigation();

        previewView = findViewById(R.id.previewView);
        tvResultado = findViewById(R.id.tvResultado);

        // Configuración optimizada para QR codes
        BarcodeScannerOptions options = new BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE, Barcode.FORMAT_ALL_FORMATS)
                .enableAllPotentialBarcodes()
                .build();
        scanner = BarcodeScanning.getClient(options);

        cameraExecutor = Executors.newSingleThreadExecutor();

        if (checkCameraPermission()) {
            startCamera();
        } else {
            requestCameraPermission();
        }
    }

    @Override
    protected int getNavigationIndex() {
        return 3;
    }

    private boolean checkCameraPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.CAMERA }, CAMERA_PERMISSION_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                Toast.makeText(this, "Permiso de cámara denegado", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    // Método eliminado: Ya no usamos modal de validación, validación es directa

    @OptIn(markerClass = ExperimentalGetImage.class)
    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                // Preview
                Preview preview = new Preview.Builder()
                        .build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                // Selector de cámara trasera
                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                // Análisis de imagen con configuración optimizada
                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setTargetRotation(previewView.getDisplay().getRotation())
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                        .build();

                imageAnalysis.setAnalyzer(cameraExecutor, this::analyzeImage);

                // Desvincula todos los casos de uso antes de vincular nuevos
                cameraProvider.unbindAll();

                // Vincula los casos de uso al ciclo de vida
                Camera camera = cameraProvider.bindToLifecycle(
                        this,
                        cameraSelector,
                        preview,
                        imageAnalysis);

            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Error al iniciar cámara: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show());
            }
        }, ContextCompat.getMainExecutor(this));
    }

    @androidx.camera.core.ExperimentalGetImage
    private void analyzeImage(ImageProxy imageProxy) {
        // Si no estamos escaneando o ya estamos procesando, cerrar imagen
        if (!isScanning || isProcessing) {
            imageProxy.close();
            return;
        }

        Image mediaImage = imageProxy.getImage();

        if (mediaImage == null) {
            imageProxy.close();
            return;
        }

        try {
            // Crear InputImage con rotación correcta
            InputImage image = InputImage.fromMediaImage(
                    mediaImage,
                    imageProxy.getImageInfo().getRotationDegrees());

            // Procesar imagen con MLKit (sin log para evitar spam)
            scanner.process(image)
                    .addOnSuccessListener(barcodes -> {
                        if (!barcodes.isEmpty()) {

                            for (Barcode barcode : barcodes) {
                                String qrContent = barcode.getRawValue();
                                long currentTime = System.currentTimeMillis();

                                // Validaciones: no nulo, no vacío, no procesando, no es el mismo QR reciente,
                                // pasó suficiente tiempo
                                boolean isDifferentQR = !qrContent.equals(lastScannedQR);
                                boolean enoughTimePassed = (currentTime - lastScanTime) > RESCAN_DELAY_MS;

                                if (qrContent != null && !qrContent.isEmpty() && !isProcessing
                                        && (isDifferentQR || enoughTimePassed)) {
                                    isProcessing = true;
                                    isScanning = false;
                                    lastScannedQR = qrContent;
                                    lastScanTime = currentTime;

                                    runOnUiThread(() -> {
                                        tvResultado.setText("QR detectado: " + qrContent);
                                        validarTicket(qrContent);
                                    });
                                    break;
                                }
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                    })
                    .addOnCompleteListener(task -> {
                        imageProxy.close();
                    });

        } catch (Exception e) {
            imageProxy.close();
        }
    }

    private void validarTicket(String qrContent) {

        // Extraer el ticketId del contenido del QR
        // Formato antiguo: ticketId|email|fecha
        // Formato nuevo: ticketId (solo)
        String ticketId = qrContent.trim();
        if (qrContent.contains("|")) {
            String[] parts = qrContent.split("\\|");
            ticketId = parts[0].trim(); // Tomar solo el ticketId (primera parte)
        } else {
        }

        final String finalTicketId = ticketId;

        // Obtener información del ticket y estudiante primero
        new Thread(() -> {
            try {

                // Obtener información del ticket
                String ticketUrl = ApiConfig.getApiUrl(CameraActivity.this, "/ticket/" + finalTicketId);

                URL urlTicket = new URL(ticketUrl);
                HttpURLConnection connTicket = (HttpURLConnection) urlTicket.openConnection();
                connTicket.setRequestMethod("GET");

                int responseCode = connTicket.getResponseCode();

                if (responseCode == 404) {
                    // Leer el mensaje de error del servidor
                    BufferedReader errorReader = new BufferedReader(new InputStreamReader(connTicket.getErrorStream()));
                    StringBuilder errorResponse = new StringBuilder();
                    String errorLine;
                    while ((errorLine = errorReader.readLine()) != null) {
                        errorResponse.append(errorLine);
                    }
                    errorReader.close();
                }

                if (responseCode != 200) {
                    runOnUiThread(() -> {
                        Toast.makeText(CameraActivity.this,
                                "Ticket no encontrado o inválido", Toast.LENGTH_SHORT).show();
                        tvResultado.setText("Ticket inválido. Esperando...");

                        // Esperar 3 segundos antes de permitir escanear de nuevo
                        new android.os.Handler().postDelayed(() -> {
                            if (!isFinishing() && !isDestroyed()) {
                                tvResultado.setText("Listo para escanear");
                                lastScannedQR = ""; // Limpiar para permitir re-escaneo
                                isProcessing = false;
                                isScanning = true;
                            }
                        }, RESCAN_DELAY_MS);
                    });
                    return;
                }

                BufferedReader readerTicket = new BufferedReader(new InputStreamReader(connTicket.getInputStream()));
                StringBuilder responseTicket = new StringBuilder();
                String lineTicket;
                while ((lineTicket = readerTicket.readLine()) != null) {
                    responseTicket.append(lineTicket);
                }
                readerTicket.close();

                final String ticketJsonString = responseTicket.toString();

                JSONObject ticketJson = new JSONObject(ticketJsonString);

                // Usar camelCase (el formato que retorna el API)
                final String studentId = ticketJson.getString("studentId");

                // Obtener información del estudiante usando email (el studentId es el email)
                URL urlStudent = new URL(ApiConfig.getApiUrl(CameraActivity.this, "/student/email/" + studentId));
                HttpURLConnection connStudent = (HttpURLConnection) urlStudent.openConnection();
                connStudent.setRequestMethod("GET");

                int studentResponseCode = connStudent.getResponseCode();

                if (studentResponseCode != 200) {
                    runOnUiThread(() -> {
                        Toast.makeText(CameraActivity.this,
                                "Error al obtener información del estudiante", Toast.LENGTH_SHORT).show();
                        tvResultado.setText("Error. Esperando...");

                        // Esperar 3 segundos antes de permitir escanear de nuevo
                        new android.os.Handler().postDelayed(() -> {
                            if (!isFinishing() && !isDestroyed()) {
                                tvResultado.setText("Listo para escanear");
                                lastScannedQR = "";
                                isProcessing = false;
                                isScanning = true;
                            }
                        }, RESCAN_DELAY_MS);
                    });
                    return;
                }

                BufferedReader readerStudent = new BufferedReader(new InputStreamReader(connStudent.getInputStream()));
                StringBuilder responseStudent = new StringBuilder();
                String lineStudent;
                while ((lineStudent = readerStudent.readLine()) != null) {
                    responseStudent.append(lineStudent);
                }
                readerStudent.close();

                JSONObject studentJson = new JSONObject(responseStudent.toString());
                final String firstName = studentJson.getString("firstName");
                final String lastName = studentJson.getString("lastName");
                final String nombreCompleto = firstName + " " + lastName;

                // Validación directa sin modal (sistema de tickets universales)
                runOnUiThread(() -> {
                    if (tvResultado != null) {
                        tvResultado.setText("Validando: " + nombreCompleto);
                    }
                    Toast.makeText(CameraActivity.this, "Validando ticket de " + nombreCompleto, Toast.LENGTH_SHORT)
                            .show();
                });

                // Procesar validación automáticamente (confirmar = true)
                procesarValidacionTicket(finalTicketId, studentId, nombreCompleto, true);

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    tvResultado.setText("Error al obtener información del ticket");
                    Toast.makeText(CameraActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();

                    // Esperar 3 segundos antes de permitir escanear de nuevo
                    new android.os.Handler().postDelayed(() -> {
                        if (!isFinishing() && !isDestroyed()) {
                            tvResultado.setText("Listo para escanear");
                            lastScannedQR = "";
                            isProcessing = false;
                            isScanning = true;
                        }
                    }, RESCAN_DELAY_MS);
                });
            }
        }).start();
    }

    // IMPORTANTE: Reemplazar TODO el método procesarValidacionTicket (líneas
    // 363-655) en CameraActivity.java con este código:

    private void procesarValidacionTicket(String ticketId, String studentId, String nombreEstudiante,
            boolean confirmar) {
        // Hacer las variables final para usarlas en lambdas
        final String finalTicketId = ticketId;
        final String finalStudentId = studentId;
        final String finalNombreEstudiante = nombreEstudiante;

        new Thread(() -> {
            try {
                SharedPreferences prefs = getSharedPreferences("MiAppPrefs", MODE_PRIVATE);
                String email = prefs.getString("LOGGED_IN_USER_EMAIL", "");

                if (email.isEmpty()) {
                    runOnUiThread(() -> {
                        Toast.makeText(CameraActivity.this, "Error: No se encontró el email del usuario",
                                Toast.LENGTH_SHORT).show();
                        isProcessing = false;
                        isScanning = true;
                    });
                    return;
                }

                // Obtener información del conductor
                URL urlDriver = new URL(ApiConfig.getApiUrl(CameraActivity.this, "/driver/email/" + email));
                HttpURLConnection connDriver = (HttpURLConnection) urlDriver.openConnection();
                connDriver.setRequestMethod("GET");

                BufferedReader readerDriver = new BufferedReader(new InputStreamReader(connDriver.getInputStream()));
                StringBuilder responseDriver = new StringBuilder();
                String lineDriver;
                while ((lineDriver = readerDriver.readLine()) != null) {
                    responseDriver.append(lineDriver);
                }
                readerDriver.close();

                String driverJsonString = responseDriver.toString();

                JSONObject driverJson = new JSONObject(driverJsonString);
                String driverId = driverJson.getString("id");

                // CAMBIO PRINCIPAL: PRE-VALIDAR sin necesidad de trip existente
                if (confirmar) {

                    SharedPreferences prefsToken = getSharedPreferences("MiAppPrefs", MODE_PRIVATE);
                    String token = prefsToken.getString("AUTH_TOKEN", "");


                    // Llamar al endpoint de pre-validación
                    String preValidateUrl = ApiConfig.getApiUrl(CameraActivity.this, "/passengerintrip/pre-validate");

                    URL urlPreValidate = new URL(preValidateUrl);
                    HttpURLConnection connPreValidate = (HttpURLConnection) urlPreValidate.openConnection();
                    connPreValidate.setRequestMethod("POST");
                    connPreValidate.setRequestProperty("Content-Type", "application/json");
                    connPreValidate.setRequestProperty("Authorization", "Bearer " + token);
                    connPreValidate.setDoOutput(true);

                    // Crear el JSON del request
                    JSONObject requestData = new JSONObject();
                    requestData.put("TicketId", finalTicketId);
                    requestData.put("DriverId", driverId);


                    java.io.OutputStream os = connPreValidate.getOutputStream();
                    os.write(requestData.toString().getBytes());
                    os.flush();
                    os.close();

                    int responseCode = connPreValidate.getResponseCode();

                    if (responseCode == 200) {
                        // Leer respuesta exitosa
                        BufferedReader readerValidate = new BufferedReader(
                                new InputStreamReader(connPreValidate.getInputStream()));
                        StringBuilder responseValidate = new StringBuilder();
                        String lineValidate;
                        while ((lineValidate = readerValidate.readLine()) != null) {
                            responseValidate.append(lineValidate);
                        }
                        readerValidate.close();

                        runOnUiThread(() -> {
                            if (!isFinishing() && !isDestroyed()) {
                                if (tvResultado != null) {
                                    tvResultado.setText("✓ Pasajero pre-registrado: " + finalNombreEstudiante);
                                    tvResultado.setTextColor(
                                            getResources().getColor(android.R.color.holo_green_dark, null));
                                }
                                Toast.makeText(CameraActivity.this,
                                        "✓ " + finalNombreEstudiante + " pre-registrado correctamente",
                                        Toast.LENGTH_LONG).show();

                                // Esperar 2 segundos antes de permitir escanear de nuevo
                                new android.os.Handler().postDelayed(() -> {
                                    if (!isFinishing() && !isDestroyed()) {
                                        tvResultado.setText("Listo para escanear");
                                        tvResultado.setTextColor(getResources().getColor(R.color.text_primary, null));
                                        lastScannedQR = "";
                                        isProcessing = false;
                                        isScanning = true;
                                    }
                                }, 2000);
                            }
                        });
                    } else {
                        // Leer mensaje de error del servidor
                        BufferedReader errorReader = new BufferedReader(
                                new InputStreamReader(connPreValidate.getErrorStream()));
                        StringBuilder errorResponse = new StringBuilder();
                        String errorLine;
                        while ((errorLine = errorReader.readLine()) != null) {
                            errorResponse.append(errorLine);
                        }
                        errorReader.close();

                        String errorMessage = errorResponse.toString();

                        // Parsear el error para mostrar mensaje más amigable
                        String userMessage = "Error al pre-validar ticket";

                        if (errorMessage.contains("already pre-validated")) {
                            userMessage = "✓ Pasajero ya estaba pre-registrado";
                        } else if (errorMessage.contains("already used")) {
                            userMessage = "Ticket ya fue utilizado";
                        } else if (errorMessage.contains("expired")) {
                            userMessage = "Ticket expirado";
                        }

                        String finalUserMessage = userMessage;

                        runOnUiThread(() -> {
                            if (!isFinishing() && !isDestroyed()) {
                                if (tvResultado != null) {
                                    tvResultado.setText(finalUserMessage);
                                    if (errorMessage.contains("already pre-validated")) {
                                        tvResultado.setTextColor(
                                                getResources().getColor(android.R.color.holo_orange_light, null));
                                    } else {
                                        tvResultado.setTextColor(getResources().getColor(R.color.error, null));
                                    }
                                }
                                Toast.makeText(CameraActivity.this, finalUserMessage, Toast.LENGTH_SHORT).show();

                                // Esperar antes de permitir escanear de nuevo
                                new android.os.Handler().postDelayed(() -> {
                                    if (!isFinishing() && !isDestroyed()) {
                                        tvResultado.setText("Listo para escanear");
                                        tvResultado.setTextColor(getResources().getColor(R.color.text_primary, null));
                                        lastScannedQR = "";
                                        isProcessing = false;
                                        isScanning = true;
                                    }
                                }, RESCAN_DELAY_MS);
                            }
                        });
                    }
                } else {
                    // Denegar el ticket - actualizar estado a "cancelled"

                    String updateUrl = ApiConfig.getApiUrl(CameraActivity.this, "/ticket/" + finalTicketId);

                    URL urlUpdate = new URL(updateUrl);
                    HttpURLConnection connUpdate = (HttpURLConnection) urlUpdate.openConnection();
                    connUpdate.setRequestMethod("PUT");
                    connUpdate.setRequestProperty("Content-Type", "application/json");
                    connUpdate.setDoOutput(true);

                    JSONObject updateData = new JSONObject();
                    updateData.put("Status", "cancelled");


                    java.io.OutputStream os = connUpdate.getOutputStream();
                    os.write(updateData.toString().getBytes());
                    os.flush();
                    os.close();

                    int responseCode = connUpdate.getResponseCode();

                    if (responseCode == 200 || responseCode == 204) {
                        runOnUiThread(() -> {
                            if (!isFinishing() && !isDestroyed()) {
                                if (tvResultado != null) {
                                    tvResultado.setText("✗ Ticket denegado: " + finalNombreEstudiante);
                                    tvResultado.setTextColor(getResources().getColor(R.color.error, null));
                                }
                                Toast.makeText(CameraActivity.this, "✗ Ticket denegado para " + finalNombreEstudiante,
                                        Toast.LENGTH_SHORT).show();

                                // Esperar 2 segundos antes de permitir escanear de nuevo
                                new android.os.Handler().postDelayed(() -> {
                                    if (!isFinishing() && !isDestroyed()) {
                                        tvResultado.setText("Listo para escanear");
                                        tvResultado.setTextColor(getResources().getColor(R.color.text_primary, null));
                                        lastScannedQR = "";
                                        isProcessing = false;
                                        isScanning = true;
                                    }
                                }, 2000);
                            }
                        });
                    } else {
                        // Leer mensaje de error
                        BufferedReader errorReader = new BufferedReader(
                                new InputStreamReader(connUpdate.getErrorStream()));
                        StringBuilder errorResponse = new StringBuilder();
                        String errorLine;
                        while ((errorLine = errorReader.readLine()) != null) {
                            errorResponse.append(errorLine);
                        }
                        errorReader.close();

                        String errorMessage = errorResponse.toString();

                        runOnUiThread(() -> {
                            if (!isFinishing() && !isDestroyed()) {
                                if (tvResultado != null) {
                                    tvResultado.setText("Error al denegar ticket");
                                }
                                Toast.makeText(CameraActivity.this, "Error al actualizar ticket: " + errorMessage,
                                        Toast.LENGTH_SHORT).show();

                                // Esperar 3 segundos antes de permitir escanear de nuevo
                                new android.os.Handler().postDelayed(() -> {
                                    if (!isFinishing() && !isDestroyed()) {
                                        tvResultado.setText("Listo para escanear");
                                        lastScannedQR = "";
                                        isProcessing = false;
                                        isScanning = true;
                                    }
                                }, RESCAN_DELAY_MS);
                            }
                        });
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    if (!isFinishing() && !isDestroyed()) {
                        if (tvResultado != null) {
                            tvResultado.setText("Error al validar ticket");
                        }
                        Toast.makeText(CameraActivity.this, "Error de conexión: " + e.getMessage(), Toast.LENGTH_SHORT)
                                .show();

                        // IMPORTANTE: Resetear los flags inmediatamente para evitar que la app se quede
                        // colgada
                        lastScannedQR = "";
                        isProcessing = false;
                        isScanning = true;

                        // Esperar 3 segundos antes de limpiar el mensaje
                        new android.os.Handler().postDelayed(() -> {
                            if (!isFinishing() && !isDestroyed() && tvResultado != null) {
                                tvResultado.setText("Listo para escanear");
                            }
                        }, RESCAN_DELAY_MS);
                    }
                });
            }
        }).start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
        scanner.close();
    }
}