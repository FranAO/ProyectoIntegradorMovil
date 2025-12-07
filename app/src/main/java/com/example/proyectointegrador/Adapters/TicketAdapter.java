package com.example.proyectointegrador.Adapters;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.proyectointegrador.Config.ApiConfig;
import com.example.proyectointegrador.Models.Ticket;
import com.example.proyectointegrador.R;
import com.example.proyectointegrador.UI.TicketDetailActivity;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;

public class TicketAdapter extends RecyclerView.Adapter<TicketAdapter.TicketViewHolder> {

    private Context context;
    private ArrayList<Ticket> ticketList;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    public TicketAdapter(Context context, ArrayList<Ticket> ticketList) {
        this.context = context;
        this.ticketList = ticketList;
    }

    @NonNull
    @Override
    public TicketViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_ticket, parent, false);
        return new TicketViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TicketViewHolder holder, int position) {
        Ticket ticket = ticketList.get(position);

        // Mostrar información del viaje si existe TripId
        if (ticket.getTripId() != null && !ticket.getTripId().isEmpty() && !ticket.getTripId().equals("null")) {
            loadTripInfo(ticket.getTripId(), holder.tvTicketId);
        } else {
            holder.tvTicketId.setText("Ticket válido - Pendiente de uso");
        }

        if (ticket.getPurchaseDate() != null) {
            holder.tvTicketDate.setText(dateFormat.format(ticket.getPurchaseDate()));
        }

        String tipo = ticket.getPackageId() != null && !ticket.getPackageId().isEmpty() 
            ? "Ticket de Paquete" : "Ticket Individual";
        holder.tvTicketType.setText(tipo);

        String status = ticket.getStatus();
        if ("available".equals(status)) {
            holder.tvTicketStatus.setText("Disponible");
            holder.tvTicketStatus.setBackgroundResource(R.drawable.status_badge_available);
        } else {
            holder.tvTicketStatus.setText("Usado");
            holder.tvTicketStatus.setBackgroundResource(R.drawable.status_badge_used);
        }

        try {
            // Solo incluir el ticketId en el QR
            // El backend obtiene toda la información del ticket usando este ID
            String qrData = ticket.getId();
            BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
            Bitmap bitmap = barcodeEncoder.encodeBitmap(qrData, BarcodeFormat.QR_CODE, 200, 200);
            holder.imgQRThumbnail.setImageBitmap(bitmap);
        } catch (WriterException e) {
            e.printStackTrace();
        }

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, TicketDetailActivity.class);
            intent.putExtra("ticketId", ticket.getId());
            intent.putExtra("studentEmail", ticket.getStudentId());
            intent.putExtra("tripId", ticket.getTripId());
            intent.putExtra("date", ticket.getPurchaseDate() != null ? 
                dateFormat.format(ticket.getPurchaseDate()) : "");
            intent.putExtra("type", tipo);
            intent.putExtra("status", ticket.getStatus());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return ticketList.size();
    }

    private void loadTripInfo(String tripId, TextView textView) {
        // Validar que tripId no sea null o inválido
        if (tripId == null || tripId.isEmpty() || tripId.equals("null") || tripId.length() != 24) {
            ((android.app.Activity) context).runOnUiThread(() -> 
                textView.setText("Ticket válido - Pendiente de uso")
            );
            return;
        }
        
        new Thread(() -> {
            try {
                java.net.URL url = new java.net.URL(ApiConfig.getApiUrl(context, "/trip/" + tripId));
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                org.json.JSONObject tripJson = new org.json.JSONObject(response.toString());
                String routeId = tripJson.has("RouteId") ? tripJson.getString("RouteId") : tripJson.getString("routeId");
                
                // Obtener información de la ruta
                java.net.URL urlRoute = new java.net.URL(ApiConfig.getApiUrl(context, "/busroute/" + routeId));
                java.net.HttpURLConnection connRoute = (java.net.HttpURLConnection) urlRoute.openConnection();
                connRoute.setRequestMethod("GET");

                java.io.BufferedReader readerRoute = new java.io.BufferedReader(new java.io.InputStreamReader(connRoute.getInputStream()));
                StringBuilder responseRoute = new StringBuilder();
                String lineRoute;
                while ((lineRoute = readerRoute.readLine()) != null) {
                    responseRoute.append(lineRoute);
                }
                readerRoute.close();

                org.json.JSONObject routeJson = new org.json.JSONObject(responseRoute.toString());
                String routeName = routeJson.has("Name") ? routeJson.getString("Name") : routeJson.getString("name");
                
                ((android.app.Activity) context).runOnUiThread(() -> {
                    textView.setText(routeName);
                });

            } catch (Exception e) {
                e.printStackTrace();
                ((android.app.Activity) context).runOnUiThread(() -> {
                    textView.setText("Viaje #" + tripId.substring(Math.max(0, tripId.length() - 6)));
                });
            }
        }).start();
    }

    public static class TicketViewHolder extends RecyclerView.ViewHolder {
        ImageView imgQRThumbnail;
        TextView tvTicketId, tvTicketDate, tvTicketType, tvTicketStatus;

        public TicketViewHolder(@NonNull View itemView) {
            super(itemView);
            imgQRThumbnail = itemView.findViewById(R.id.imgQRThumbnail);
            tvTicketId = itemView.findViewById(R.id.tvTicketId);
            tvTicketDate = itemView.findViewById(R.id.tvTicketDate);
            tvTicketType = itemView.findViewById(R.id.tvTicketType);
            tvTicketStatus = itemView.findViewById(R.id.tvTicketStatus);
        }
    }
}
