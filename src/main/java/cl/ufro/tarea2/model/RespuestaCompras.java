package cl.ufro.tarea2.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class RespuestaCompras {

    private String unidad;
    private List<CompraConvertida> compras;
    private long totalPesos;

    public RespuestaCompras(String unidad, List<CompraConvertida> compras, long totalPesos) {
        this.unidad = unidad;
        this.compras = compras;
        this.totalPesos = totalPesos;
    }

    public String getUnidad() {
        return unidad;
    }

    public List<CompraConvertida> getCompras() {
        return compras;
    }

    @JsonProperty("total_pesos")
    public long getTotalPesos() {
        return totalPesos;
    }
}
