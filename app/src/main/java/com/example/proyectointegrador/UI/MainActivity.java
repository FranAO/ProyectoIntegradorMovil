package com.example.proyectointegrador.UI;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.cardview.widget.CardView;
import androidx.core.app.NotificationCompat;

import com.example.proyectointegrador.Config.ApiConfig;
import com.google.android.material.button.MaterialButton;
import com.example.proyectointegrador.Database.DBHelper;
import com.example.proyectointegrador.Models.Student;
import com.example.proyectointegrador.R;
import com.example.proyectointegrador.Services.SignalRService;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends BaseNavigationActivity implements SignalRService.SignalRListener {
    private CardView settingsButton, cardBuyTicket, cardHistory;
    private TextView tvUserName;
    private MaterialButton btnVerViaje;
    private SignalRService signalRService;
    private String currentTripId = null;
    private String currentDriverId = null;
    private String studentId = null;
    private String currentTicketId = null;
    private boolean tripStartedByDriver = false;
    private static final String CHANNEL_ID = "trip_notifications";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        settingsButton = findViewById(R.id.SettingsCard);
        cardBuyTicket = findViewById(R.id.cardBuyTicket);
        cardHistory = findViewById(R.id.cardHistory);
        tvUserName = findViewById(R.id.tvUserName);
        btnVerViaje = findViewById(R.id.btnVerViaje);
        
        SharedPreferences prefs = getSharedPreferences("MiAppPrefs", MODE_PRIVATE);
        String userEmail = prefs.getString("LOGGED_IN_USER_EMAIL", "Usuario");
        
        DBHelper dbHelper = new DBHelper(this);
        Student student = dbHelper.obtenerStudentPorEmail(userEmail);
        
        if (student != null) {
            String firstName = student.getFirstName() != null ? student.getFirstName().trim() : "";
            String lastName = student.getLastName() != null ? student.getLastName().trim() : "";
            
            String fullName = firstName + " " + lastName;
            fullName = fullName.trim();
            
            if (fullName.isEmpty()) {
                tvUserName.setText("Usuario");
            } else {
                tvUserName.setText(fullName);
            }
        } else {
            tvUserName.setText("Usuario");
        }
        
        setupNavigation();
        
        // Obtener studentId
        if (student != null) {
            studentId = student.getId();
        }
        
        // Crear canal de notificaciones
        createNotificationChannel();
        
        // Inicializar SignalR
        signalRService = new SignalRService(this, this);
        signalRService.connect();

        settingsButton.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, SettingsActivity.class));
        });
        
        cardBuyTicket.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, PackagesActivity.class);
            startActivity(intent);
        });
        
        cardHistory.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, HistoryActivity.class);
            startActivity(intent);
        });
        
//        btnViewTicket.setOnClickListener(v -> {
//            if (currentTicketId != null && !currentTicketId.isEmpty()) {
//                Intent intent = new Intent(MainActivity.this, TicketDetailActivity.class);
//                intent.putExtra("TICKET_ID", currentTicketId);
//                startActivity(intent);
//            } else {
//                Toast.makeText(this, "No hay ticket disponible", Toast.LENGTH_SHORT).show();
//            }
//        });
        

        btnVerViaje.setOnClickListener(v -> {
            // Re-obtener prefs para tener el valor más actualizado
            SharedPreferences currentPrefs = getSharedPreferences("MiAppPrefs", MODE_PRIVATE);
            String tripId = currentPrefs.getString("CURRENT_TRIP_ID", "");
            
            // LOGS DE DIAGNÓSTICO
            
            if (!tripId.isEmpty()) {
                Intent intent = new Intent(MainActivity.this, RouteActivity.class);
                intent.putExtra("TRIP_ID", tripId);
                intent.putExtra("TICKET_ID", currentTicketId);
                startActivity(intent);
            } else {
                Toast.makeText(this, "No hay viajes activos en este momento", Toast.LENGTH_SHORT).show();
            }
        });
        
        // NO llamar verificarViajeActivo() aquí - onResume() lo hará automáticamente
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        verificarViajeActivo();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (signalRService != null) {
            signalRService.disconnect();
        }
    }
    
    @Override
    protected void onStart() {
        super.onStart();
        // SignalR se encarga de mantener la conexión activa
    }
    
    @Override
    protected void onStop() {
        super.onStop();
        // SignalR maneja la desconexión automáticamente
    }
    
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Notificaciones de Viaje";
            String description = "Notificaciones sobre el estado de tus viajes";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void verificarViajeActivo() {
        SharedPreferences prefs = getSharedPreferences("MiAppPrefs", MODE_PRIVATE);
        String userEmail = prefs.getString("LOGGED_IN_USER_EMAIL", "");
        String currentTripIdBefore = prefs.getString("CURRENT_TRIP_ID", "");
        
        
        if (userEmail.isEmpty()) {
            return;
        }
        
        new Thread(() -> {
            try {
                String url = ApiConfig.getApiUrl(MainActivity.this, "/student/" + userEmail + "/active-trip");
                
                HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                conn.setRequestMethod("GET");
                
                int responseCode = conn.getResponseCode();
                
                if (responseCode == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();
                    
                    
                    JSONObject json = new JSONObject(response.toString());
                    boolean hasActiveTrip = json.getBoolean("hasActiveTrip");
                    
                    
                    if (hasActiveTrip) {
                        String tripId = json.getString("tripId");
                        
                        // Guardar para usar en botón
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putString("CURRENT_TRIP_ID", tripId);
                        editor.apply();
                        
                    } else {
                        // Limpiar si no hay viaje activo
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.remove("CURRENT_TRIP_ID");
                        editor.apply();
                        
                    }
                } else {
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    @Override
    public void onBusLocationReceived(String busId, double latitude, double longitude, String status) {
        // Aquí puedes actualizar la ubicación en el mapa si está visible
        runOnUiThread(() -> {
            // Log para debug
        });
    }

    @Override
    public void onTripStarted(String tripId, String busId, String routeId) {
        // Verificar si este viaje es para el estudiante actual
        if (currentTripId != null && currentTripId.equals(tripId)) {
            runOnUiThread(() -> {
                tripStartedByDriver = true;
                Toast.makeText(this, "¡Tu viaje ha comenzado! 🚌", Toast.LENGTH_LONG).show();
                showNotification("Viaje Iniciado", "Tu bus ha comenzado el recorrido");
            });
        }
    }

    @Override
    public void onTripEnded(String tripId) {
        // Verificar si este viaje es para el estudiante actual
        if (currentTripId != null && currentTripId.equals(tripId)) {
            runOnUiThread(() -> {
                Toast.makeText(this, "Tu viaje ha finalizado", Toast.LENGTH_LONG).show();
                showNotification("Viaje Finalizado", "Has llegado a tu destino");
                
                // Recargar el próximo viaje después de que termina uno
                SharedPreferences prefs = getSharedPreferences("MiAppPrefs", MODE_PRIVATE);
                String userEmail = prefs.getString("LOGGED_IN_USER_EMAIL", "");
                //if (!userEmail.isEmpty()) {
                  //  cargarProximoViaje(userEmail);
                //}
                
                // Mostrar diálogo de calificación
                if (currentDriverId != null && studentId != null) {
                    showRatingDialog();
                }
                
                // Limpiar datos del viaje actual
                currentTripId = null;
                currentDriverId = null;
                tripStartedByDriver = false;
            });
        }
    }

    @Override
    public void onTicketScanned(String ticketId, String tripId, String studentId) {
        // Este método es llamado cuando cualquier ticket es escaneado
        // La lógica específica se maneja en RouteActivity
    }
    
    /**
     * Cancela tickets que no fueron usados (no escaneados) cuando termina el viaje
     */
    private void cancelarTicketsNoUsados(String tripId) {
        new Thread(() -> {
            try {
                // Obtener todos los tickets del estudiante para este viaje
                String urlStr = ApiConfig.getApiUrl(this, "ticket/my-tickets/" + studentId);
                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                
                if (conn.getResponseCode() == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) response.append(line);
                    reader.close();
                    
                    JSONArray tickets = new JSONArray(response.toString());
                    int canceledCount = 0;
                    
                    for (int i = 0; i < tickets.length(); i++) {
                        JSONObject ticket = tickets.getJSONObject(i);
                        String ticketTripId = ticket.optString("TripId", "");
                        String ticketStatus = ticket.optString("Status", "");
                        String ticketId = ticket.optString("Id", ticket.optString("id", ""));
                        
                        // Cancelar tickets confirmados pero no usados de este viaje
                        if (ticketTripId.equals(tripId) && ticketStatus.equalsIgnoreCase("confirmed")) {
                            if (cancelarTicket(ticketId)) {
                                canceledCount++;
                            }
                        }
                    }
                    
                    final int finalCount = canceledCount;
                    if (finalCount > 0) {
                        runOnUiThread(() -> {
                            Toast.makeText(this, finalCount + " ticket(s) no usado(s) cancelado(s)", Toast.LENGTH_SHORT).show();
                        });
                    }
                }
                
            } catch (Exception e) {
            }
        }).start();
    }
    
    /**
     * Cancela un ticket específico
     */
    private boolean cancelarTicket(String ticketId) {
        try {
            String urlStr = ApiConfig.getApiUrl(this, "ticket/" + ticketId + "/cancel");
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("PUT");
            conn.setRequestProperty("Content-Type", "application/json");
            
            int responseCode = conn.getResponseCode();
            conn.disconnect();
            
            return (responseCode == 200 || responseCode == 204);
        } catch (Exception e) {
            return false;
        }
    }
    
    private void showNotification(String title, String message) {
        // Verificar permiso para Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                // Si no hay permiso, solo mostrar Toast
                Toast.makeText(this, title + ": " + message, Toast.LENGTH_LONG).show();
                return;
            }
        }
        
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify((int) System.currentTimeMillis(), builder.build());
    }
    
    private void showRatingDialog() {
        RatingDialog dialog = new RatingDialog(this, currentTripId, currentDriverId, studentId, 
            new RatingDialog.RatingListener() {
                @Override
                public void onRatingSubmitted(float rating, String comment) {
                    enviarCalificacion(currentTripId, currentDriverId, studentId, rating, comment);
                }

                @Override
                public void onRatingSkipped() {
                    Toast.makeText(MainActivity.this, "Calificación omitida", Toast.LENGTH_SHORT).show();
                }
            });
        dialog.show();
    }
    
    private void enviarCalificacion(String tripId, String driverId, String studentId, float rating, String comment) {
        new Thread(() -> {
            try {
                URL url = new URL(ApiConfig.getApiUrl(MainActivity.this, "/driverrating"));
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                JSONObject ratingData = new JSONObject();
                ratingData.put("TripId", tripId);
                ratingData.put("DriverId", driverId);
                ratingData.put("StudentId", studentId);
                ratingData.put("Rating", rating);
                ratingData.put("Comment", comment);
                ratingData.put("CreatedAt", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(new Date()));

                java.io.OutputStream os = conn.getOutputStream();
                os.write(ratingData.toString().getBytes());
                os.flush();
                os.close();

                int responseCode = conn.getResponseCode();

                runOnUiThread(() -> {
                    if (responseCode == 200 || responseCode == 201) {
                        Toast.makeText(this, "¡Gracias por tu calificación!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Error al enviar calificación", Toast.LENGTH_SHORT).show();
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    Toast.makeText(this, "Error de conexión", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }
    
    /**
     * Método para mostrar la notificación (via SignalR)
     */
    private void lanzarNotificacion(String titulo, String mensaje) {
        // Verificar permiso para Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                // Si no hay permiso, solo mostrar Toast
                runOnUiThread(() -> Toast.makeText(this, titulo + ": " + mensaje, Toast.LENGTH_LONG).show());
                return;
            }
        }
        
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        String channelId = "canal_estado_viaje";
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                channelId, 
                "Alertas de Viaje", 
                NotificationManager.IMPORTANCE_HIGH
            );
            manager.createNotificationChannel(channel);
        }
        
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(titulo)
                .setContentText(mensaje)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);
        
        manager.notify(1, builder.build());
        
        // También mostrar Toast
        runOnUiThread(() -> Toast.makeText(this, titulo, Toast.LENGTH_LONG).show());
    }

    @Override
    protected int getNavigationIndex() {
        return 0;
    }
}