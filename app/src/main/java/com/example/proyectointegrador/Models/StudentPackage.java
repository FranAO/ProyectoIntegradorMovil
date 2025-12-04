package com.example.proyectointegrador.Models;

import java.util.Date;

public class StudentPackage {
    private String Id;
    private String StudentEmail;
    private String PackageId;
    private String PackageName;
    private int TotalTickets;
    private int RemainingTickets;
    private double Price;
    private Date PurchaseDate;
    private Date ExpirationDate;
    private String PaymentStatus; // "Pending", "Paid"
    private boolean IsActive;

    public StudentPackage(String id, String studentEmail, String packageId, String packageName,
                          int totalTickets, int remainingTickets, double price,
                          Date purchaseDate, Date expirationDate, String paymentStatus, boolean isActive) {
        Id = id;
        StudentEmail = studentEmail;
        PackageId = packageId;
        PackageName = packageName;
        TotalTickets = totalTickets;
        RemainingTickets = remainingTickets;
        Price = price;
        PurchaseDate = purchaseDate;
        ExpirationDate = expirationDate;
        PaymentStatus = paymentStatus;
        IsActive = isActive;
    }

    public String getId() {
        return Id;
    }

    public void setId(String id) {
        Id = id;
    }

    public String getStudentEmail() {
        return StudentEmail;
    }

    public void setStudentEmail(String studentEmail) {
        StudentEmail = studentEmail;
    }

    public String getPackageId() {
        return PackageId;
    }

    public void setPackageId(String packageId) {
        PackageId = packageId;
    }

    public String getPackageName() {
        return PackageName;
    }

    public void setPackageName(String packageName) {
        PackageName = packageName;
    }

    public int getTotalTickets() {
        return TotalTickets;
    }

    public void setTotalTickets(int totalTickets) {
        TotalTickets = totalTickets;
    }

    public int getRemainingTickets() {
        return RemainingTickets;
    }

    public void setRemainingTickets(int remainingTickets) {
        RemainingTickets = remainingTickets;
    }

    public double getPrice() {
        return Price;
    }

    public void setPrice(double price) {
        Price = price;
    }

    public Date getPurchaseDate() {
        return PurchaseDate;
    }

    public void setPurchaseDate(Date purchaseDate) {
        PurchaseDate = purchaseDate;
    }

    public Date getExpirationDate() {
        return ExpirationDate;
    }

    public void setExpirationDate(Date expirationDate) {
        ExpirationDate = expirationDate;
    }

    public String getPaymentStatus() {
        return PaymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        PaymentStatus = paymentStatus;
    }

    public boolean isActive() {
        return IsActive;
    }

    public void setActive(boolean active) {
        IsActive = active;
    }
}
