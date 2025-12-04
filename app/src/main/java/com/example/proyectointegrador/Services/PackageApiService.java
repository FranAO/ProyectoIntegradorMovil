package com.example.proyectointegrador.Services;

import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.example.proyectointegrador.Models.Package;

public class PackageApiService {
    
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());
    
    public interface PackageListCallback {
        void onSuccess(List<Package> packages);
        void onError(String error);
    }
    
    public static void getAllPackages(PackageListCallback callback) {
        executor.execute(() -> {
            try {
                String endpoint = "/package";
                ApiService.ApiResponse response = ApiService.get(endpoint);
                
                if (response.success) {
                    JSONArray jsonArray = new JSONArray(response.data);
                    List<Package> packages = new ArrayList<>();
                    
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject json = jsonArray.getJSONObject(i);
                        
                        Package pkg = new Package(
                            json.getString("id"),
                            json.getString("name"),
                            json.getString("description"),
                            json.getInt("ticketCount"),
                            json.getDouble("price"),
                            json.getInt("durationDays"),
                            json.getBoolean("active")
                        );
                        
                        // Solo agregar packages activos
                        if (pkg.isActive()) {
                            packages.add(pkg);
                        }
                    }
                    
                    mainHandler.post(() -> callback.onSuccess(packages));
                } else {
                    mainHandler.post(() -> callback.onError(response.error != null ? response.error : "Error al cargar paquetes"));
                }
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }
}
