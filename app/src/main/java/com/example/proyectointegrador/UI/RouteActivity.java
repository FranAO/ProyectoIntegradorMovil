package com.example.proyectointegrador.UI;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.proyectointegrador.Config.ApiConfig;
import com.example.proyectointegrador.R;
import com.example.proyectointegrador.Services.SignalRService;
import com.mapbox.bindgen.Value;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.LineString;
import com.mapbox.geojson.Point;
import com.mapbox.maps.CameraOptions;
import com.mapbox.maps.MapView;
import com.mapbox.maps.MapboxMap;
import com.mapbox.maps.extension.style.layers.generated.CircleLayer;
import com.mapbox.maps.extension.style.layers.generated.LineLayer;
import com.mapbox.maps.extension.style.layers.properties.generated.IconAnchor;
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource;
import com.mapbox.maps.plugin.animation.CameraAnimationsUtils;
import com.mapbox.maps.plugin.animation.MapAnimationOptions;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class RouteActivity extends AppCompatActivity implements SignalRService.SignalRListener {
    private MapView mapView;
    private String tripId;
    private String ticketId;
    private android.view.View waitingOverlay;

    // Para rastrear la posición del bus
    private Point currentBusPosition;
    private List<Point> routePoints;
    private boolean tripStartedByDriver = false;
    private boolean ticketWasScanned = false;
    private SignalRService signalRService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_route);

        findViewById(R.id.card_back_button).setOnClickListener(v -> finish());

        mapView = findViewById(R.id.mapView);
        waitingOverlay = findViewById(R.id.waitingOverlay);

        // Inicializar lista para la ruta
        routePoints = new ArrayList<>();

        // Obtener el tripId, ticketId o routeId del intent
        tripId = getIntent().getStringExtra("TRIP_ID");
        ticketId = getIntent().getStringExtra("TICKET_ID");
        String routeId = getIntent().getStringExtra("ROUTE_ID");
        tripStartedByDriver = getIntent().getBooleanExtra("TRIP_STARTED", false);

        // Si hay ROUTE_ID (desde BusesActivity), mostrar ruta estática directamente
        if (routeId != null && !routeId.isEmpty()) {
            cargarRutaEstatica(routeId);
        } else if (tripId != null && !tripId.isEmpty()) {
            // Inicializar SignalR solo si es un trip activo
            signalRService = new SignalRService(this, this);
            signalRService.connect();

            // Verificar estado del ticket antes de mostrar la ruta
            if (ticketId != null && !ticketId.isEmpty()) {
                verificarEstadoTicket();
            } else {
                cargarYMostrarRuta();
            }
        } else {
            Toast.makeText(this, "No hay ruta para mostrar", Toast.LENGTH_SHORT).show();
            mostrarMapaDefault();
        }
    }

    private void mostrarMapaDefault() {
        MapboxMap mapboxMap = mapView.getMapboxMap();
        mapboxMap.loadStyleUri("mapbox://styles/mapbox/streets-v12", style -> {
            mapboxMap.setCamera(
                    new CameraOptions.Builder()
                            .center(Point.fromLngLat(-98.0, 39.5))
                            .zoom(3.0)
                            .build());
        });
    }

    private void cargarRutaEstatica(String routeId) {
        new Thread(() -> {
            try {
                String routeUrlStr = ApiConfig.getApiUrl(this, "busroute/" + routeId);

                URL routeUrl = new URL(routeUrlStr);
                HttpURLConnection routeConn = (HttpURLConnection) routeUrl.openConnection();
                routeConn.setRequestMethod("GET");

                int routeResponseCode = routeConn.getResponseCode();

                if (routeResponseCode == 200) {
                    BufferedReader routeReader = new BufferedReader(new InputStreamReader(routeConn.getInputStream()));
                    StringBuilder routeResponse = new StringBuilder();
                    String routeLine;
                    while ((routeLine = routeReader.readLine()) != null)
                        routeResponse.append(routeLine);
                    routeReader.close();

                    String routeResponseStr = routeResponse.toString();
                    JSONObject routeObj = new JSONObject(routeResponseStr);
                    String routeGeometry = routeObj.optString("routeGeometry");

                    if (routeGeometry != null && !routeGeometry.isEmpty() && !routeGeometry.equals("null")) {
                        runOnUiThread(() -> dibujarRutaEnMapa(routeGeometry));
                    } else {
                        runOnUiThread(() -> {
                            Toast.makeText(this, "No hay geometría de ruta disponible", Toast.LENGTH_SHORT).show();
                            mostrarMapaDefault();
                        });
                    }
                } else {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Error al cargar la ruta", Toast.LENGTH_SHORT).show();
                        mostrarMapaDefault();
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    mostrarMapaDefault();
                });
            }
        }).start();
    }

    private void cargarYMostrarRuta() {
        new Thread(() -> {
            try {

                // Usar el nuevo endpoint que devuelve trip + route geometry en un solo llamado
                String tripDetailUrl = ApiConfig.getApiUrl(this, "trip/active-detail/" + tripId);

                URL url = new URL(tripDetailUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                int responseCode = conn.getResponseCode();

                if (responseCode == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null)
                        response.append(line);
                    reader.close();

                    String tripResponse = response.toString();

                    JSONObject tripObj = new JSONObject(tripResponse);

                    // El nuevo endpoint devuelve RouteGeometry directamente
                    String routeGeometry = tripObj.optString("routeGeometry");

                    if (routeGeometry != null && !routeGeometry.isEmpty() && !routeGeometry.equals("null")) {
                        runOnUiThread(() -> dibujarRutaEnMapa(routeGeometry));
                    } else {
                        runOnUiThread(() -> {
                            Toast.makeText(this, "No hay geometría de ruta disponible", Toast.LENGTH_SHORT).show();
                            mostrarMapaDefault();
                        });
                    }
                } else {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Error al cargar el viaje: código " + responseCode, Toast.LENGTH_SHORT)
                                .show();
                        mostrarMapaDefault();
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    Toast.makeText(this, "Error al cargar la ruta: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    mostrarMapaDefault();
                });
            }
        }).start();
    }

    /**
     * Decodifica una cadena polyline al formato de Google Maps
     * 
     * @param encoded Cadena polyline codificada
     * @return Lista de puntos Point
     */
    private List<Point> decodePolyline(String encoded) {
        List<Point> poly = new ArrayList<>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            // IMPORTANTE: OSRM usa precisión 5 (1E5), NO 6 (1E6)
            Point p = Point.fromLngLat((double) lng / 1E5, (double) lat / 1E5);
            poly.add(p);
        }

        return poly;
    }

    private void dibujarRutaEnMapa(String polylineString) {
        try {

            // Decodificar el polyline
            List<Point> points = decodePolyline(polylineString);

            if (points.isEmpty()) {
                Toast.makeText(this, "No hay puntos en la ruta", Toast.LENGTH_SHORT).show();
                mostrarMapaDefault();
                return;
            }

            // Guardar los puntos de la ruta para la animación
            routePoints.clear();
            routePoints.addAll(points);

            MapboxMap mapboxMap = mapView.getMapboxMap();
            mapboxMap.loadStyleUri("mapbox://styles/mapbox/streets-v12", style -> {

                try {
                    // Crear LineString con los puntos de la ruta
                    LineString lineString = LineString.fromLngLats(points);

                    // Agregar source para la línea de la ruta
                    GeoJsonSource routeSource = new GeoJsonSource.Builder("route-source")
                            .geometry(lineString)
                            .build();
                    routeSource.bindTo(style);

                    // Agregar layer para la línea de la ruta
                    LineLayer lineLayer = new LineLayer("route-layer", "route-source");
                    lineLayer.lineColor("#0080FF");
                    lineLayer.lineWidth(5.0);
                    lineLayer.bindTo(style);

                    // Crear marcador de inicio (azul)
                    Point startPoint = points.get(0);
                    Feature startFeature = Feature.fromGeometry(startPoint);

                    GeoJsonSource startSource = new GeoJsonSource.Builder("start-source")
                            .feature(startFeature)
                            .build();
                    startSource.bindTo(style);

                    style.addImage("start-marker-icon", getBitmapFromVectorDrawable(R.drawable.ic_location_blue));

                    com.mapbox.maps.extension.style.layers.generated.SymbolLayer startLayer = new com.mapbox.maps.extension.style.layers.generated.SymbolLayer(
                            "start-layer", "start-source");
                    startLayer.iconImage("start-marker-icon");
                    startLayer.iconSize(1.5);
                    startLayer.iconAnchor(IconAnchor.BOTTOM);
                    startLayer.bindTo(style);

                    // Crear marcador de fin (rojo)
                    Point endPoint = points.get(points.size() - 1);
                    Feature endFeature = Feature.fromGeometry(endPoint);

                    GeoJsonSource endSource = new GeoJsonSource.Builder("end-source")
                            .feature(endFeature)
                            .build();
                    endSource.bindTo(style);

                    style.addImage("end-marker-icon", getBitmapFromVectorDrawable(R.drawable.ic_location_red));

                    com.mapbox.maps.extension.style.layers.generated.SymbolLayer endLayer = new com.mapbox.maps.extension.style.layers.generated.SymbolLayer(
                            "end-layer", "end-source");
                    endLayer.iconImage("end-marker-icon");
                    endLayer.iconSize(1.5);
                    endLayer.iconAnchor(IconAnchor.BOTTOM);
                    endLayer.bindTo(style);

                    // Crear el marcador del bus
                    Point firstPoint = points.get(0);
                    Feature busFeature = Feature.fromGeometry(firstPoint);

                    GeoJsonSource busSource = new GeoJsonSource.Builder("bus-source")
                            .feature(busFeature)
                            .build();
                    busSource.bindTo(style);

                    // Círculo sombra del bus
                    CircleLayer busShadowLayer = new CircleLayer("bus-shadow-layer", "bus-source");
                    busShadowLayer.circleRadius(18.0);
                    busShadowLayer.circleColor(Color.parseColor("#40000000"));
                    busShadowLayer.circleOpacity(0.3f);
                    busShadowLayer.bindTo(style);

                    // Círculo exterior del bus
                    CircleLayer busOuterLayer = new CircleLayer("bus-outer-layer", "bus-source");
                    busOuterLayer.circleRadius(16.0);
                    busOuterLayer.circleColor(Color.parseColor("#8D0743"));
                    busOuterLayer.bindTo(style);

                    // Anillo blanco
                    CircleLayer busRingLayer = new CircleLayer("bus-ring-layer", "bus-source");
                    busRingLayer.circleRadius(13.0);
                    busRingLayer.circleColor(Color.WHITE);
                    busRingLayer.bindTo(style);

                    // Círculo interior
                    CircleLayer busLayer = new CircleLayer("bus-layer", "bus-source");
                    busLayer.circleRadius(10.0);
                    busLayer.circleColor(Color.parseColor("#B31D5A"));
                    busLayer.bindTo(style);

                    // Centrar cámara
                    mapboxMap.setCamera(
                            new CameraOptions.Builder()
                                    .center(firstPoint)
                                    .zoom(14.0)
                                    .build());

                    // Posición inicial del bus
                    currentBusPosition = firstPoint;

                    runOnUiThread(() -> {
                        Toast.makeText(this, "Ruta cargada correctamente", Toast.LENGTH_SHORT).show();
                    });
                } catch (Exception e) {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Error al dibujar la línea de la ruta", Toast.LENGTH_SHORT).show();
                    });
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error al dibujar la ruta: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            mostrarMapaDefault();
        }
    }

    @Override
    public void onBusLocationReceived(String busId, double latitude, double longitude, String status) {
        android.util.Log.d("RouteActivity",
                String.format("📍 onBusLocationReceived: busId=%s, lat=%f, lng=%f", busId, latitude, longitude));

        runOnUiThread(() -> {
            Point newPosition = Point.fromLngLat(longitude, latitude);

            mapView.getMapboxMap().getStyle(style -> {
                try {
                    String featureCollection = Feature.fromGeometry(newPosition).toJson();
                    style.setStyleSourceProperty("bus-source", "data", Value.valueOf(featureCollection));

                    CameraAnimationsUtils.flyTo(
                            mapView.getMapboxMap(),
                            new CameraOptions.Builder()
                                    .center(newPosition)
                                    .zoom(15.0)
                                    .build(),
                            new MapAnimationOptions.Builder()
                                    .duration(2500)
                                    .build());

                } catch (Exception e) {
                }
            });
        });
    }

    @Override
    public void onTripStarted(String tripId, String busId, String routeId) {
        if (tripId.equals(this.tripId)) {
            runOnUiThread(() -> {
                tripStartedByDriver = true;
                Toast.makeText(this, "¡El viaje ha comenzado!", Toast.LENGTH_LONG).show();
            });
        }
    }

    @Override
    public void onTripEnded(String tripId) {
        if (tripId.equals(this.tripId)) {
            runOnUiThread(() -> {
                Toast.makeText(this, "El viaje ha finalizado", Toast.LENGTH_LONG).show();

                String driverId = getIntent().getStringExtra("DRIVER_ID");
                android.content.SharedPreferences prefs = getSharedPreferences("MiAppPrefs", MODE_PRIVATE);
                String studentEmail = prefs.getString("LOGGED_IN_USER_EMAIL", "");

                new Thread(() -> {
                    try {
                        String urlStr = com.example.proyectointegrador.Config.ApiConfig.getApiUrl(this,
                                "student/email/" + studentEmail);
                        java.net.URL url = new java.net.URL(urlStr);
                        java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                        conn.setRequestMethod("GET");

                        if (conn.getResponseCode() == 200) {
                            java.io.BufferedReader reader = new java.io.BufferedReader(
                                    new java.io.InputStreamReader(conn.getInputStream()));
                            StringBuilder response = new StringBuilder();
                            String line;
                            while ((line = reader.readLine()) != null)
                                response.append(line);
                            reader.close();

                            org.json.JSONObject student = new org.json.JSONObject(response.toString());
                            String studentId = student.has("Id") ? student.getString("Id") : student.getString("id");

                            String finalDriverId = driverId;
                            final String useDriverId = finalDriverId;

                            runOnUiThread(() -> {
                                if (!isFinishing() && !isDestroyed()) {
                                    if (useDriverId != null && !useDriverId.isEmpty()) {
                                        showRatingDialog(tripId, useDriverId, studentId);
                                    }
                                }
                            });
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }).start();
            });
        }

    }

    private void showRatingDialog(String tripId, String driverId, String studentId) {
        RatingDialog dialog = new RatingDialog(this, tripId, driverId, studentId,
                new RatingDialog.RatingListener() {
                    @Override
                    public void onRatingSubmitted(float rating, String comment) {
                        enviarCalificacion(tripId, driverId, studentId, rating, comment);
                    }

                    @Override
                    public void onRatingSkipped() {
                        Toast.makeText(RouteActivity.this, "Calificación omitida", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
        dialog.show();
    }

    private void enviarCalificacion(String tripId, String driverId, String studentId, float rating, String comment) {
        new Thread(() -> {
            try {
                java.net.URL url = new java.net.URL(
                        com.example.proyectointegrador.Config.ApiConfig.getApiUrl(RouteActivity.this, "/driverrating"));
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                org.json.JSONObject ratingData = new org.json.JSONObject();
                ratingData.put("TripId", tripId);
                ratingData.put("DriverId", driverId);
                ratingData.put("StudentId", studentId);
                ratingData.put("Rating", rating);
                ratingData.put("Comment", comment);
                ratingData.put("CreatedAt",
                        new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault())
                                .format(new java.util.Date()));

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
                    finish();
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    Toast.makeText(this, "Error de conexión", Toast.LENGTH_SHORT).show();
                    finish();
                });
            }
        }).start();
    }

    @Override
    public void onTicketScanned(String scannedTicketId, String scannedTripId, String studentId) {
        if (ticketId != null && ticketId.equals(scannedTicketId)) {
            runOnUiThread(() -> {
                ticketWasScanned = true;
                Toast.makeText(this, "¡Tu ticket fue escaneado! Cargando ruta...", Toast.LENGTH_LONG).show();

                waitingOverlay.setVisibility(android.view.View.GONE);
                cargarYMostrarRuta();
            });
        }
    }

    private void verificarEstadoTicket() {
        new Thread(() -> {
            try {
                String url = ApiConfig.getApiUrl(this, "ticket/" + ticketId + "/status");

                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) new java.net.URL(url).openConnection();
                conn.setRequestMethod("GET");

                if (conn.getResponseCode() == 200) {
                    java.io.BufferedReader reader = new java.io.BufferedReader(
                            new java.io.InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    org.json.JSONObject json = new org.json.JSONObject(response.toString());
                    boolean wasScanned = json.getBoolean("wasScanned");

                    runOnUiThread(() -> {
                        if (wasScanned) {
                            ticketWasScanned = true;
                            waitingOverlay.setVisibility(android.view.View.GONE);
                            cargarYMostrarRuta();
                        } else {
                            ticketWasScanned = false;
                            waitingOverlay.setVisibility(android.view.View.VISIBLE);
                        }
                    });
                } else {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Error al verificar estado del ticket", Toast.LENGTH_SHORT).show();
                        finish();
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    Toast.makeText(this, "Error de conexión", Toast.LENGTH_SHORT).show();
                    finish();
                });
            }
        }).start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (signalRService != null) {
            signalRService.disconnect();
        }
    }

    private android.graphics.Bitmap getBitmapFromVectorDrawable(int drawableId) {
        android.graphics.drawable.Drawable drawable = androidx.core.content.ContextCompat.getDrawable(this, drawableId);
        if (drawable instanceof android.graphics.drawable.BitmapDrawable) {
            return ((android.graphics.drawable.BitmapDrawable) drawable).getBitmap();
        }

        android.graphics.Bitmap bitmap = android.graphics.Bitmap.createBitmap(
                drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(),
                android.graphics.Bitmap.Config.ARGB_8888);
        android.graphics.Canvas canvas = new android.graphics.Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }
}