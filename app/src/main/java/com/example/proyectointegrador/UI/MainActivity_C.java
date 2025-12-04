package com.example.proyectointegrador.UI;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.cardview.widget.CardView;

import com.example.proyectointegrador.Config.ApiConfig;
import com.example.proyectointegrador.R;
import com.example.proyectointegrador.Services.RouteSimulationService;
import com.google.android.material.button.MaterialButton;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity_C extends BaseNavigationActivity_C {
    CardView settingsButton;
    MaterialButton startTripButton;
    TextView tvNombreChofer, tvCodigoVehiculo, tvPlacaVehiculo, tvAsientosDispo, tvCalificacion;

    private SharedPreferences prefs;
    private static final String PREFS_NAME = "MiAppPrefs";
    private static final String LOGGED_IN_USER_EMAIL = "LOGGED_IN_USER_EMAIL";

    private String driverId;
    private String assignedBusId;
    private String assignedRouteId;
    private int busCapacity = 0;
    
    // Simulación
    private RouteSimulationService simulationService;
    private String currentTripId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main_c);

        settingsButton = findViewById(R.id.settingsCard);
        startTripButton = findViewById(R.id.startTripButton);
        tvNombreChofer = findViewById(R.id.tvNombreChofer);
        tvCodigoVehiculo = findViewById(R.id.tvCodigoVehiculo);
        tvPlacaVehiculo = findViewById(R.id.tvPlacaVehiculo);
        tvAsientosDispo = findViewById(R.id.tvAsientosDispo);
        tvCalificacion = findViewById(R.id.tvCalificacion);

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        
        // Inicializar servicio de simulación (Singleton)
        String baseUrl = ApiConfig.getApiUrl(this, "");
        simulationService = RouteSimulationService.getInstance(this, baseUrl);

        setupNavigation();
        cargarDatosDriver();

        settingsButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity_C.this, SettingsActivity_C.class);
            startActivity(intent);
        });

        startTripButton.setOnClickListener(v -> {
            android.util.Log.d("MainActivity_C", "🔘 Botón presionado. Texto actual: " + startTripButton.getText().toString());
            // Si el texto es "VIAJE EN CURSO", abrir RouteActivity
            if (startTripButton.getText().toString().equals("VIAJE EN CURSO")) {
                android.util.Log.d("MainActivity_C", "📱 Abriendo RouteActivity");
                Intent intent = new Intent(MainActivity_C.this, RouteActivity_C.class);
                startActivity(intent);
            } else {
                android.util.Log.d("MainActivity_C", "🚀 Iniciando viaje...");
                iniciarViaje();
            }
        });
    }

    private void cargarDatosDriver() {
        String email = prefs.getString(LOGGED_IN_USER_EMAIL, null);
        if (email == null) return;

        new Thread(() -> {
            try {
                // --- CORRECCIÓN: USAR ApiConfig ---
                String urlStr = ApiConfig.getApiUrl(this, "driver/email/" + email);
                URL url = new URL(urlStr);

                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("GET");

                if (con.getResponseCode() == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()));
                    StringBuilder respuesta = new StringBuilder();
                    String linea;
                    while ((linea = reader.readLine()) != null) respuesta.append(linea);

                    android.util.Log.d("MainActivity_C", "📥 Respuesta driver: " + respuesta.toString());
                    
                    JSONObject driver = new JSONObject(respuesta.toString());

                    driverId = driver.has("id") ? driver.getString("id") : driver.getString("Id");
                    assignedBusId = driver.has("assignedBusId") ? driver.getString("assignedBusId") : driver.optString("AssignedBusId");
                    
                    android.util.Log.d("MainActivity_C", "🚗 assignedBusId obtenido: " + assignedBusId);

                    String firstName = driver.optString("firstName");
                    String lastName = driver.optString("lastName");
                    double rating = driver.optDouble("rating", 0.0);

                    runOnUiThread(() -> {
                        tvNombreChofer.setText(firstName + " " + lastName);
                        tvCalificacion.setText(String.format("%.1f", rating));
                    });

                    if (assignedBusId != null && !assignedBusId.isEmpty() && !assignedBusId.equals("null")) {
                        cargarDatosBus(assignedBusId);
                    }
                    
                    // Habilitar botón solo si todos los datos están cargados
                    runOnUiThread(() -> {
                        if (driverId != null && assignedBusId != null && assignedRouteId != null) {
                            startTripButton.setEnabled(true);
                        }
                    });
                    
                    verificarViajeActivo();
                } else {
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void cargarDatosBus(String busId) {
        new Thread(() -> {
            try {
                // --- CORRECCIÓN: USAR ApiConfig ---
                String urlStr = ApiConfig.getApiUrl(this, "bus/" + busId);
                URL url = new URL(urlStr);

                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("GET");

                if (con.getResponseCode() == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()));
                    StringBuilder respuesta = new StringBuilder();
                    String linea;
                    while ((linea = reader.readLine()) != null) respuesta.append(linea);

                    android.util.Log.d("MainActivity_C", "📥 Respuesta bus: " + respuesta.toString());
                    
                    JSONObject bus = new JSONObject(respuesta.toString());

                    assignedRouteId = bus.has("routeId") ? bus.getString("routeId") : bus.optString("RouteId");
                    
                    android.util.Log.d("MainActivity_C", "🛣️ assignedRouteId obtenido: " + assignedRouteId);
                    busCapacity = bus.optInt("capacity", 30);

                    String busCode = bus.optString("busCode");
                    String plate = bus.optString("plate");

                    runOnUiThread(() -> {
                        tvCodigoVehiculo.setText(busCode);
                        tvPlacaVehiculo.setText(plate);
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void iniciarViaje() {
        android.util.Log.d("MainActivity_C", "📊 Datos: driverId=" + driverId + ", busId=" + assignedBusId + ", routeId=" + assignedRouteId);
        
        if (driverId == null || assignedBusId == null || assignedRouteId == null) {
            android.util.Log.e("MainActivity_C", "❌ Faltan datos para iniciar viaje");
            Toast.makeText(this, "Cargando datos... espera un momento", Toast.LENGTH_SHORT).show();
            // Intentar recargar por si acaso falló la red antes
            cargarDatosDriver();
            return;
        }

        android.util.Log.d("MainActivity_C", "✅ Datos completos, iniciando petición...");
        new Thread(() -> {
            try {
                String token = prefs.getString("AUTH_TOKEN", "");
                android.util.Log.d("MainActivity_C", "🔑 Token: " + (token.isEmpty() ? "VACÍO" : "Presente"));

                // --- CORRECCIÓN: USAR ApiConfig ---
                String urlStr = ApiConfig.getApiUrl(this, "trip/start");
                URL url = new URL(urlStr);

                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Authorization", "Bearer " + token);
                conn.setDoOutput(true);

                JSONObject tripData = new JSONObject();
                tripData.put("BusId", assignedBusId);
                tripData.put("DriverId", driverId);
                tripData.put("RouteId", assignedRouteId);
                tripData.put("TotalSeats", busCapacity);

                OutputStream os = conn.getOutputStream();
                os.write(tripData.toString().getBytes("UTF-8"));
                os.close();

                int responseCode = conn.getResponseCode();

                if (responseCode == 200 || responseCode == 201) {
                    // Leer la respuesta para obtener el Trip ID
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) response.append(line);
                    reader.close();
                    
                    JSONObject tripResponse = new JSONObject(response.toString());
                    currentTripId = tripResponse.has("id") ? tripResponse.getString("id") : tripResponse.optString("Id");
                    
                    // NUEVO: Activar el trip para que comience la simulación
                    String activateUrlStr = ApiConfig.getApiUrl(MainActivity_C.this, "trip/activate/" + currentTripId);
                    URL activateUrl = new URL(activateUrlStr);
                    HttpURLConnection activateConn = (HttpURLConnection) activateUrl.openConnection();
                    activateConn.setRequestMethod("PUT");
                    activateConn.setRequestProperty("Authorization", "Bearer " + token);
                    
                    int activateResponseCode = activateConn.getResponseCode();
                    
                    if (activateResponseCode == 200 || activateResponseCode == 204) {
                        runOnUiThread(() -> {
                            Toast.makeText(this, "¡Viaje Iniciado!", Toast.LENGTH_LONG).show();
                            startTripButton.setText("VIAJE EN CURSO");
                            startTripButton.setEnabled(true); // Mantener habilitado para abrir RouteActivity
                            
                            cargarTripActivo(driverId);
                            
                            // Redirigir a RouteActivity_C después de iniciar el viaje
                            Intent intent = new Intent(MainActivity_C.this, RouteActivity_C.class);
                            intent.putExtra("TRIP_ID", currentTripId);
                            startActivity(intent);
                        });
                    } else {
                        runOnUiThread(() -> {
                            Toast.makeText(this, "Error al activar el viaje", Toast.LENGTH_SHORT).show();
                        });
                    }
                } else {
                    runOnUiThread(() -> Toast.makeText(this, "Error: " + responseCode, Toast.LENGTH_SHORT).show());
                }

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }
    
    /**
     * Inicia la simulación de la ruta
     */
    private void iniciarSimulacionRuta(String tripId) {
        new Thread(() -> {
            try {
                // Obtener detalles del viaje para la geometría de la ruta
                String urlStr = ApiConfig.getApiUrl(this, "trip/active-detail/driver/" + driverId);
                URL url = new URL(urlStr);
                
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                
                int responseCode = conn.getResponseCode();
                
                if (responseCode == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) response.append(line);
                    reader.close();
                    
                    String responseStr = response.toString();
                    
                    JSONObject tripDetail = new JSONObject(responseStr);
                    String routeGeometry = tripDetail.has("routeGeometry") ? 
                        tripDetail.getString("routeGeometry") : 
                        tripDetail.optString("RouteGeometry", "");
                    
                    if (routeGeometry != null && !routeGeometry.isEmpty() && !routeGeometry.equals("null")) {
                        String finalGeometry = routeGeometry;
                        // Iniciar la simulación
                        runOnUiThread(() -> {
                            simulationService.startSimulation(finalGeometry, tripId, assignedBusId, assignedRouteId);
                        });
                    } else {
                        runOnUiThread(() -> Toast.makeText(this, "Error: No hay geometría de ruta", Toast.LENGTH_LONG).show());
                    }
                } else {
                    runOnUiThread(() -> Toast.makeText(this, "Error código: " + responseCode, Toast.LENGTH_SHORT).show());
                }
                
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void verificarViajeActivo() {
        if (driverId == null) return;
        cargarTripActivo(driverId);
    }

    private void cargarTripActivo(String dId) {
        new Thread(() -> {
            try {
                // --- CORRECCIÓN: USAR ApiConfig ---
                String urlStr = ApiConfig.getApiUrl(this, "trip/active-by-driver/" + dId);
                URL url = new URL(urlStr);

                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("GET");

                if (con.getResponseCode() == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()));
                    StringBuilder respuesta = new StringBuilder();
                    String linea;
                    while ((linea = reader.readLine()) != null) respuesta.append(linea);

                    JSONObject trip = new JSONObject(respuesta.toString());
                    currentTripId = trip.has("id") ? trip.getString("id") : trip.optString("Id");
                    int occupied = trip.optInt("occupiedSeats", 0);
                    int total = trip.optInt("totalSeats", 30);

                    runOnUiThread(() -> {
                        tvAsientosDispo.setText(occupied + " / " + total);
                        startTripButton.setText("VIAJE EN CURSO");
                        startTripButton.setEnabled(true); // Habilitado para abrir RouteActivity
                    });
                    
                    // La simulación se maneja automáticamente en el backend por BusSimulationService
                    // cuando el trip está en status="active"
                    
                    // Cargar pasajeros del viaje
                    cargarPasajerosViaje(currentTripId);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    /**
     * Carga los pasajeros que compraron tickets para el viaje actual
     */
    private void cargarPasajerosViaje(String tripId) {
        if (tripId == null || tripId.isEmpty()) {
            return;
        }
        
        new Thread(() -> {
            try {
                String urlStr = ApiConfig.getApiUrl(this, "trip/" + tripId);
                URL url = new URL(urlStr);

                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("GET");

                if (con.getResponseCode() == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()));
                    StringBuilder respuesta = new StringBuilder();
                    String linea;
                    while ((linea = reader.readLine()) != null) respuesta.append(linea);
                    reader.close();

                    JSONObject trip = new JSONObject(respuesta.toString());
                    
                    // Los tickets contienen la información de los pasajeros
                    if (trip.has("ticketIds")) {
                        org.json.JSONArray ticketIds = trip.getJSONArray("ticketIds");
                        
                        // Cargar detalles de cada ticket/pasajero
                        for (int i = 0; i < ticketIds.length(); i++) {
                            String ticketId = ticketIds.getString(i);
                            cargarDetalleTicket(ticketId);
                        }
                    } else {
                    }
                } else {
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
    
    /**
     * Carga los detalles de un ticket específico
     */
    private void cargarDetalleTicket(String ticketId) {
        new Thread(() -> {
            try {
                String urlStr = ApiConfig.getApiUrl(this, "ticket/" + ticketId);
                URL url = new URL(urlStr);

                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("GET");

                if (con.getResponseCode() == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()));
                    StringBuilder respuesta = new StringBuilder();
                    String linea;
                    while ((linea = reader.readLine()) != null) respuesta.append(linea);
                    reader.close();

                    JSONObject ticket = new JSONObject(respuesta.toString());
                    String studentId = ticket.optString("studentId");
                    
                    // Cargar información del estudiante
                    cargarDetalleEstudiante(studentId);
                } else {
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
    
    /**
     * Carga los detalles de un estudiante
     */
    private void cargarDetalleEstudiante(String studentId) {
        new Thread(() -> {
            try {
                String urlStr = ApiConfig.getApiUrl(this, "student/" + studentId);
                URL url = new URL(urlStr);

                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("GET");

                if (con.getResponseCode() == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()));
                    StringBuilder respuesta = new StringBuilder();
                    String linea;
                    while ((linea = reader.readLine()) != null) respuesta.append(linea);
                    reader.close();

                    JSONObject student = new JSONObject(respuesta.toString());
                    String firstName = student.optString("firstName", "");
                    String lastName = student.optString("lastName", "");
                    String email = student.optString("email", "");
                    
                    
                    // Aquí puedes actualizar la UI si tienes un RecyclerView
                    // Por ahora solo mostramos en el log
                } else {
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    @Override
    protected int getNavigationIndex() {
        return 0;
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Detener simulación si está activa
        if (simulationService != null && simulationService.isSimulating()) {
            simulationService.stopSimulation();
        }
    }
}