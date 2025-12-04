package com.example.proyectointegrador.UI;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.proyectointegrador.Adapters.PassengerAdapter;
import com.example.proyectointegrador.Config.ApiConfig;
import com.example.proyectointegrador.Models.Passenger;
import com.example.proyectointegrador.R;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PassengersActivity extends BaseNavigationActivity_C {
    private RecyclerView recyclerViewPasajeros;
    private PassengerAdapter adapter;
    private List<Passenger> listaPasajeros;
    private Map<String, String> ticketStatusMap = new HashMap<>();
    private static final int SCAN_QR_REQUEST = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_passengers);

        setupNavigation();

        recyclerViewPasajeros = findViewById(R.id.recyclerViewPasajeros);
        recyclerViewPasajeros.setLayoutManager(new LinearLayoutManager(this));

        listaPasajeros = new ArrayList<>();
        adapter = new PassengerAdapter(listaPasajeros, this::onScanQRClicked);
        recyclerViewPasajeros.setAdapter(adapter);

        cargarPasajeros();
    }

    @Override
    protected int getNavigationIndex() {
        return 2;
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Recargar pasajeros cada vez que se regresa a esta actividad
        cargarPasajeros();
    }

    private void onScanQRClicked(Passenger passenger) {
        Intent intent = new Intent(this, CameraActivity.class);
        intent.putExtra("PASSENGER_ID", passenger.getId());
        intent.putExtra("PASSENGER_NAME", passenger.getName());
        startActivityForResult(intent, SCAN_QR_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == SCAN_QR_REQUEST && resultCode == RESULT_OK && data != null) {
            boolean ticketConfirmed = data.getBooleanExtra("TICKET_CONFIRMED", false);
            String studentId = data.getStringExtra("STUDENT_ID");
            String studentName = data.getStringExtra("STUDENT_NAME");
            
            if (studentId != null && studentName != null) {
                // Actualizar o agregar pasajero en la lista
                boolean pasajeroExiste = false;
                for (Passenger p : listaPasajeros) {
                    if (p.getId().equals(studentId)) {
                        pasajeroExiste = true;
                        p.setName(studentName);
                        p.setTicketStatus(ticketConfirmed ? "confirmed" : "cancelled");
                        break;
                    }
                }
                
                if (!pasajeroExiste) {
                    // Agregar nuevo pasajero a la lista
                    Passenger newPassenger = new Passenger(studentId, studentName, ticketConfirmed ? "confirmed" : "cancelled");
                    listaPasajeros.add(newPassenger);
                }
                
                adapter.notifyDataSetChanged();
                Toast.makeText(this, ticketConfirmed ? "Pasajero confirmado" : "Ticket denegado", Toast.LENGTH_SHORT).show();
            }
        }
    }



    private void cargarPasajeros() {
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

                URL urlDriver = new URL(ApiConfig.getApiUrl(PassengersActivity.this, "/driver/email/" + email));
                HttpURLConnection connDriver = (HttpURLConnection) urlDriver.openConnection();
                connDriver.setRequestMethod("GET");

                BufferedReader readerDriver = new BufferedReader(new InputStreamReader(connDriver.getInputStream()));
                StringBuilder responseDriver = new StringBuilder();
                String lineDriver;
                while ((lineDriver = readerDriver.readLine()) != null) {
                    responseDriver.append(lineDriver);
                }
                readerDriver.close();

                JSONObject driverJson = new JSONObject(responseDriver.toString());
                String driverId = driverJson.has("Id") ? driverJson.getString("Id") : driverJson.getString("id");

                // Obtener el viaje activo del chofer
                URL urlTrips = new URL(ApiConfig.getApiUrl(PassengersActivity.this, "/trip"));
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
                String currentTripId = null;

                // Buscar viaje scheduled o active del chofer
                for (int i = 0; i < tripsArray.length(); i++) {
                    JSONObject tripJson = tripsArray.getJSONObject(i);
                    String tripDriverId = tripJson.has("DriverId") ? tripJson.getString("DriverId") : tripJson.getString("driverId");
                    String tripStatus = tripJson.has("Status") ? tripJson.getString("Status") : tripJson.getString("status");
                    String tripId = tripJson.has("Id") ? tripJson.getString("Id") : tripJson.getString("id");
                    
                    // Buscar viajes scheduled o active (donde se pueden escanear tickets)
                    if (tripDriverId.equals(driverId) && 
                        (tripStatus.equalsIgnoreCase("scheduled") || tripStatus.equalsIgnoreCase("active"))) {
                        currentTripId = tripId;
                        break;
                    }
                }

                if (currentTripId != null) {
                    // Obtener pasajeros validados del viaje usando PassengerInTrip
                    URL urlPassengers = new URL(ApiConfig.getApiUrl(PassengersActivity.this, "/passengerintrip/" + currentTripId));
                    HttpURLConnection connPassengers = (HttpURLConnection) urlPassengers.openConnection();
                    connPassengers.setRequestMethod("GET");

                    int responseCode = connPassengers.getResponseCode();
                    if (responseCode == 200) {
                        BufferedReader readerPassengers = new BufferedReader(new InputStreamReader(connPassengers.getInputStream()));
                        StringBuilder responsePassengers = new StringBuilder();
                        String linePassenger;
                        while ((linePassenger = readerPassengers.readLine()) != null) {
                            responsePassengers.append(linePassenger);
                        }
                        readerPassengers.close();

                        JSONArray passengersArray = new JSONArray(responsePassengers.toString());
                        
                        if (passengersArray.length() == 0) {
                            listaPasajeros.clear();
                            runOnUiThread(() -> {
                                adapter.notifyDataSetChanged();
                                findViewById(R.id.emptyState).setVisibility(android.view.View.VISIBLE);
                                recyclerViewPasajeros.setVisibility(android.view.View.GONE);
                            });
                            return;
                        }

                        listaPasajeros.clear();

                        for (int j = 0; j < passengersArray.length(); j++) {
                            try {
                                JSONObject passengerJson = passengersArray.getJSONObject(j);
                                String studentId = passengerJson.has("StudentId") ? passengerJson.getString("StudentId") : passengerJson.getString("studentId");
                                
                                android.util.Log.d("PassengersActivity", "Cargando estudiante con ID: " + studentId);
                                
                                // Verificar si studentId es un email (no debería serlo)
                                if (studentId.contains("@")) {
                                    android.util.Log.e("PassengersActivity", "ERROR: StudentId es un email en lugar de un ID: " + studentId);
                                    // Intentar buscar por email como fallback
                                    URL urlStudent = new URL(ApiConfig.getApiUrl(PassengersActivity.this, "/student/email/" + studentId));
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
                                    String realStudentId = studentJson.has("Id") ? studentJson.getString("Id") : studentJson.getString("id");
                                    String firstName = studentJson.has("FirstName") ? studentJson.getString("FirstName") : studentJson.getString("firstName");
                                    String lastName = studentJson.has("LastName") ? studentJson.getString("LastName") : studentJson.getString("lastName");
                                    String fullName = firstName + " " + lastName;
                                    
                                    Passenger passenger = new Passenger(realStudentId, fullName, "confirmed");
                                    listaPasajeros.add(passenger);
                                } else {
                                    // Es un ID válido
                                    URL urlStudent = new URL(ApiConfig.getApiUrl(PassengersActivity.this, "/student/" + studentId));
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
                                    String firstName = studentJson.has("FirstName") ? studentJson.getString("FirstName") : studentJson.getString("firstName");
                                    String lastName = studentJson.has("LastName") ? studentJson.getString("LastName") : studentJson.getString("lastName");
                                    String fullName = firstName + " " + lastName;
                                    
                                    Passenger passenger = new Passenger(studentId, fullName, "confirmed");
                                    listaPasajeros.add(passenger);
                                }
                            } catch (Exception ex) {
                                android.util.Log.e("PassengersActivity", "Error cargando pasajero " + j + ": " + ex.getMessage());
                                ex.printStackTrace();
                                // Continuar con el siguiente pasajero
                            }
                        }

                        runOnUiThread(() -> {
                            adapter.notifyDataSetChanged();
                            findViewById(R.id.emptyState).setVisibility(android.view.View.GONE);
                            recyclerViewPasajeros.setVisibility(android.view.View.VISIBLE);
                        });
                        return;
                    } else {
                        // Error o no hay pasajeros
                        listaPasajeros.clear();
                        runOnUiThread(() -> {
                            adapter.notifyDataSetChanged();
                            findViewById(R.id.emptyState).setVisibility(android.view.View.VISIBLE);
                            recyclerViewPasajeros.setVisibility(android.view.View.GONE);
                        });
                        return;
                    }
                }

                // Si no hay viaje activo
                runOnUiThread(() -> {
                    Toast.makeText(this, "No hay viaje activo en este momento", Toast.LENGTH_SHORT).show();
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    Toast.makeText(this, "Error al cargar pasajeros: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }
}
