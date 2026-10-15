package cl.ufro.tarea2.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Compra {

    private int id;
    private String producto;
    private BigDecimal monto;
    private String unidad;
    private LocalDate fecha;

    public Compra(int id, String producto, BigDecimal monto, String unidad, LocalDate fecha) {
        this.id = id;
        this.producto = producto;
        this.monto = monto;
        this.unidad = unidad;
        this.fecha = fecha;
    }

    public int getId() {
        return id;
    }

    public String getProducto() {
        return producto;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public String getUnidad() {
        return unidad;
    }

    public LocalDate getFecha() {
        return fecha;
    }
}
