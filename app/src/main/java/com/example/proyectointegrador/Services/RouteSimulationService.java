package com.example.proyectointegrador.Services;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.microsoft.signalr.HubConnection;
import com.microsoft.signalr.HubConnectionBuilder;
import com.microsoft.signalr.HubConnectionState;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Servicio para simular el movimiento del bus a lo largo de una ruta
 * Implementado como Singleton para mantener el estado entre Activities
 */
public class RouteSimulationService {
    private static final String TAG = "RouteSimulation";
    private static RouteSimulationService instance;
    
    private Context context;
    private String baseUrl;
    private HubConnection hubConnection;
    
    private List<RoutePoint> routePoints;
    private int currentPointIndex = 0;
    private boolean isSimulating = false;
    private boolean simulationStartedByDriver = false;
    
    private String currentTripId;
    private String currentBusId;
    private String currentRouteId;
    
    private Handler simulationHandler;
    private static final long SIMULATION_INTERVAL = 3000; // 3 segundos entre puntos
    
    /**
     * Clase interna para representar un punto de la ruta
     * Movida al final de la clase como clase pública estática
     */
    
    private RouteSimulationService(Context context, String baseUrl) {
        this.context = context.getApplicationContext();
        this.baseUrl = baseUrl;
        this.simulationHandler = new Handler(Looper.getMainLooper());
        this.routePoints = new ArrayList<>();
        initializeSignalR();
    }
    
    /**
     * Obtiene la instancia única del servicio (Singleton)
     */
    public static synchronized RouteSimulationService getInstance(Context context, String baseUrl) {
        if (instance == null) {
            instance = new RouteSimulationService(context, baseUrl);
        }
        return instance;
    }
    
    /**
     * Inicializa la conexión SignalR
     */
    private void initializeSignalR() {
        try {
            // Limpiar la URL base y construir la URL del hub correctamente
            String cleanBaseUrl = baseUrl.replace("/api", "").replace("/api/", "");
            if (cleanBaseUrl.endsWith("/")) {
                cleanBaseUrl = cleanBaseUrl.substring(0, cleanBaseUrl.length() - 1);
            }
            String hubUrl = cleanBaseUrl + "/bushub";
            
            hubConnection = HubConnectionBuilder.create(hubUrl)
                    .build();
            
            hubConnection.onClosed(exception -> {
                // Intentar reconectar
                simulationHandler.postDelayed(() -> {
                    if (hubConnection.getConnectionState() == HubConnectionState.DISCONNECTED) {
                        connectSignalR();
                    }
                }, 5000);
            });
            
        } catch (Exception e) {
        }
    }
    
    /**
     * Conecta a SignalR
     */
    private void connectSignalR() {
        if (hubConnection == null) {
            initializeSignalR();
        }
        
        if (hubConnection.getConnectionState() == HubConnectionState.DISCONNECTED) {
            new Thread(() -> {
                try {
                    hubConnection.start().blockingAwait();
                } catch (Exception e) {
                }
            }).start();
        }
    }
    
    /**
     * Inicia la simulación de la ruta
     * @param routeGeometry GeoJSON string con la geometría de la ruta
     * @param tripId ID del viaje
     * @param busId ID del bus
     * @param routeId ID de la ruta
     */
    public void startSimulation(String routeGeometry, String tripId, String busId, String routeId) {
        this.currentTripId = tripId;
        this.currentBusId = busId;
        this.currentRouteId = routeId;
        
        // Parsear la geometría de la ruta
        if (!parseRouteGeometry(routeGeometry)) {
            return;
        }
        
        // Conectar a SignalR si no está conectado
        if (hubConnection.getConnectionState() != HubConnectionState.CONNECTED) {
            connectSignalR();
            // Dar tiempo para la conexión antes de empezar a enviar datos
            simulationHandler.postDelayed(() -> {
                continueSimulation();
            }, 2000);
        } else {
            continueSimulation();
        }
    }
    
    private void continueSimulation() {
        // Iniciar la simulación
        isSimulating = true;
        simulationStartedByDriver = true;
        currentPointIndex = 0;
        
        // Notificar que el viaje ha iniciado via SignalR
        notifyTripStarted();
        
        simulateNextPoint();
    }
    
    /**
     * Notifica via SignalR que el viaje ha iniciado
     */
    private void notifyTripStarted() {
        if (hubConnection == null || hubConnection.getConnectionState() != HubConnectionState.CONNECTED) {
            return;
        }
        
        new Thread(() -> {
            try {
                // Enviar notificación de inicio de viaje
                hubConnection.send("NotifyTripStarted", currentTripId, currentBusId, currentRouteId);
            } catch (Exception e) {
            }
        }).start();
    }
    
    /**
     * Notifica via SignalR que el viaje ha finalizado
     */
    private void notifyTripEnded() {
        if (hubConnection == null || hubConnection.getConnectionState() != HubConnectionState.CONNECTED) {
            return;
        }
        
        new Thread(() -> {
            try {
                // Enviar notificación de fin de viaje
                hubConnection.send("NotifyTripEnded", currentTripId);
                
                // Actualizar el estado del viaje en el servidor
                updateTripStatus();
            } catch (Exception e) {
            }
        }).start();
    }
    
    /**
     * Actualiza el estado del viaje a 'completed'
     */
    private void updateTripStatus() {
        new Thread(() -> {
            try {
                String urlStr = baseUrl + "trip/" + currentTripId + "/complete";
                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("PUT");
                conn.setRequestProperty("Content-Type", "application/json");
                
                int responseCode = conn.getResponseCode();
                if (responseCode == 200 || responseCode == 204) {
                } else {
                }
                conn.disconnect();
            } catch (Exception e) {
            }
        }).start();
    }
    
    /**
     * Decodifica una cadena polyline de Google Maps
     * @param encoded Cadena polyline codificada
     * @return Lista de RoutePoints
     */
    private List<RoutePoint> decodePolyline(String encoded) {
        List<RoutePoint> poly = new ArrayList<>();
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

            // Polyline con precisión de 6 decimales (1E6) - estándar de Mapbox
            RoutePoint p = new RoutePoint((double) lat / 1E6, (double) lng / 1E6);
            poly.add(p);
        }

        return poly;
    }
    
    /**
     * Parsea la geometría de la ruta (ahora acepta polyline codificado)
     */
    private boolean parseRouteGeometry(String routeGeometry) {
        try {
            routePoints.clear();
            
            // Intentar decodificar como polyline primero (formato esperado del backend)
            try {
                routePoints = decodePolyline(routeGeometry);
                return routePoints.size() > 0;
            } catch (Exception e) {
            }
            
            // Si falla, intentar parsear como GeoJSON (legacy)
            JSONObject geoJson = new JSONObject(routeGeometry);
            String type = geoJson.getString("type");
            
            if ("LineString".equals(type)) {
                JSONArray coordinates = geoJson.getJSONArray("coordinates");
                for (int i = 0; i < coordinates.length(); i++) {
                    JSONArray coord = coordinates.getJSONArray(i);
                    double lng = coord.getDouble(0);
                    double lat = coord.getDouble(1);
                    routePoints.add(new RoutePoint(lat, lng));
                }
            } else {
                return false;
            }
            
            return routePoints.size() > 0;
            
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Simula el siguiente punto de la ruta
     */
    private void simulateNextPoint() {
        if (!isSimulating || currentPointIndex >= routePoints.size()) {
            // Fin de la simulación
            if (currentPointIndex >= routePoints.size()) {
                notifyTripEnded();
            }
            isSimulating = false;
            return;
        }
        
        RoutePoint point = routePoints.get(currentPointIndex);
        
        // Enviar ubicación a través de SignalR
        sendLocationUpdate(point.latitude, point.longitude);
        
        currentPointIndex++;
        
        // Programar el siguiente punto
        simulationHandler.postDelayed(this::simulateNextPoint, SIMULATION_INTERVAL);
    }
    
    /**
     * Envía la ubicación actual al servidor a través de SignalR
     */
    private void sendLocationUpdate(double latitude, double longitude) {
        if (hubConnection == null || hubConnection.getConnectionState() != HubConnectionState.CONNECTED) {
            return;
        }
        
        new Thread(() -> {
            try {
                String status = "in_progress";
                
                // Enviar ubicación al hub - este método existe en BusHub.cs
                hubConnection.send("UpdateBusLocation", currentBusId, latitude, longitude, status);
                
            } catch (Exception e) {
            }
        }).start();
    }
    
    /**
     * Detiene la simulación
     */
    public void stopSimulation() {
        isSimulating = false;
        currentPointIndex = 0;
        simulationHandler.removeCallbacksAndMessages(null);
        
        // Desconectar SignalR
        if (hubConnection != null && hubConnection.getConnectionState() == HubConnectionState.CONNECTED) {
            new Thread(() -> {
                try {
                    hubConnection.stop().blockingAwait();
                } catch (Exception e) {
                }
            }).start();
        }
    }
    
    /**
     * Verifica si la simulación está activa
     */
    public boolean isSimulating() {
        return isSimulating;
    }
    
    /**
     * Obtiene el índice del punto actual
     */
    public int getCurrentPointIndex() {
        return currentPointIndex;
    }
    
    /**
     * Obtiene el total de puntos de la ruta
     */
    public int getTotalPoints() {
        return routePoints.size();
    }
    
    /**
     * Obtiene la posición actual del bus
     */
    public RoutePoint getCurrentPosition() {
        if (routePoints.isEmpty() || currentPointIndex >= routePoints.size()) {
            return null;
        }
        return routePoints.get(currentPointIndex);
    }
    
    /**
     * Obtiene el punto en un índice específico
     */
    public RoutePoint getPointAt(int index) {
        if (index >= 0 && index < routePoints.size()) {
            return routePoints.get(index);
        }
        return null;
    }
    
    /**
     * Clase interna RoutePoint - accesible desde otras clases
     */
    public static class RoutePoint {
        public double latitude;
        public double longitude;
        
        public RoutePoint(double latitude, double longitude) {
            this.latitude = latitude;
            this.longitude = longitude;
        }
    }
}
