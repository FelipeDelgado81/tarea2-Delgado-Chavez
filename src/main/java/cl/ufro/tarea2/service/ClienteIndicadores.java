package cl.ufro.tarea2.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import cl.ufro.tarea2.model.RespuestaIndicador;
import cl.ufro.tarea2.model.ValorIndicador;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

@Service
public class ClienteIndicadores {

    private RestClient cliente;
    private ObjectMapper objectMapper = new JsonMapper();
    private String urlUf;
    private String urlDolar;

    public ClienteIndicadores(@Value("${indicadores.uf-url}") String urlUf,
            @Value("${indicadores.dolar-url}") String urlDolar) {
        this.urlUf = urlUf;
        this.urlDolar = urlDolar;

        SimpleClientHttpRequestFactory conexion = new SimpleClientHttpRequestFactory();
        conexion.setConnectTimeout(5000);
        conexion.setReadTimeout(5000);
        cliente = RestClient.builder().requestFactory(conexion).build();
    }

    public BigDecimal obtenerValor(String unidad, LocalDate fecha) {
        String indicador;
        String url;

        if ("UF".equals(unidad)) {
            indicador = "uf";
            url = urlUf;
        } else if ("USD".equals(unidad)) {
            indicador = "dolar";
            url = urlDolar;
        } else {
            throw new IllegalArgumentException("La unidad debe ser UF o USD.");
        }

        String contenido = cliente.get().uri(url).retrieve().body(String.class);
        if (contenido == null || contenido.isBlank()) {
            throw new RestClientException("El archivo de indicadores esta vacio.");
        }

        RespuestaIndicador respuesta;
        try {
            respuesta = objectMapper.readValue(contenido, RespuestaIndicador.class);
        } catch (JacksonException e) {
            throw new RestClientException("El archivo de indicadores no contiene un JSON valido.", e);
        }

        if (respuesta == null || !indicador.equals(respuesta.getCodigo())) {
            throw new RestClientException("El archivo no contiene el indicador solicitado.");
        }
        if (respuesta.getSerie() == null || respuesta.getSerie().isEmpty()) {
            throw new RestClientException("No hay datos disponibles para " + unidad + ".");
        }

        ValorIndicador elegido = null;
        LocalDate fechaElegida = null;

        for (ValorIndicador dato : respuesta.getSerie()) {
            if (dato == null || dato.getFecha() == null || dato.getFecha().length() < 10) {
                continue;
            }

            LocalDate fechaDato;
            try {
                fechaDato = LocalDate.parse(dato.getFecha().substring(0, 10));
            } catch (DateTimeParseException e) {
                throw new RestClientException("El archivo contiene una fecha no valida.", e);
            }

            if (fechaDato.equals(fecha)) {
                elegido = dato;
                break;
            }

            if ("USD".equals(unidad) && fechaDato.isBefore(fecha)
                    && (fechaElegida == null || fechaDato.isAfter(fechaElegida))) {
                elegido = dato;
                fechaElegida = fechaDato;
            }
        }

        if (elegido == null) {
            throw new RestClientException("No hay un valor de " + unidad + " para la fecha " + fecha + ".");
        }
        if (elegido.getValor() == null || elegido.getValor().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RestClientException("El valor del indicador no es valido.");
        }
        return elegido.getValor();
    }
}
