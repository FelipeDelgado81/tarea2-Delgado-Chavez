package cl.ufro.tarea2.model;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CompraDetalle {

    private int id;
    private String producto;
    private BigDecimal monto;
    private String unidad;
    private LocalDate fecha;
    private BigDecimal valorUnidad;
    private long montoPesos;

    public CompraDetalle(Compra compra, BigDecimal valorUnidad, long montoPesos) {
        this.id = compra.getId();
        this.producto = compra.getProducto();
        this.monto = compra.getMonto();
        this.unidad = compra.getUnidad();
        this.fecha = compra.getFecha();
        this.valorUnidad = valorUnidad;
        this.montoPesos = montoPesos;
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

    @JsonProperty("valor_unidad")
    public BigDecimal getValorUnidad() {
        return valorUnidad;
    }

    @JsonProperty("monto_pesos")
    public long getMontoPesos() {
        return montoPesos;
    }
}
