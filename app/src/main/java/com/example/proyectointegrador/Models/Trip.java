package com.example.proyectointegrador.Models;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Trip {
    public String id;
    public String busId;
    public String driverId;
    public String routeId;
    public String routeName; // Nombre descriptivo de la ruta
    public Date startTime;
    public Date endTime;
    public String status;
    public int occupiedSeats;
    public int totalSeats;

    // Default constructor
    public Trip() {
    }

    // Constructor with parameters
    public Trip(String id, String busId, String driverId, String routeId, Date startTime, Date endTime, String status) {
        this.id = id;
        this.busId = busId;
        this.driverId = driverId;
        this.routeId = routeId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = status;
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getBusId() {
        return busId;
    }

    public String getDriverId() {
        return driverId;
    }

    public String getRouteId() {
        return routeId;
    }

    public Date getStartTime() {
        return startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public String getStatus() {
        return status;
    }

    public int getOccupiedSeats() {
        return occupiedSeats;
    }

    public int getTotalSeats() {
        return totalSeats;
    }

    // Setters
    public void setId(String id) {
        this.id = id;
    }

    public void setBusId(String busId) {
        this.busId = busId;
    }

    public void setDriverId(String driverId) {
        this.driverId = driverId;
    }

    public void setRouteId(String routeId) {
        this.routeId = routeId;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setOccupiedSeats(int occupiedSeats) {
        this.occupiedSeats = occupiedSeats;
    }

    public void setTotalSeats(int totalSeats) {
        this.totalSeats = totalSeats;
    }

    @Override
    public String toString() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        String dateStr = startTime != null ? dateFormat.format(startTime) : "Fecha no disponible";
        String route = routeName != null && !routeName.isEmpty() ? routeName : "Ruta desconocida";
        return route + " - " + dateStr;
    }
}
