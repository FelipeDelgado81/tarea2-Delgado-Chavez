package cl.ufro.tarea2.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import cl.ufro.tarea2.model.RespuestaIndicador;
import cl.ufro.tarea2.model.ValorIndicador;

@Service
public class ClienteIndicadores {

    private RestClient cliente;

    public ClienteIndicadores(@Value("${indicadores.url}") String url) {
        SimpleClientHttpRequestFactory conexion = new SimpleClientHttpRequestFactory();
        conexion.setConnectTimeout(5000);
        conexion.setReadTimeout(5000);

        cliente = RestClient.builder()
                .baseUrl(url)
                .requestFactory(conexion)
                .build();
    }

    public BigDecimal obtenerValor(String unidad, LocalDate fecha) {
        String indicador;

        if ("UF".equals(unidad)) {
            indicador = "uf";
        } else if ("USD".equals(unidad)) {
            indicador = "dolar";
        } else {
            throw new IllegalArgumentException("La unidad debe ser UF o USD");
        }

        String fechaConsulta = fecha.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));

        RespuestaIndicador respuesta = cliente.get()
                .uri("/{indicador}/{fecha}", indicador, fechaConsulta)
                .retrieve()
                .body(RespuestaIndicador.class);

        if (respuesta == null || !indicador.equals(respuesta.getCodigo())) {
            throw new RestClientException("La API no devolvio el indicador solicitado.");
        }

        if (respuesta.getSerie() == null || respuesta.getSerie().isEmpty()) {
            throw new RestClientException("No hay un valor de " + unidad + " para la fecha " + fecha + ".");
        }

        for (ValorIndicador dato : respuesta.getSerie()) {
            if (dato != null && dato.getFecha() != null
                    && dato.getFecha().startsWith(fecha.toString() + "T")) {
                if (dato.getValor() == null || dato.getValor().compareTo(BigDecimal.ZERO) <= 0) {
                    throw new RestClientException("El valor del indicador no es valido.");
                }
                return dato.getValor();
            }
        }

        throw new RestClientException("La API no devolvio un valor para la fecha " + fecha + ".");
    }
}
