package com.example.proyectointegrador.Models;

public class Passenger {
    private String id;
    private String name;
    private String ticketStatus; // "pending", "confirmed", "cancelled"

    public Passenger(String id, String name, String ticketStatus) {
        this.id = id;
        this.name = name;
        this.ticketStatus = ticketStatus;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTicketStatus() {
        return ticketStatus;
    }

    public void setTicketStatus(String ticketStatus) {
        this.ticketStatus = ticketStatus;
    }
}
