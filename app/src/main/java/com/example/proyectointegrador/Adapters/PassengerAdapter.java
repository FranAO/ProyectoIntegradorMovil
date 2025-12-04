package com.example.proyectointegrador.Adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.proyectointegrador.Models.Passenger;
import com.example.proyectointegrador.R;

import java.util.List;

public class PassengerAdapter extends RecyclerView.Adapter<PassengerAdapter.PassengerViewHolder> {
    private List<Passenger> pasajeros;
    private OnScanQRClickListener scanQRListener;

    public interface OnScanQRClickListener {
        void onScanQR(Passenger passenger);
    }

    public PassengerAdapter(List<Passenger> pasajeros, OnScanQRClickListener listener) {
        this.pasajeros = pasajeros;
        this.scanQRListener = listener;
    }

    @NonNull
    @Override
    public PassengerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_passenger, parent, false);
        return new PassengerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PassengerViewHolder holder, int position) {
        Passenger pasajero = pasajeros.get(position);
        holder.tvPassangerName.setText(pasajero.getName());
        
        String status = pasajero.getTicketStatus();
        
        // Ocultar todos los iconos primero
        holder.ivPendingIcon.setVisibility(View.GONE);
        holder.ivConfirmIcon.setVisibility(View.GONE);
        holder.ivCancelIcon.setVisibility(View.GONE);
        
        // Mostrar el icono correcto según el estado
        if ("confirmed".equals(status)) {
            holder.ivConfirmIcon.setVisibility(View.VISIBLE);
            holder.stateIconContainer.setCardBackgroundColor(0xFF4CAF50); // Verde
        } else if ("cancelled".equals(status)) {
            holder.ivCancelIcon.setVisibility(View.VISIBLE);
            holder.stateIconContainer.setCardBackgroundColor(0xFFE53935); // Rojo
        } else {
            // Estado "pending" o cualquier otro
            holder.ivPendingIcon.setVisibility(View.VISIBLE);
            holder.stateIconContainer.setCardBackgroundColor(0xFF9CADBA); // Gris
        }
        
        // Click listener para escanear QR (solo si está pendiente)
        holder.stateIconContainer.setOnClickListener(v -> {
            if ("pending".equals(status) && scanQRListener != null) {
                scanQRListener.onScanQR(pasajero);
            }
        });
    }

    @Override
    public int getItemCount() {
        return pasajeros.size();
    }

    public void updateTicketStatus(String passengerId, String newStatus) {
        for (int i = 0; i < pasajeros.size(); i++) {
            if (pasajeros.get(i).getId().equals(passengerId)) {
                pasajeros.get(i).setTicketStatus(newStatus);
                notifyItemChanged(i);
                break;
            }
        }
    }

    static class PassengerViewHolder extends RecyclerView.ViewHolder {
        TextView tvPassangerName;
        CardView stateIconContainer;
        ImageView ivPendingIcon;
        ImageView ivConfirmIcon;
        ImageView ivCancelIcon;

        PassengerViewHolder(View itemView) {
            super(itemView);
            tvPassangerName = itemView.findViewById(R.id.tvPassangerName);
            stateIconContainer = itemView.findViewById(R.id.stateIconContainer);
            ivPendingIcon = itemView.findViewById(R.id.ivPendingIcon);
            ivConfirmIcon = itemView.findViewById(R.id.ivConfirmIcon);
            ivCancelIcon = itemView.findViewById(R.id.ivCancelIcon);
        }
    }
}
