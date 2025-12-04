package com.example.proyectointegrador.Models;

import java.util.Date;

public class Student {
    private String Id;
    private String FirstName;
    private String LastName;
    private String InstitutionalEmail;
    private String phone;
    private String PaswordHash;
    private String Role;
    private Date CreatedAt;

    public Student(String id, String firstName, String lastName, String institutionalEmail, String phone, String paswordHash, String role, Date createdAt) {
        Id = id;
        FirstName = firstName;
        LastName = lastName;
        InstitutionalEmail = institutionalEmail;
        this.phone = phone;
        PaswordHash = paswordHash;
        Role = role;
        CreatedAt = createdAt;
    }

    public String getId() {
        return Id;
    }

    public void setId(String id) {
        Id = id;
    }

    public String getFirstName() {
        return FirstName;
    }

    public void setFirstName(String firstName) {
        FirstName = firstName;
    }

    public String getLastName() {
        return LastName;
    }

    public void setLastName(String lastName) {
        LastName = lastName;
    }

    public String getInstitutionalEmail() {
        return InstitutionalEmail;
    }

    public void setInstitutionalEmail(String institutionalEmail) {
        InstitutionalEmail = institutionalEmail;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getPaswordHash() {
        return PaswordHash;
    }

    public void setPaswordHash(String paswordHash) {
        PaswordHash = paswordHash;
    }

    public String getRole() {
        return Role;
    }

    public void setRole(String role) {
        Role = role;
    }

    public Date getCreatedAt() {
        return CreatedAt;
    }

    public void setCreatedAt(Date createdAt) {
        CreatedAt = createdAt;
    }
}
