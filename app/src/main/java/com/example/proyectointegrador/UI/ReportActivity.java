package com.example.proyectointegrador.UI;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;

import com.example.proyectointegrador.Config.ApiConfig;
import com.example.proyectointegrador.Models.Trip;
import com.example.proyectointegrador.R;
import com.google.android.material.button.MaterialButton;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ReportActivity extends BaseNavigationActivity {

    private EditText descriptionEditText;
    private Spinner tripSpinner;
    private MaterialButton confirmReportButton;
    private List<Trip> tripsList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_report);

        setupNavigation();

        descriptionEditText = findViewById(R.id.descriptionEditText);
        tripSpinner = findViewById(R.id.tripSpinner);
        confirmReportButton = findViewById(R.id.confirmReportButton);

        cargarViajes();

        confirmReportButton.setOnClickListener(v -> enviarQueja());
    }

    @Override
    protected int getNavigationIndex() {
        return 4;
    }

    private void cargarViajes() {
        new Thread(() -> {
            try {
                SharedPreferences prefs = getSharedPreferences("MiAppPrefs", MODE_PRIVATE);
                String email = prefs.getString("LOGGED_IN_USER_EMAIL", "");


                if (email.isEmpty()) {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Error: No se encontró el email del usuario", Toast.LENGTH_SHORT).show();
                    });
                    return;
                }

                // Obtener el studentId del email
                URL urlStudent = new URL(ApiConfig.getApiUrl(ReportActivity.this, "/student/email/" + email));
                HttpURLConnection connStudent = (HttpURLConnection) urlStudent.openConnection();
                connStudent.setRequestMethod("GET");

                int studentResponseCode = connStudent.getResponseCode();

                BufferedReader readerStudent = new BufferedReader(new InputStreamReader(connStudent.getInputStream()));
                StringBuilder responseStudent = new StringBuilder();
                String lineStudent;
                while ((lineStudent = readerStudent.readLine()) != null) {
                    responseStudent.append(lineStudent);
                }
                readerStudent.close();


                JSONObject studentJson = new JSONObject(responseStudent.toString());
                String studentId = studentJson.has("Id") ? studentJson.getString("Id") : studentJson.getString("id");


                // CORRECCIÓN: Los tickets usan el EMAIL del estudiante, no el ID de MongoDB
                // Usar el email directamente en lugar del studentId

                // Obtener tickets del estudiante POR EMAIL
                URL urlTickets = new URL(ApiConfig.getApiUrl(ReportActivity.this, "/ticket/my-tickets/" + email));
                HttpURLConnection connTickets = (HttpURLConnection) urlTickets.openConnection();
                connTickets.setRequestMethod("GET");

                int ticketsResponseCode = connTickets.getResponseCode();

                BufferedReader readerTickets = new BufferedReader(new InputStreamReader(connTickets.getInputStream()));
                StringBuilder responseTickets = new StringBuilder();
                String lineTickets;
                while ((lineTickets = readerTickets.readLine()) != null) {
                    responseTickets.append(lineTickets);
                }
                readerTickets.close();


                JSONArray ticketsArray = new JSONArray(responseTickets.toString());
                List<String> tripIds = new ArrayList<>();


                // Obtener TripIds únicos de tickets usados (escaneados)
                for (int i = 0; i < ticketsArray.length(); i++) {
                    JSONObject ticket = ticketsArray.getJSONObject(i);
                    String ticketId = ticket.has("Id") ? ticket.getString("Id") : ticket.getString("id");
                    String status = ticket.has("Status") ? ticket.getString("Status") : ticket.getString("status");

                    // Solo mostrar viajes de tickets que fueron usados
                    if ("used".equalsIgnoreCase(status)) {
                        // CORRECCIÓN: El campo correcto es UsedInTripId, no TripId
                        String tripId = ticket.has("UsedInTripId") ? ticket.getString("UsedInTripId")
                                : ticket.optString("usedInTripId", null);


                        if (!tripIds.contains(tripId) && tripId != null && !tripId.isEmpty()
                                && !tripId.equals("null")) {
                            tripIds.add(tripId);
                        } else {
                        }
                    } else {
                    }
                }


                // Obtener información de cada viaje
                URL urlTrips = new URL(ApiConfig.getApiUrl(ReportActivity.this, "/trip"));
                HttpURLConnection connTrips = (HttpURLConnection) urlTrips.openConnection();
                connTrips.setRequestMethod("GET");

                BufferedReader readerTrips = new BufferedReader(new InputStreamReader(connTrips.getInputStream()));
                StringBuilder responseTrips = new StringBuilder();
                String lineTrips;
                while ((lineTrips = readerTrips.readLine()) != null) {
                    responseTrips.append(lineTrips);
                }
                readerTrips.close();

                JSONArray tripsArray = new JSONArray(responseTrips.toString());
                tripsList.clear();

                // Obtener información de rutas para mostrar nombres descriptivos
                URL urlRoutes = new URL(ApiConfig.getApiUrl(ReportActivity.this, "/busroute"));
                HttpURLConnection connRoutes = (HttpURLConnection) urlRoutes.openConnection();
                connRoutes.setRequestMethod("GET");

                BufferedReader readerRoutes = new BufferedReader(new InputStreamReader(connRoutes.getInputStream()));
                StringBuilder responseRoutes = new StringBuilder();
                String lineRoutes;
                while ((lineRoutes = readerRoutes.readLine()) != null) {
                    responseRoutes.append(lineRoutes);
                }
                readerRoutes.close();

                JSONArray routesArray = new JSONArray(responseRoutes.toString());

                for (String tripId : tripIds) {
                    for (int i = 0; i < tripsArray.length(); i++) {
                        JSONObject tripJson = tripsArray.getJSONObject(i);
                        String id = tripJson.has("Id") ? tripJson.getString("Id") : tripJson.getString("id");

                        if (id.equals(tripId)) {
                            String status = tripJson.has("Status") ? tripJson.getString("Status")
                                    : tripJson.getString("status");

                            // Solo agregar viajes completados
                            if ("completed".equalsIgnoreCase(status)) {
                                Trip trip = new Trip();
                                trip.id = id;
                                trip.busId = tripJson.has("BusId") ? tripJson.getString("BusId")
                                        : tripJson.getString("busId");
                                trip.driverId = tripJson.has("DriverId") ? tripJson.getString("DriverId")
                                        : tripJson.getString("driverId");
                                trip.routeId = tripJson.has("RouteId") ? tripJson.getString("RouteId")
                                        : tripJson.getString("routeId");
                                trip.status = status;

                                // Buscar el nombre de la ruta
                                for (int j = 0; j < routesArray.length(); j++) {
                                    JSONObject routeJson = routesArray.getJSONObject(j);
                                    String routeIdFromArray = routeJson.has("Id") ? routeJson.getString("Id")
                                            : routeJson.getString("id");
                                    if (routeIdFromArray.equals(trip.routeId)) {
                                        trip.routeName = routeJson.has("RouteName") ? routeJson.getString("RouteName")
                                                : routeJson.getString("routeName");
                                        break;
                                    }
                                }

                                String startTimeStr = tripJson.has("StartTime") ? tripJson.getString("StartTime")
                                        : tripJson.getString("startTime");
                                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss",
                                        Locale.getDefault());
                                trip.startTime = inputFormat.parse(startTimeStr);

                                tripsList.add(trip);
                            }
                            break;
                        }
                    }
                }

                runOnUiThread(() -> {
                    if (tripsList.isEmpty()) {
                        Toast.makeText(this, "No tienes viajes completados para reportar", Toast.LENGTH_LONG).show();
                        // Agregar un elemento dummy para mostrar en el spinner
                        Trip emptyTrip = new Trip();
                        emptyTrip.id = "";
                        emptyTrip.routeName = "No hay viajes completados";
                        tripsList.add(emptyTrip);
                    }

                    ArrayAdapter<Trip> adapter = new ArrayAdapter<>(this, R.layout.spinner_item, tripsList);
                    adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
                    tripSpinner.setAdapter(adapter);
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    Toast.makeText(this, "Error al cargar viajes: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void enviarQueja() {
        String description = descriptionEditText.getText().toString().trim();

        if (tripsList.isEmpty()) {
            Toast.makeText(this, "No hay viajes disponibles", Toast.LENGTH_SHORT).show();
            return;
        }

        if (tripSpinner.getSelectedItem() == null) {
            Toast.makeText(this, "Seleccione un viaje", Toast.LENGTH_SHORT).show();
            return;
        }

        Trip selectedTrip = (Trip) tripSpinner.getSelectedItem();

        // Validar que no sea el elemento dummy de "sin viajes"
        if (selectedTrip.id == null || selectedTrip.id.isEmpty()) {
            Toast.makeText(this, "No tienes viajes completados para reportar", Toast.LENGTH_SHORT).show();
            return;
        }

        if (description.isEmpty()) {
            Toast.makeText(this, "Ingrese una descripción", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(() -> {
            try {
                SharedPreferences prefs = getSharedPreferences("MiAppPrefs", MODE_PRIVATE);
                String email = prefs.getString("LOGGED_IN_USER_EMAIL", "");

                // Obtener el studentId
                URL urlStudent = new URL(ApiConfig.getApiUrl(ReportActivity.this, "/student/email/" + email));
                HttpURLConnection connStudent = (HttpURLConnection) urlStudent.openConnection();
                connStudent.setRequestMethod("GET");

                BufferedReader readerStudent = new BufferedReader(new InputStreamReader(connStudent.getInputStream()));
                StringBuilder responseStudent = new StringBuilder();
                String lineStudent;
                while ((lineStudent = readerStudent.readLine()) != null) {
                    responseStudent.append(lineStudent);
                }
                readerStudent.close();

                JSONObject studentJson = new JSONObject(responseStudent.toString());
                String studentId = studentJson.has("Id") ? studentJson.getString("Id") : studentJson.getString("id");

                // Crear incidente
                URL url = new URL(ApiConfig.getApiUrl(ReportActivity.this, "/incident"));
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");

                // Agregar token de autenticación
                String token = prefs.getString("AUTH_TOKEN", "");
                if (!token.isEmpty()) {
                    conn.setRequestProperty("Authorization", "Bearer " + token);
                } else {
                }

                conn.setDoOutput(true);

                JSONObject incidentData = new JSONObject();
                incidentData.put("TripId", selectedTrip.id);
                incidentData.put("StudentId", email); // USAR EMAIL en lugar de ID de MongoDB
                incidentData.put("Type", "Queja");
                incidentData.put("Description", description);

                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
                incidentData.put("ReportedAt", dateFormat.format(new Date()));


                OutputStream os = conn.getOutputStream();
                os.write(incidentData.toString().getBytes());
                os.flush();
                os.close();

                int responseCode = conn.getResponseCode();

                runOnUiThread(() -> {
                    if (responseCode == 200 || responseCode == 201) {
                        Toast.makeText(this, "Queja enviada exitosamente", Toast.LENGTH_SHORT).show();
                        limpiarFormulario();
                    } else {
                        Toast.makeText(this, "Error al enviar la queja (código " + responseCode + ")",
                                Toast.LENGTH_SHORT).show();
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void limpiarFormulario() {
        descriptionEditText.setText("");
        if (tripSpinner.getAdapter() != null && tripSpinner.getAdapter().getCount() > 0) {
            tripSpinner.setSelection(0);
        }
    }
}