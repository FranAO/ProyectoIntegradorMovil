package com.example.proyectointegrador.Adapters;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.proyectointegrador.Models.Bus;
import com.example.proyectointegrador.R;
import com.example.proyectointegrador.UI.TicketDetailActivity;

import java.util.ArrayList;

public class BusAdapter extends RecyclerView.Adapter<BusAdapter.BusViewHolder> {

    private ArrayList<Bus> busList;

    public BusAdapter(ArrayList<Bus> busList) {
        this.busList = busList;
    }

    @NonNull
    @Override
    public BusViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_bus, parent, false);
        return new BusViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BusViewHolder holder, int position) {
        Bus bus = busList.get(position);
        holder.tvBusNumber.setText(bus.getBusCode());
        holder.tvBusStatus.setText(bus.getCurrentPassengers() + "/" + bus.getCapacity());
        
        // Cargar nombre de ruta
        loadRouteName(bus.getRouteId(), holder.tvRouteName);
        
        // Click en el ícono del mapa para ver la ruta ESTÁTICA (no el viaje en tiempo real)
        holder.mapIconContainer.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), com.example.proyectointegrador.UI.RouteActivity.class);
            if (bus.getRouteId() != null && !bus.getRouteId().isEmpty()) {
                intent.putExtra("ROUTE_ID", bus.getRouteId());
                v.getContext().startActivity(intent);
            } else {
                Toast.makeText(v.getContext(), "No hay ruta disponible", Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void loadRouteName(String routeId, TextView textView) {
        if (routeId == null || routeId.isEmpty()) {
            textView.setText("Ruta no disponible");
            return;
        }
        
        new Thread(() -> {
            try {
                String url = com.example.proyectointegrador.Config.ApiConfig.getApiUrl(
                    textView.getContext(), 
                    "/busroute/" + routeId
                );
                
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) new java.net.URL(url).openConnection();
                conn.setRequestMethod("GET");
                
                if (conn.getResponseCode() == 200) {
                    java.io.BufferedReader reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(conn.getInputStream())
                    );
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();
                    
                    org.json.JSONObject routeJson = new org.json.JSONObject(response.toString());
                    String routeName = routeJson.optString("routeName", "Ruta");
                    
                    ((android.app.Activity) textView.getContext()).runOnUiThread(() -> {
                        textView.setText(routeName);
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                ((android.app.Activity) textView.getContext()).runOnUiThread(() -> {
                    textView.setText("Ruta");
                });
            }
        }).start();
    }

    @Override
    public int getItemCount() {
        return busList.size();
    }

    public static class BusViewHolder extends RecyclerView.ViewHolder {
        TextView tvBusNumber, tvBusStatus, tvRouteName;
        CardView mapIconContainer;

        public BusViewHolder(@NonNull View itemView) {
            super(itemView);
            tvBusNumber = itemView.findViewById(R.id.tvBusNumber);
            tvBusStatus = itemView.findViewById(R.id.tvBusStatus);
            tvRouteName = itemView.findViewById(R.id.tvRouteName);
            mapIconContainer = itemView.findViewById(R.id.mapIconContainer);
        }
    }
}
