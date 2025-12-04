package com.example.proyectointegrador.UI;

import android.app.DatePickerDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ProgressBar;
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
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class WarningActivity extends BaseNavigationActivity_C {

    private EditText dateEditText, descriptionEditText;
    private Spinner tripSpinner, incidentTypeSpinner;
    private MaterialButton confirmReportButton;
    private ProgressBar progressBar;
    
    private List<Trip> tripList = new ArrayList<>();
    private Trip selectedTrip;
    private String selectedIncidentType;
    private Calendar selectedDate;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_warning);

        setupNavigation();
        initViews();
        setupDatePicker();
        loadDriverTrips();
    }

    @Override
    protected int getNavigationIndex() {
        return 4;
    }

    private void initViews() {
        dateEditText = findViewById(R.id.dateEditText);
        descriptionEditText = findViewById(R.id.descriptionEditText);
        tripSpinner = findViewById(R.id.tripSpinner);
        incidentTypeSpinner = findViewById(R.id.incidentTypeSpinner);
        confirmReportButton = findViewById(R.id.confirmReportButton);
        progressBar = findViewById(R.id.progressBar);

        selectedDate = Calendar.getInstance();
        dateEditText.setText(dateFormat.format(selectedDate.getTime()));

        incidentTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position > 0) {
                    selectedIncidentType = parent.getItemAtPosition(position).toString();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        tripSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position > 0) {
                    selectedTrip = tripList.get(position - 1);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        confirmReportButton.setOnClickListener(v -> submitIncident());
    }

    private void setupDatePicker() {
        dateEditText.setOnClickListener(v -> {
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    selectedDate.set(Calendar.YEAR, year);
                    selectedDate.set(Calendar.MONTH, month);
                    selectedDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    dateEditText.setText(dateFormat.format(selectedDate.getTime()));
                },
                selectedDate.get(Calendar.YEAR),
                selectedDate.get(Calendar.MONTH),
                selectedDate.get(Calendar.DAY_OF_MONTH)
            );
            datePickerDialog.show();
        });
    }

    private void loadDriverTrips() {
        progressBar.setVisibility(View.VISIBLE);
        
        new Thread(() -> {
            try {
                SharedPreferences prefs = getSharedPreferences("MiAppPrefs", MODE_PRIVATE);
                String email = prefs.getString("LOGGED_IN_USER_EMAIL", "");

                if (email.isEmpty()) {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Error: No se encontró el email del usuario", Toast.LENGTH_SHORT).show();
                        progressBar.setVisibility(View.GONE);
                    });
                    return;
                }

                // Obtener datos del chofer
                URL urlDriver = new URL(ApiConfig.getApiUrl(WarningActivity.this, "/driver/email/" + email));
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

                // Obtener viajes del chofer
                URL urlTrips = new URL(ApiConfig.getApiUrl(WarningActivity.this, "/trip"));
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
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());

                for (int i = 0; i < tripsArray.length(); i++) {
                    JSONObject tripJson = tripsArray.getJSONObject(i);
                    
                    String tripDriverId = tripJson.has("DriverId") ? tripJson.getString("DriverId") : tripJson.getString("driverId");
                    
                    if (tripDriverId.equals(driverId)) {
                        String tripId = tripJson.has("Id") ? tripJson.getString("Id") : tripJson.getString("id");
                        String busId = tripJson.has("BusId") ? tripJson.getString("BusId") : tripJson.getString("busId");
                        String routeId = tripJson.has("RouteId") ? tripJson.getString("RouteId") : tripJson.getString("routeId");
                        
                        String startTimeStr = tripJson.has("StartTime") ? tripJson.getString("StartTime") : tripJson.getString("startTime");
                        Date startTime = sdf.parse(startTimeStr);
                        
                        Date endTime = null;
                        if (tripJson.has("EndTime") && !tripJson.isNull("EndTime")) {
                            String endTimeStr = tripJson.getString("EndTime");
                            endTime = sdf.parse(endTimeStr);
                        } else if (tripJson.has("endTime") && !tripJson.isNull("endTime")) {
                            String endTimeStr = tripJson.getString("endTime");
                            endTime = sdf.parse(endTimeStr);
                        }
                        
                        String status = tripJson.has("Status") ? tripJson.getString("Status") : tripJson.getString("status");

                        Trip trip = new Trip(tripId, busId, driverId, routeId, startTime, endTime, status);
                        tripList.add(trip);
                    }
                }

                runOnUiThread(() -> {
                    setupTripSpinner();
                    progressBar.setVisibility(View.GONE);
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    Toast.makeText(this, "Error al cargar viajes: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                });
            }
        }).start();
    }

    private void setupTripSpinner() {
        List<String> tripStrings = new ArrayList<>();
        tripStrings.add("Seleccione un viaje");
        for (Trip trip : tripList) {
            tripStrings.add(trip.toString());
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
            this,
            R.layout.spinner_item,
            tripStrings
        );
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        tripSpinner.setAdapter(adapter);
    }

    private void submitIncident() {
        String description = descriptionEditText.getText().toString().trim();

        if (selectedTrip == null) {
            Toast.makeText(this, "Por favor seleccione un viaje", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedIncidentType == null || selectedIncidentType.equals("Seleccione tipo de incidente")) {
            Toast.makeText(this, "Por favor seleccione el tipo de incidente", Toast.LENGTH_SHORT).show();
            return;
        }

        if (description.isEmpty()) {
            Toast.makeText(this, "Por favor ingrese una descripción", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        confirmReportButton.setEnabled(false);

        new Thread(() -> {
            try {
                SharedPreferences prefs = getSharedPreferences("MiAppPrefs", MODE_PRIVATE);
                String email = prefs.getString("LOGGED_IN_USER_EMAIL", "");

                // Obtener datos del chofer
                URL urlDriver = new URL(ApiConfig.getApiUrl(WarningActivity.this, "/driver/email/" + email));
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

                // Obtener el token de autenticación
                String token = prefs.getString("AUTH_TOKEN", "");

                // Crear el incidente (endpoint específico para drivers)
                URL url = new URL(ApiConfig.getApiUrl(WarningActivity.this, "/incident/report"));
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Authorization", "Bearer " + token);
                conn.setDoOutput(true);

                JSONObject incident = new JSONObject();
                incident.put("TripId", selectedTrip.getId());
                incident.put("DriverId", driverId);
                incident.put("Type", selectedIncidentType);
                incident.put("Description", description);
                incident.put("ReportedAt", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(selectedDate.getTime()));

                OutputStream os = conn.getOutputStream();
                os.write(incident.toString().getBytes());
                os.flush();
                os.close();

                int responseCode = conn.getResponseCode();

                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    confirmReportButton.setEnabled(true);

                    if (responseCode == 201 || responseCode == 200) {
                        Toast.makeText(this, "Incidente reportado exitosamente", Toast.LENGTH_LONG).show();
                        clearForm();
                    } else {
                        Toast.makeText(this, "Error al reportar incidente", Toast.LENGTH_SHORT).show();
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    confirmReportButton.setEnabled(true);
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void clearForm() {
        descriptionEditText.setText("");
        tripSpinner.setSelection(0);
        incidentTypeSpinner.setSelection(0);
        selectedTrip = null;
        selectedIncidentType = null;
        selectedDate = Calendar.getInstance();
        dateEditText.setText(dateFormat.format(selectedDate.getTime()));
    }
}