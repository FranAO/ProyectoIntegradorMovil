package com.example.proyectointegrador.Config;

import android.content.Context;
import android.content.SharedPreferences;

public class ApiConfig {
    private static final String PREFS_NAME = "ApiConfigPrefs";
    private static final String KEY_BASE_URL = "base_url";
    
    // URLs predefinidas
    private static final String EMULATOR_URL = "http://10.0.2.2:5090";
    private static final String DEFAULT_PHYSICAL_URL = "http://10.26.10.34:5090"; // IP de tu computadora
    
    private static String customUrl = null;
    
    /**
     * Obtiene la URL base configurada
     */
    public static String getBaseUrl(Context context) {
        if (customUrl != null) {
            return customUrl;
        }
        
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_BASE_URL, DEFAULT_PHYSICAL_URL);
    }
    
    /**
     * Guarda una URL personalizada
     */
    public static void setBaseUrl(Context context, String url) {
        customUrl = url;
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_BASE_URL, url).apply();
    }
    
    /**
     * Configura para usar el emulador
     */
    public static void useEmulator(Context context) {
        setBaseUrl(context, EMULATOR_URL);
    }
    
    /**
     * Configura para usar dispositivo físico con la IP por defecto
     */
    public static void usePhysicalDevice(Context context) {
        setBaseUrl(context, DEFAULT_PHYSICAL_URL);
    }
    
    /**
     * Configura para usar dispositivo físico con una IP personalizada
     */
    public static void usePhysicalDevice(Context context, String ipAddress) {
        setBaseUrl(context, "http://" + ipAddress + ":5090");
    }
    
    /**
     * Obtiene la URL completa para un endpoint
     */
    public static String getApiUrl(Context context, String endpoint) {
        String baseUrl = getBaseUrl(context);
        if (endpoint.startsWith("/")) {
            return baseUrl + "/api" + endpoint;
        }
        return baseUrl + "/api/" + endpoint;
    }
}
