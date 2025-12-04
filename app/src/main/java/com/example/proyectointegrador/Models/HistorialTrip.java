package com.example.proyectointegrador.Models;

import java.util.Date;

public class HistorialTrip {
    private String codigoBus;
    private String nombreRuta;
    private Date horaInicio;
    private Date horaFin;

    public HistorialTrip(String codigoBus, String nombreRuta, Date horaInicio, Date horaFin) {
        this.codigoBus = codigoBus;
        this.nombreRuta = nombreRuta;
        this.horaInicio = horaInicio;
        this.horaFin = horaFin;
    }

    public String getCodigoBus() {
        return codigoBus;
    }

    public void setCodigoBus(String codigoBus) {
        this.codigoBus = codigoBus;
    }

    public String getNombreRuta() {
        return nombreRuta;
    }

    public void setNombreRuta(String nombreRuta) {
        this.nombreRuta = nombreRuta;
    }

    public Date getHoraInicio() {
        return horaInicio;
    }

    public void setHoraInicio(Date horaInicio) {
        this.horaInicio = horaInicio;
    }

    public Date getHoraFin() {
        return horaFin;
    }

    public void setHoraFin(Date horaFin) {
        this.horaFin = horaFin;
    }
}
