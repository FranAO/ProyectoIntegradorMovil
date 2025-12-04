package com.example.proyectointegrador.Adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.proyectointegrador.Models.HistorialTrip;
import com.example.proyectointegrador.R;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class HistorialAdapter extends RecyclerView.Adapter<HistorialAdapter.HistorialViewHolder> {
    private List<HistorialTrip> historial;
    private SimpleDateFormat timeFormatter = new SimpleDateFormat("HH:mm", Locale.US);

    public HistorialAdapter(List<HistorialTrip> historial) {
        this.historial = historial;
    }

    @NonNull
    @Override
    public HistorialViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_historial, parent, false);
        return new HistorialViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistorialViewHolder holder, int position) {
        HistorialTrip trip = historial.get(position);
        holder.busNumberTextView.setText(trip.getCodigoBus());
        holder.routeTextView.setText(trip.getNombreRuta());
        String horario = timeFormatter.format(trip.getHoraInicio()) + " - " + timeFormatter.format(trip.getHoraFin());
        holder.scheduleTextView.setText(horario);
    }

    @Override
    public int getItemCount() {
        return historial.size();
    }

    static class HistorialViewHolder extends RecyclerView.ViewHolder {
        TextView busNumberTextView;
        TextView routeTextView;
        TextView scheduleTextView;

        HistorialViewHolder(View itemView) {
            super(itemView);
            busNumberTextView = itemView.findViewById(R.id.busNumberTextView);
            routeTextView = itemView.findViewById(R.id.routeTextView);
            scheduleTextView = itemView.findViewById(R.id.scheduleTextView);
        }
    }
}
