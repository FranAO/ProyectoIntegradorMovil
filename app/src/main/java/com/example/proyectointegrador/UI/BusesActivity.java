package com.example.proyectointegrador.UI;

import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.proyectointegrador.Adapters.BusAdapter;
import com.example.proyectointegrador.Config.ApiConfig;
import com.example.proyectointegrador.Models.Bus;
import com.example.proyectointegrador.R;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class BusesActivity extends BaseNavigationActivity {

    private RecyclerView recyclerViewBuses;
    private BusAdapter busAdapter;
    private ArrayList<Bus> busList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_buses);

        recyclerViewBuses = findViewById(R.id.recyclerViewBuses);
        recyclerViewBuses.setLayoutManager(new LinearLayoutManager(this));

        busList = new ArrayList<>();
        busAdapter = new BusAdapter(busList);
        recyclerViewBuses.setAdapter(busAdapter);

        setupNavigation();
        cargarBuses();
    }

    private void cargarBuses() {
        new Thread(() -> {
            try {
                // Cargar TODOS los buses (información est ática)
                URL busUrl = new URL(ApiConfig.getApiUrl(BusesActivity.this, "/bus"));
                HttpURLConnection busCon = (HttpURLConnection) busUrl.openConnection();
                busCon.setRequestMethod("GET");
                busCon.setConnectTimeout(10000);
                busCon.setReadTimeout(10000);

                int responseCode = busCon.getResponseCode();

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(busCon.getInputStream()));
                    StringBuilder respuesta = new StringBuilder();
                    String linea;

                    while ((linea = reader.readLine()) != null) {
                        respuesta.append(linea);
                    }
                    reader.close();

                    String jsonResponse = respuesta.toString();
                    JSONArray busesArray = new JSONArray(jsonResponse);

                    busList.clear();

                    // Procesar cada bus
                    for (int i = 0; i < busesArray.length(); i++) {
                        JSONObject busObj = busesArray.getJSONObject(i);

                        Bus bus = new Bus();
                        bus.setId(busObj.optString("id"));
                        bus.setBusCode(busObj.optString("busCode"));
                        bus.setPlate(busObj.optString("plate"));
                        bus.setCapacity(busObj.optInt("capacity"));
                        bus.setStatus(busObj.optString("status"));
                        bus.setDriverId(busObj.optString("driverId"));
                        bus.setRouteId(busObj.optString("routeId")); // Ruta ESTÁTICA asignada al bus

                        // Obtener pasajeros actuales del trip activo si existe
                        String tripId = busObj.optString("tripId", "");
                        int currentPassengers = 0;

                        if (tripId != null && !tripId.isEmpty()) {
                            // Si hay un tripId, intentar obtener los pasajeros ocupados
                            try {
                                URL tripUrl = new URL(ApiConfig.getApiUrl(BusesActivity.this, "/trip/" + tripId));
                                HttpURLConnection tripConn = (HttpURLConnection) tripUrl.openConnection();
                                tripConn.setRequestMethod("GET");
                                tripConn.setConnectTimeout(5000);
                                tripConn.setReadTimeout(5000);

                                if (tripConn.getResponseCode() == 200) {
                                    BufferedReader tripReader = new BufferedReader(
                                            new InputStreamReader(tripConn.getInputStream()));
                                    StringBuilder tripResponse = new StringBuilder();
                                    String tripLine;
                                    while ((tripLine = tripReader.readLine()) != null) {
                                        tripResponse.append(tripLine);
                                    }
                                    tripReader.close();

                                    JSONObject tripObj = new JSONObject(tripResponse.toString());
                                    currentPassengers = tripObj.optInt("occupiedSeats", 0);
                                }
                                tripConn.disconnect();
                            } catch (Exception e) {
                                // Si falla, dejarlo en 0
                                e.printStackTrace();
                            }
                        }

                        bus.setCurrentPassengers(currentPassengers);

                        busList.add(bus);
                    }

                    runOnUiThread(() -> {
                        busAdapter.notifyDataSetChanged();
                        if (busList.isEmpty()) {
                            Toast.makeText(this, "No hay buses registrados", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "Cargados " + busList.size() + " buses", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    int finalResponseCode = responseCode;
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Error HTTP: " + finalResponseCode, Toast.LENGTH_SHORT).show();
                    });
                }
            } catch (java.net.UnknownHostException e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    Toast.makeText(this, "No se puede conectar al servidor. Verifica que esté corriendo.",
                            Toast.LENGTH_LONG).show();
                });
            } catch (java.net.SocketTimeoutException e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    Toast.makeText(this, "Tiempo de espera agotado. El servidor no responde.", Toast.LENGTH_LONG)
                            .show();
                });
            } catch (java.io.IOException e) {
                e.printStackTrace();
                String errorMsg = e.getMessage() != null ? e.getMessage() : "Error de conexión";
                runOnUiThread(() -> {
                    Toast.makeText(this, "Error de red: " + errorMsg, Toast.LENGTH_LONG).show();
                });
            } catch (Exception e) {
                e.printStackTrace();
                String errorMsg = e.getClass().getSimpleName() + ": " +
                        (e.getMessage() != null ? e.getMessage() : "Error desconocido");
                runOnUiThread(() -> {
                    Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    @Override
    protected int getNavigationIndex() {
        return 1;
    }
}