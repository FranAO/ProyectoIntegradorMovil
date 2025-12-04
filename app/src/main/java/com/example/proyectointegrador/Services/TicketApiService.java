package com.example.proyectointegrador.Services;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TicketApiService {
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface ApiCallback {
        void onSuccess(String response);
        void onError(String error);
    }

    public static void purchasePackage(String studentEmail, int ticketCount, int durationDays, ApiCallback callback) {
        executor.execute(() -> {
            try {
                String endpoint = "/Ticket/purchase-package?studentId=" + studentEmail + 
                                "&ticketCount=" + ticketCount + 
                                "&durationDays=" + durationDays;
                
                ApiService.ApiResponse response = ApiService.post(endpoint, null);

                mainHandler.post(() -> {
                    if (response.success) {
                        callback.onSuccess(response.data);
                    } else {
                        callback.onError(response.error != null ? response.error : "Error desconocido");
                    }
                });
            } catch (Exception e) {
                final String errorMsg = e.getMessage() != null ? e.getMessage() : "Error de conexión";
                mainHandler.post(() -> callback.onError("Error de conexión: " + errorMsg));
            }
        });
    }

    public static void getPackageSummary(String studentEmail, ApiCallback callback) {
        executor.execute(() -> {
            try {
                String endpoint = "/Ticket/package-summary/" + studentEmail;
                ApiService.ApiResponse response = ApiService.get(endpoint);

                mainHandler.post(() -> {
                    if (response.success) {
                        callback.onSuccess(response.data);
                    } else {
                        callback.onError(response.error != null ? response.error : "Error desconocido");
                    }
                });
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    public static void getPackageTicket(String studentEmail, ApiCallback callback) {
        executor.execute(() -> {
            try {
                String endpoint = "/Ticket/get-package-ticket/" + studentEmail;
                ApiService.ApiResponse response = ApiService.get(endpoint);

                mainHandler.post(() -> {
                    if (response.success) {
                        callback.onSuccess(response.data);
                    } else {
                        callback.onError(response.error != null ? response.error : "No hay tickets disponibles");
                    }
                });
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    public static void useTicketFromPackage(String studentEmail, String tripId, ApiCallback callback) {
        executor.execute(() -> {
            try {
                String endpoint = "/Ticket/use-from-package?studentId=" + studentEmail + "&tripId=" + tripId;
                ApiService.ApiResponse response = ApiService.post(endpoint, null);

                mainHandler.post(() -> {
                    if (response.success) {
                        callback.onSuccess(response.data);
                    } else {
                        callback.onError(response.error != null ? response.error : "Error desconocido");
                    }
                });
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    public static void purchaseSingleTicket(String studentEmail, String tripId, ApiCallback callback) {
        executor.execute(() -> {
            try {
                String endpoint = "/Ticket/purchase-single?studentId=" + studentEmail;
                if (tripId != null && !tripId.isEmpty()) {
                    endpoint += "&tripId=" + tripId;
                }
                ApiService.ApiResponse response = ApiService.post(endpoint, null);

                mainHandler.post(() -> {
                    if (response.success) {
                        callback.onSuccess(response.data);
                    } else {
                        callback.onError(response.error != null ? response.error : "Error desconocido");
                    }
                });
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    public static void getAvailableTickets(String studentEmail, ApiCallback callback) {
        executor.execute(() -> {
            try {
                String endpoint = "/Ticket/available/" + studentEmail;
                ApiService.ApiResponse response = ApiService.get(endpoint);

                mainHandler.post(() -> {
                    if (response.success) {
                        callback.onSuccess(response.data);
                    } else {
                        callback.onError(response.error != null ? response.error : "Error desconocido");
                    }
                });
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }
}
