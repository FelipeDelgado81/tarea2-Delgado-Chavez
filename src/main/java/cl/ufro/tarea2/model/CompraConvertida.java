package cl.ufro.tarea2.model;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CompraConvertida {

    private int id;
    private String producto;
    private BigDecimal monto;
    private LocalDate fecha;
    private BigDecimal valorUnidad;
    private long montoPesos;

    public CompraConvertida(Compra compra, BigDecimal valorUnidad, long montoPesos) {
        this.id = compra.getId();
        this.producto = compra.getProducto();
        this.monto = compra.getMonto();
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
