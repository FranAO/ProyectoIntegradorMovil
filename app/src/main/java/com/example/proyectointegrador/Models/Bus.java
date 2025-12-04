package com.example.proyectointegrador.Models;

public class Bus {
    private String id;
    private String busCode;
    private String plate;
    private int capacity;
    private String status;
    private String driverId;
    private String routeId;
    private int currentPassengers;
    private String tripId; // ID del viaje activo asociado a este bus

    public Bus() {
    }

    public Bus(String id, String busCode, String plate, int capacity, String status, String driverId, String routeId) {
        this.id = id;
        this.busCode = busCode;
        this.plate = plate;
        this.capacity = capacity;
        this.status = status;
        this.driverId = driverId;
        this.routeId = routeId;
        this.currentPassengers = 0;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getBusCode() {
        return busCode;
    }

    public void setBusCode(String busCode) {
        this.busCode = busCode;
    }

    public String getPlate() {
        return plate;
    }

    public void setPlate(String plate) {
        this.plate = plate;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDriverId() {
        return driverId;
    }

    public void setDriverId(String driverId) {
        this.driverId = driverId;
    }

    public String getRouteId() {
        return routeId;
    }

    public void setRouteId(String routeId) {
        this.routeId = routeId;
    }

    public int getCurrentPassengers() {
        return currentPassengers;
    }

    public void setCurrentPassengers(int currentPassengers) {
        this.currentPassengers = currentPassengers;
    }

    public String getTripId() {
        return tripId;
    }

    public void setTripId(String tripId) {
        this.tripId = tripId;
    }
}
