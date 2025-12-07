package com.example.proyectointegrador.UI;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import com.example.proyectointegrador.Config.ApiConfig;
import com.example.proyectointegrador.R;
import com.example.proyectointegrador.Services.RouteSimulationService;
import com.mapbox.bindgen.Value;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.LineString;
import com.mapbox.geojson.Point;
import com.mapbox.maps.CameraOptions;
import com.mapbox.maps.MapView;
import com.mapbox.maps.Style;
import com.mapbox.maps.extension.style.layers.properties.generated.IconAnchor;
import com.mapbox.maps.plugin.animation.CameraAnimationsUtils;
import com.mapbox.maps.plugin.animation.MapAnimationOptions;
import com.microsoft.signalr.HubConnection;
import com.microsoft.signalr.HubConnectionBuilder;
import com.microsoft.signalr.HubConnectionState;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class RouteActivity_C extends BaseNavigationActivity_C {

    private MapView mapView;
    private String driverEmail;
    private String driverId;
    private RouteSimulationService simulationService;
    private String currentTripId;
    private String assignedBusId;
    private String assignedRouteId;

    // Para la animación del bus
    private Point currentBusPosition;
    private Handler animationHandler;
    private java.util.List<Point> routePoints;
    private int currentPointIndex = 0;

    // SignalR para recibir actualizaciones de ubicación en tiempo real
    private HubConnection hubConnection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_route_c);

        setupNavigation();

        SharedPreferences prefs = getSharedPreferences("MiAppPrefs", MODE_PRIVATE);
        driverEmail = prefs.getString("LOGGED_IN_USER_EMAIL", "");

        // Obtener instancia del servicio de simulación (Singleton)
        String baseUrl = ApiConfig.getApiUrl(this, "");
        simulationService = RouteSimulationService.getInstance(this, baseUrl);

        // Inicializar handler para animación
        animationHandler = new Handler(Looper.getMainLooper());
        routePoints = new java.util.ArrayList<>();

        mapView = findViewById(R.id.mapView);

        mapView.getMapboxMap().loadStyleUri(Style.MAPBOX_STREETS, style -> {
            if (!driverEmail.isEmpty()) {
                obtenerDetalleViaje();
                // Inicializar conexión SignalR
                initializeSignalR();
            } else {
                Toast.makeText(this, "Error: No hay sesión de chofer", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Inicializa la conexión SignalR para recibir actualizaciones de ubicación en
     * tiempo real
     */
    private void initializeSignalR() {
        new Thread(() -> {
            try {
                String cleanBaseUrl = ApiConfig.getApiUrl(this, "").replace("/api", "").replace("/api/", "");
                if (cleanBaseUrl.endsWith("/")) {
                    cleanBaseUrl = cleanBaseUrl.substring(0, cleanBaseUrl.length() - 1);
                }
                String hubUrl = cleanBaseUrl + "/bushub";


                hubConnection = HubConnectionBuilder.create(hubUrl)
                        .build();

                // Suscribirse al evento ReceiveBusLocation
                hubConnection.on("ReceiveBusLocation", (busId, latitude, longitude, status) -> {
                    // Solo actualizar si es el bus del viaje actual
                    if (busId.equals(assignedBusId)) {
                        runOnUiThread(() -> {
                            updateBusPosition(latitude, longitude);
                        });
                    }
                }, String.class, Double.class, Double.class, String.class);

                // Ya no necesitamos listener de TripEnded aquí

                // Conectar
                hubConnection.start().blockingAwait();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    /**
     * Actualiza la posición del bus en el mapa
     */
    private void updateBusPosition(double latitude, double longitude) {
        if (mapView == null)
            return;

        Point newPosition = Point.fromLngLat(longitude, latitude);
        currentBusPosition = newPosition;

        mapView.getMapboxMap().getStyle(style -> {
            try {
                // Actualizar posición del marcador del bus
                if (style.styleSourceExists("bus-source")) {
                    String featureCollection = Feature.fromGeometry(newPosition).toJson();
                    style.setStyleSourceProperty("bus-source", "data", Value.valueOf(featureCollection));

                    // Mover la cámara suavemente para seguir al bus
                    CameraAnimationsUtils.flyTo(
                            mapView.getMapboxMap(),
                            new CameraOptions.Builder()
                                    .center(newPosition)
                                    .zoom(15.0)
                                    .build(),
                            new MapAnimationOptions.Builder()
                                    .duration(2000) // Animación suave de 2 segundos
                                    .build());
                }
            } catch (Exception e) {
            }
        });
    }

    @Override
    protected int getNavigationIndex() {
        return 1;
    }

    private void obtenerDetalleViaje() {
        new Thread(() -> {
            try {
                String driverId = obtenerDriverId(driverEmail);
                if (driverId == null) {
                    return;
                }

                String urlStr = ApiConfig.getApiUrl(this, "trip/active-detail/driver/" + driverId);

                URL url = new URL(urlStr);
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

                    String jsonStr = response.toString();
                    // Logueamos una parte del JSON para no saturar, pero ver si hay datos

                    JSONObject json = new JSONObject(jsonStr);
                    String routeGeometry = json.optString("routeGeometry", json.optString("RouteGeometry"));


                    // Extraer datos del viaje (el backend devuelve con primera letra mayúscula)
                    currentTripId = json.optString("tripId", json.optString("TripId"));
                    assignedBusId = json.optString("busId", json.optString("BusId"));
                    assignedRouteId = json.optString("routeId", json.optString("RouteId"));

                    // !!! VALIDACIÓN CLAVE
                    if (routeGeometry == null || routeGeometry.isEmpty()) {
                    } else {
                    }

                    double startLat = json.optDouble("startLat", 0.0);
                    double startLng = json.optDouble("startLng", 0.0);


                    // Si las coordenadas vienen en 0, forzamos Santa Cruz para no ver el océano
                    if (startLat == 0.0 && startLng == 0.0) {
                        startLat = -17.7833;
                        startLng = -63.1821;
                    }

                    double finalLat = startLat;
                    double finalLng = startLng;
                    String finalRouteGeometry = routeGeometry;

                    runOnUiThread(() -> {
                        dibujarRuta(finalRouteGeometry, finalLat, finalLng);

                        // NO iniciar simulación automáticamente desde RouteActivity
                        // La simulación solo se inicia desde MainActivity cuando se presiona "INICIAR
                        // VIAJE"
                        if (simulationService != null && simulationService.isSimulating()) {
                            Toast.makeText(this, "Ruta cargada - Simulación en curso", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "Ruta cargada", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    runOnUiThread(() -> Toast
                            .makeText(this, "No hay viaje activo", Toast.LENGTH_LONG)
                            .show());
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private String obtenerDriverId(String email) throws Exception {
        String urlStr = ApiConfig.getApiUrl(this, "driver/email/" + email);
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        if (conn.getResponseCode() == 200) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null)
                response.append(line);
            JSONObject json = new JSONObject(response.toString());
            return json.has("id") ? json.getString("id") : json.getString("Id");
        }
        return null;
    }

    private void dibujarRuta(String geometry, double lat, double lng) {
        if (mapView == null)
            return;


        mapView.getMapboxMap().getStyle(style -> {
            try {
                // IMPORTANTE: OSRM devuelve polylines con precisión 5 (no 6)
                // Precisión 5 = estándar de Google/OSRM (5 dígitos decimales)
                // Precisión 6 = usado por algunos servicios (6 dígitos decimales)
                LineString lineString = LineString.fromPolyline(geometry, 5);
                routePoints = lineString.coordinates();

                if (routePoints.size() > 0) {
                }

                // PRIMERO: Dibujar la línea azul de la ruta completa
                String routeSourceId = "route-source";
                String routeLayerId = "route-layer";

                // Limpiar si existe
                if (style.styleSourceExists(routeSourceId)) {
                    style.removeStyleLayer(routeLayerId);
                    style.removeStyleSource(routeSourceId);
                }

                // Crear fuente para la línea de la ruta
                com.mapbox.maps.extension.style.sources.generated.GeoJsonSource routeSource = new com.mapbox.maps.extension.style.sources.generated.GeoJsonSource.Builder(
                        routeSourceId)
                        .geometry(lineString)
                        .build();

                routeSource.bindTo(style);

                // Capa de línea para la ruta (azul)
                com.mapbox.maps.extension.style.layers.generated.LineLayer routeLayer = new com.mapbox.maps.extension.style.layers.generated.LineLayer(
                        routeLayerId, routeSourceId)
                        .lineColor("#0080FF") // Azul
                        .lineWidth(5.0);

                routeLayer.bindTo(style);

                // Crear marcador de inicio (azul) usando drawable
                String startSourceId = "start-source";
                String startLayerId = "start-layer";

                if (style.styleSourceExists(startSourceId)) {
                    style.removeStyleLayer(startLayerId);
                    style.removeStyleSource(startSourceId);
                }

                Point startPoint = routePoints.get(0);
                com.mapbox.maps.extension.style.sources.generated.GeoJsonSource startSource = new com.mapbox.maps.extension.style.sources.generated.GeoJsonSource.Builder(
                        startSourceId)
                        .feature(Feature.fromGeometry(startPoint))
                        .build();
                startSource.bindTo(style);

                // Cargar el icono de ubicación azul
                style.addImage("start-marker-icon", getBitmapFromVectorDrawable(R.drawable.ic_location_blue));

                com.mapbox.maps.extension.style.layers.generated.SymbolLayer startLayer = new com.mapbox.maps.extension.style.layers.generated.SymbolLayer(
                        startLayerId, startSourceId)
                        .iconImage("start-marker-icon")
                        .iconSize(1.5)
                        .iconAnchor(IconAnchor.BOTTOM);
                startLayer.bindTo(style);

                // Crear marcador de fin (rojo) usando drawable
                String endSourceId = "end-source";
                String endLayerId = "end-layer";

                if (style.styleSourceExists(endSourceId)) {
                    style.removeStyleLayer(endLayerId);
                    style.removeStyleSource(endSourceId);
                }

                Point endPoint = routePoints.get(routePoints.size() - 1);
                com.mapbox.maps.extension.style.sources.generated.GeoJsonSource endSource = new com.mapbox.maps.extension.style.sources.generated.GeoJsonSource.Builder(
                        endSourceId)
                        .feature(Feature.fromGeometry(endPoint))
                        .build();
                endSource.bindTo(style);

                // Cargar el icono de ubicación rojo
                style.addImage("end-marker-icon", getBitmapFromVectorDrawable(R.drawable.ic_location_red));

                com.mapbox.maps.extension.style.layers.generated.SymbolLayer endLayer = new com.mapbox.maps.extension.style.layers.generated.SymbolLayer(
                        endLayerId, endSourceId)
                        .iconImage("end-marker-icon")
                        .iconSize(1.5)
                        .iconAnchor(IconAnchor.BOTTOM);
                endLayer.bindTo(style);

                // SEGUNDO: Preparar el marcador del bus con diseño mejorado
                String busSourceId = "bus-source";

                // Limpiar si existe
                if (style.styleSourceExists(busSourceId)) {
                    style.removeStyleLayer("bus-shadow-layer");
                    style.removeStyleLayer("bus-outer-layer");
                    style.removeStyleLayer("bus-ring-layer");
                    style.removeStyleLayer("bus-layer");
                    style.removeStyleSource(busSourceId);
                }

                // Posición inicial del bus
                currentBusPosition = routePoints.get(0);
                currentPointIndex = 0;

                // Crear fuente para el marcador del bus
                com.mapbox.maps.extension.style.sources.generated.GeoJsonSource busSource = new com.mapbox.maps.extension.style.sources.generated.GeoJsonSource.Builder(
                        busSourceId)
                        .feature(Feature.fromGeometry(currentBusPosition))
                        .build();

                busSource.bindTo(style);

                // Círculo exterior del bus (sombra)
                com.mapbox.maps.extension.style.layers.generated.CircleLayer busShadowLayer = new com.mapbox.maps.extension.style.layers.generated.CircleLayer(
                        "bus-shadow-layer", busSourceId)
                        .circleRadius(18.0)
                        .circleColor(Color.parseColor("#40000000")) // Sombra
                        .circleOpacity(0.3);
                busShadowLayer.bindTo(style);

                // Círculo exterior del bus (gradiente)
                com.mapbox.maps.extension.style.layers.generated.CircleLayer busOuterLayer = new com.mapbox.maps.extension.style.layers.generated.CircleLayer(
                        "bus-outer-layer", busSourceId)
                        .circleRadius(16.0)
                        .circleColor(Color.parseColor("#8D0743")); // Color principal
                busOuterLayer.bindTo(style);

                // Anillo blanco
                com.mapbox.maps.extension.style.layers.generated.CircleLayer busRingLayer = new com.mapbox.maps.extension.style.layers.generated.CircleLayer(
                        "bus-ring-layer", busSourceId)
                        .circleRadius(13.0)
                        .circleColor(Color.WHITE);
                busRingLayer.bindTo(style);

                // Círculo interior del bus
                com.mapbox.maps.extension.style.layers.generated.CircleLayer busLayer = new com.mapbox.maps.extension.style.layers.generated.CircleLayer(
                        "bus-layer", busSourceId)
                        .circleRadius(10.0)
                        .circleColor(Color.parseColor("#B31D5A")); // Color vibrante
                busLayer.bindTo(style);

                // Centrar cámara en la posición inicial
                mapView.getMapboxMap().setCamera(
                        new CameraOptions.Builder()
                                .center(currentBusPosition)
                                .zoom(15.0) // Zoom más cercano para seguir al bus
                                .build());

                // Ya no necesitamos animación manual, SignalR se encargará de actualizar la
                // posición

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Solo detener la animación visual, NO la simulación del servicio
        if (animationHandler != null) {
            animationHandler.removeCallbacksAndMessages(null);
        }

        // Desconectar SignalR
        if (hubConnection != null && hubConnection.getConnectionState() == HubConnectionState.CONNECTED) {
            new Thread(() -> {
                try {
                    hubConnection.stop().blockingAwait();
                } catch (Exception e) {
                }
            }).start();
        }

        // La simulación continúa en el servicio Singleton
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