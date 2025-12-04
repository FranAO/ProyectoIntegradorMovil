package com.example.proyectointegrador.Models;

public class Package {
    private String Id;
    private String Name;
    private String Description;
    private int TicketCount;
    private double Price;
    private int DurationDays;
    private boolean Active;

    public Package(String id, String name, String description, int ticketCount, double price, int durationDays, boolean active) {
        Id = id;
        Name = name;
        Description = description;
        TicketCount = ticketCount;
        Price = price;
        DurationDays = durationDays;
        Active = active;
    }

    public String getId() {
        return Id;
    }

    public void setId(String id) {
        Id = id;
    }

    public String getName() {
        return Name;
    }

    public void setName(String name) {
        Name = name;
    }

    public String getDescription() {
        return Description;
    }

    public void setDescription(String description) {
        Description = description;
    }

    public int getTicketCount() {
        return TicketCount;
    }

    public void setTicketCount(int ticketCount) {
        TicketCount = ticketCount;
    }

    public double getPrice() {
        return Price;
    }

    public void setPrice(double price) {
        Price = price;
    }

    public int getDurationDays() {
        return DurationDays;
    }

    public void setDurationDays(int durationDays) {
        DurationDays = durationDays;
    }

    public boolean isActive() {
        return Active;
    }

    public void setActive(boolean active) {
        Active = active;
    }
}
