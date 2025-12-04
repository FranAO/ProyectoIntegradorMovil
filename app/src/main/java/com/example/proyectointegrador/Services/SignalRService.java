package com.example.proyectointegrador.Services;

import android.content.Context;

import com.example.proyectointegrador.Config.ApiConfig;
import com.microsoft.signalr.HubConnection;
import com.microsoft.signalr.HubConnectionBuilder;
import com.microsoft.signalr.HubConnectionState;

public class SignalRService {
    private static final String TAG = "SignalRService";
    private HubConnection hubConnection;
    private Context context;
    private SignalRListener listener;

    public interface SignalRListener {
        void onBusLocationReceived(String busId, double latitude, double longitude, String status);
        void onTripStarted(String tripId, String busId, String routeId);
        void onTripEnded(String tripId);
        void onTicketScanned(String ticketId, String tripId, String studentId);
    }

    public SignalRService(Context context, SignalRListener listener) {
        this.context = context;
        this.listener = listener;
        initializeConnection();
    }

    private void initializeConnection() {
        try {
            // Obtener la base URL y construir la URL del hub
            String baseUrl = ApiConfig.getBaseUrl(context);
            String hubUrl = baseUrl.replace("/api", "") + "/bushub";
            

            hubConnection = HubConnectionBuilder.create(hubUrl)
                    .build();

            // Registrar handler para ubicación del bus
            hubConnection.on("ReceiveBusLocation", (busId, latitude, longitude, status) -> {
                android.util.Log.d("SignalR", String.format("📡 ReceiveBusLocation: busId=%s, lat=%f, lng=%f, status=%s", busId, latitude, longitude, status));
                if (listener != null) {
                    listener.onBusLocationReceived(busId, latitude, longitude, status);
                } else {
                    android.util.Log.w("SignalR", "Listener es null, no se puede procesar ubicación");
                }
            }, String.class, Double.class, Double.class, String.class);

            // Registrar handler para inicio de viaje
            hubConnection.on("TripStarted", (tripId, busId, routeId) -> {
                android.util.Log.d("SignalR", String.format("🚀 TripStarted: tripId=%s, busId=%s, routeId=%s", tripId, busId, routeId));
                if (listener != null) {
                    listener.onTripStarted(tripId, busId, routeId);
                }
            }, String.class, String.class, String.class);

            // Registrar handler para fin de viaje
            hubConnection.on("TripEnded", (tripId) -> {
                android.util.Log.d("SignalR", String.format("🏁 TripEnded: tripId=%s", tripId));
                if (listener != null) {
                    listener.onTripEnded(tripId);
                }
            }, String.class);

            // Registrar handler para ticket escaneado
            hubConnection.on("TicketScanned", (ticketId, tripId, studentId) -> {
                if (listener != null) {
                    listener.onTicketScanned(ticketId, tripId, studentId);
                }
            }, String.class, String.class, String.class);

            hubConnection.onClosed(exception -> {
                // Intentar reconectar después de 5 segundos
                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                    if (hubConnection.getConnectionState() == HubConnectionState.DISCONNECTED) {
                        connect();
                    }
                }, 5000);
            });

        } catch (Exception e) {
        }
    }

    public void connect() {
        if (hubConnection == null) {
            android.util.Log.d("SignalR", "🔧 Inicializando conexión...");
            initializeConnection();
        }

        if (hubConnection.getConnectionState() == HubConnectionState.DISCONNECTED) {
            android.util.Log.d("SignalR", "🔗 Intentando conectar...");
            new Thread(() -> {
                try {
                    hubConnection.start().blockingAwait();
                    android.util.Log.d("SignalR", "✅ Conectado exitosamente");
                } catch (Exception e) {
                    android.util.Log.e("SignalR", "❌ Error al conectar: " + e.getMessage());
                }
            }).start();
        }
    }

    public void disconnect() {
        if (hubConnection != null && hubConnection.getConnectionState() == HubConnectionState.CONNECTED) {
            new Thread(() -> {
                try {
                    hubConnection.stop().blockingAwait();
                } catch (Exception e) {
                }
            }).start();
        }
    }

    public boolean isConnected() {
        return hubConnection != null && hubConnection.getConnectionState() == HubConnectionState.CONNECTED;
    }
}
