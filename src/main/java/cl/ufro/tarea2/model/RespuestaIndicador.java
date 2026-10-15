package cl.ufro.tarea2.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RespuestaIndicador {

    private String codigo;
    private List<ValorIndicador> serie;

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public List<ValorIndicador> getSerie() {
        return serie;
    }

    public void setSerie(List<ValorIndicador> serie) {
        this.serie = serie;
    }
}
