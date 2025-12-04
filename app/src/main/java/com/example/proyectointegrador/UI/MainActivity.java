package com.example.proyectointegrador.UI;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
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
    private CardView settingsButton, mapIconContainer, cardBuyTicket, cardHistory, btnViewTripCard;
    private TextView tvUserName;
    private TextView tvBusNumber, tvTime;
    private TextView tvNextTripTitle, tvOrigin, tvDestination, tvTripDate, tvTripTime;
    private MaterialButton btnViewTicket, btnViewTrip;
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
        tvNextTripTitle = findViewById(R.id.tvNextTripTitle);
        tvOrigin = findViewById(R.id.tvOrigin);
        tvDestination = findViewById(R.id.tvDestination);
        tvTripDate = findViewById(R.id.tvTripDate);
        tvTripTime = findViewById(R.id.tvTripTime);
        btnViewTicket = findViewById(R.id.btnViewTicket);
        btnViewTrip = findViewById(R.id.btnViewTrip);
        btnViewTripCard = findViewById(R.id.btnViewTripCard);
        
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
        cargarProximoViaje(userEmail);
        
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
        
        btnViewTicket.setOnClickListener(v -> {
            if (currentTicketId != null && !currentTicketId.isEmpty()) {
                Intent intent = new Intent(MainActivity.this, TicketDetailActivity.class);
                intent.putExtra("TICKET_ID", currentTicketId);
                startActivity(intent);
            } else {
                Toast.makeText(this, "No hay ticket disponible", Toast.LENGTH_SHORT).show();
            }
        });
        
        btnViewTrip.setOnClickListener(v -> {
            String tripId = prefs.getString("CURRENT_TRIP_ID", "");
            
            if (!tripId.isEmpty()) {
                Intent intent = new Intent(MainActivity.this, RouteActivity.class);
                intent.putExtra("TRIP_ID", tripId);
                intent.putExtra("TICKET_ID", currentTicketId); // Pasar el ticketId actual
                startActivity(intent);
            } else {
                Toast.makeText(this, "No hay viaje activo", Toast.LENGTH_SHORT).show();
            }
        });
        
        // Verificar viaje activo al iniciar
        verificarViajeActivo();
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
        
        android.util.Log.d("MainActivity", "🔍 Verificando viaje activo para: " + userEmail);
        
        if (userEmail.isEmpty()) {
            btnViewTripCard.setVisibility(View.GONE);
            return;
        }
        
        new Thread(() -> {
            try {
                String url = ApiConfig.getApiUrl(MainActivity.this, "/student/" + userEmail + "/active-trip");
                android.util.Log.d("MainActivity", "🌐 URL: " + url);
                
                HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                conn.setRequestMethod("GET");
                
                int responseCode = conn.getResponseCode();
                android.util.Log.d("MainActivity", "📡 Response Code: " + responseCode);
                
                if (responseCode == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();
                    
                    android.util.Log.d("MainActivity", "📥 Respuesta: " + response.toString());
                    
                    JSONObject json = new JSONObject(response.toString());
                    boolean hasActiveTrip = json.getBoolean("hasActiveTrip");
                    
                    android.util.Log.d("MainActivity", "✅ hasActiveTrip: " + hasActiveTrip);
                    
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
                    
                    runOnUiThread(() -> {
                        if (hasActiveTrip) {
                            btnViewTripCard.setVisibility(View.VISIBLE);
                        } else {
                            btnViewTripCard.setVisibility(View.GONE);
                        }
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> btnViewTripCard.setVisibility(View.GONE));
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
                if (!userEmail.isEmpty()) {
                    cargarProximoViaje(userEmail);
                }
                
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



    private void cargarProximoViaje(String userEmail) {
        new Thread(() -> {
            try {
                // Obtener el student ID
                DBHelper dbHelper = new DBHelper(this);
                Student student = dbHelper.obtenerStudentPorEmail(userEmail);
                if (student == null) {
                    runOnUiThread(() -> {
                        tvNextTripTitle.setText("Sin viajes");
                        tvOrigin.setText("-");
                        tvDestination.setText("-");
                        tvTripDate.setText("-");
                        tvTripTime.setText("-");
                        btnViewTicket.setEnabled(false);
                    });
                    return;
                }
                
                String studentIdLocal = student.getId();
                
                // Obtener tickets disponibles del estudiante
                URL ticketUrl = new URL(ApiConfig.getApiUrl(MainActivity.this, "/ticket/my-tickets/" + studentIdLocal));
                HttpURLConnection ticketCon = (HttpURLConnection) ticketUrl.openConnection();
                ticketCon.setRequestMethod("GET");
                ticketCon.setConnectTimeout(10000);
                ticketCon.setReadTimeout(10000);

                if (ticketCon.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(ticketCon.getInputStream()));
                    StringBuilder respuesta = new StringBuilder();
                    String linea;

                    while ((linea = reader.readLine()) != null) {
                        respuesta.append(linea);
                    }
                    reader.close();

                    JSONArray ticketsArray = new JSONArray(respuesta.toString());
                    String tripIdToLoad = null;
                    String ticketIdToLoad = null;

                    // Buscar el primer ticket disponible o confirmado con tripId
                    for (int i = 0; i < ticketsArray.length(); i++) {
                        JSONObject ticketObj = ticketsArray.getJSONObject(i);
                        String status = ticketObj.optString("Status", ticketObj.optString("status", ""));
                        String tripId = ticketObj.optString("TripId", ticketObj.optString("tripId", ""));
                        String ticketId = ticketObj.optString("Id", ticketObj.optString("id", ""));

                        if (("available".equalsIgnoreCase(status) || "confirmed".equalsIgnoreCase(status)) 
                            && tripId != null && !tripId.isEmpty() && !tripId.equals("null")) {
                            tripIdToLoad = tripId;
                            ticketIdToLoad = ticketId;
                            break;
                        }
                    }

                    if (tripIdToLoad != null) {
                        cargarDatosViajeCompleto(tripIdToLoad, ticketIdToLoad);
                    } else {
                        runOnUiThread(() -> {
                            tvNextTripTitle.setText("Sin viajes programados");
                            tvOrigin.setText("-");
                            tvDestination.setText("-");
                            tvTripDate.setText("Compra un ticket");
                            tvTripTime.setText("-");
                            btnViewTicket.setEnabled(false);
                        });
                    }
                } else {
                    runOnUiThread(() -> {
                        tvNextTripTitle.setText("Error al cargar");
                        tvOrigin.setText("-");
                        tvDestination.setText("-");
                        tvTripDate.setText("-");
                        tvTripTime.setText("-");
                        btnViewTicket.setEnabled(false);
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    tvNextTripTitle.setText("Error de conexión");
                    tvOrigin.setText("-");
                    tvDestination.setText("-");
                    tvTripDate.setText("-");
                    tvTripTime.setText("-");
                    btnViewTicket.setEnabled(false);
                });
            }
        }).start();
    }

    private void cargarDatosViajeCompleto(String tripId, String ticketId) {
        new Thread(() -> {
            try {
                // Cargar datos del trip
                URL tripUrl = new URL(ApiConfig.getApiUrl(MainActivity.this, "/trip/" + tripId));
                HttpURLConnection tripCon = (HttpURLConnection) tripUrl.openConnection();
                tripCon.setRequestMethod("GET");
                tripCon.setConnectTimeout(10000);
                tripCon.setReadTimeout(10000);

                if (tripCon.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(tripCon.getInputStream()));
                    StringBuilder respuesta = new StringBuilder();
                    String linea;

                    while ((linea = reader.readLine()) != null) {
                        respuesta.append(linea);
                    }
                    reader.close();

                    JSONObject tripObj = new JSONObject(respuesta.toString());
                    String busId = tripObj.optString("BusId", tripObj.optString("busId", ""));
                    String routeId = tripObj.optString("RouteId", tripObj.optString("routeId", ""));
                    String driverId = tripObj.optString("DriverId", tripObj.optString("driverId", ""));
                    String startTimeStr = tripObj.optString("StartTime", tripObj.optString("startTime", ""));
                    String endTimeStr = tripObj.optString("EndTime", tripObj.optString("endTime", ""));
                    
                    // Guardar tripId, ticketId y driverId para notificaciones y calificaciones
                    currentTripId = tripId;
                    currentTicketId = ticketId;
                    currentDriverId = driverId;

                    // Cargar datos del bus
                    URL busUrl = new URL(ApiConfig.getApiUrl(MainActivity.this, "/bus/" + busId));
                    HttpURLConnection busCon = (HttpURLConnection) busUrl.openConnection();
                    busCon.setRequestMethod("GET");
                    busCon.setConnectTimeout(10000);
                    busCon.setReadTimeout(10000);

                    String busCode = "Bus";
                    if (busCon.getResponseCode() == HttpURLConnection.HTTP_OK) {
                        BufferedReader busReader = new BufferedReader(new InputStreamReader(busCon.getInputStream()));
                        StringBuilder busRespuesta = new StringBuilder();
                        String busLinea;

                        while ((busLinea = busReader.readLine()) != null) {
                            busRespuesta.append(busLinea);
                        }
                        busReader.close();

                        JSONObject busObj = new JSONObject(busRespuesta.toString());
                        busCode = busObj.optString("BusCode", busObj.optString("busCode", "Bus"));
                    }
                    
                    // Cargar datos de la ruta para obtener origen y destino
                    URL routeUrl = new URL(ApiConfig.getApiUrl(MainActivity.this, "/busroute/" + routeId));
                    HttpURLConnection routeCon = (HttpURLConnection) routeUrl.openConnection();
                    routeCon.setRequestMethod("GET");
                    routeCon.setConnectTimeout(10000);
                    routeCon.setReadTimeout(10000);

                    String origin = "Origen";
                    String destination = "Destino";
                    if (routeCon.getResponseCode() == HttpURLConnection.HTTP_OK) {
                        BufferedReader routeReader = new BufferedReader(new InputStreamReader(routeCon.getInputStream()));
                        StringBuilder routeRespuesta = new StringBuilder();
                        String routeLinea;

                        while ((routeLinea = routeReader.readLine()) != null) {
                            routeRespuesta.append(routeLinea);
                        }
                        routeReader.close();

                        JSONObject routeObj = new JSONObject(routeRespuesta.toString());
                        origin = routeObj.optString("RouteName", routeObj.optString("routeName", "Ruta Principal"));
                        
                        // Obtener stops para inicio y fin
                        JSONArray stopsArray = routeObj.optJSONArray("Stops");
                        if (stopsArray == null) {
                            stopsArray = routeObj.optJSONArray("stops");
                        }
                        
                        if (stopsArray != null && stopsArray.length() > 0) {
                            JSONObject firstStop = stopsArray.getJSONObject(0);
                            JSONObject lastStop = stopsArray.getJSONObject(stopsArray.length() - 1);
                            
                            origin = firstStop.optString("Name", firstStop.optString("name", "Parada Inicio"));
                            destination = lastStop.optString("Name", lastStop.optString("name", "Parada Final"));
                        }
                    }

                    // Formatear fecha y hora
                    String fechaFormateada = "";
                    String horaFormateada = "";
                    try {
                        SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
                        Date startTime = isoFormat.parse(startTimeStr);
                        
                        SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM, yyyy", new Locale("es", "ES"));
                        fechaFormateada = dateFormat.format(startTime);
                        
                        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.US);
                        horaFormateada = timeFormat.format(startTime);
                        
                        if (endTimeStr != null && !endTimeStr.isEmpty() && !endTimeStr.equals("null")) {
                            Date endTime = isoFormat.parse(endTimeStr);
                            horaFormateada += " - " + timeFormat.format(endTime);
                        }
                    } catch (Exception e) {
                        fechaFormateada = "Fecha no disponible";
                        horaFormateada = "Hora no disponible";
                    }

                    String finalBusCode = busCode;
                    String finalOrigin = origin;
                    String finalDestination = destination;
                    String finalFecha = fechaFormateada;
                    String finalHora = horaFormateada;

                    runOnUiThread(() -> {
                        tvNextTripTitle.setText(finalBusCode);
                        tvOrigin.setText(finalOrigin);
                        tvDestination.setText(finalDestination);
                        tvTripDate.setText(finalFecha);
                        tvTripTime.setText(finalHora);
                        btnViewTicket.setEnabled(true);
                        
                        // Mantener compatibilidad con el mapIconContainer
                        tvBusNumber.setText(finalBusCode);
                        tvTime.setText(finalHora);
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    tvNextTripTitle.setText("Error");
                    tvOrigin.setText("-");
                    tvDestination.setText("-");
                    tvTripDate.setText("No disponible");
                    tvTripTime.setText("-");
                    btnViewTicket.setEnabled(false);
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