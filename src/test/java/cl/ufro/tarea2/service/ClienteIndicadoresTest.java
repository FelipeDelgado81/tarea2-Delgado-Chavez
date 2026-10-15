package cl.ufro.tarea2.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClientException;

import com.sun.net.httpserver.HttpServer;

class ClienteIndicadoresTest {

    private HttpServer servidor;
    private ClienteIndicadores cliente;

    @BeforeEach
    void iniciarServidor() throws IOException {
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.start();
        String url = "http://127.0.0.1:" + servidor.getAddress().getPort() + "/api";
        cliente = new ClienteIndicadores(url);
    }

    @AfterEach
    void detenerServidor() {
        if (servidor != null) {
            servidor.stop(0);
        }
    }

    @Test
    void consultaLaUfEnLaFechaDeLaCompra() {
        responder("/api/uf/01-09-2026", 200,
                "{\"codigo\":\"uf\",\"nombre\":\"Unidad de fomento\",\"serie\":["
                + "{\"fecha\":\"2026-09-01T04:00:00.000Z\",\"valor\":40000.25}]}");

        BigDecimal valor = cliente.obtenerValor("UF", LocalDate.of(2026, 9, 1));

        assertEquals(new BigDecimal("40000.25"), valor);
    }

    @Test
    void consultaUsdComoDolarEnLaFechaDeLaCompra() {
        responder("/api/dolar/02-09-2026", 200,
                "{\"codigo\":\"dolar\",\"serie\":["
                + "{\"fecha\":\"2026-09-02T04:00:00.000Z\",\"valor\":950.50}]}");

        BigDecimal valor = cliente.obtenerValor("USD", LocalDate.of(2026, 9, 2));

        assertEquals(new BigDecimal("950.50"), valor);
    }

    @Test
    void informaSiNoHayDatosParaLaFecha() {
        responder("/api/uf/01-09-2026", 200, "{\"codigo\":\"uf\",\"serie\":[]}");

        assertThrows(RestClientException.class,
                () -> cliente.obtenerValor("UF", LocalDate.of(2026, 9, 1)));
    }

    @Test
    void rechazaUnValorDeOtraFecha() {
        responder("/api/uf/01-09-2026", 200,
                "{\"codigo\":\"uf\",\"serie\":["
                + "{\"fecha\":\"2026-08-31T04:00:00.000Z\",\"valor\":40000}]}");

        assertThrows(RestClientException.class,
                () -> cliente.obtenerValor("UF", LocalDate.of(2026, 9, 1)));
    }

    @Test
    void rechazaUnValorNulo() {
        responder("/api/uf/01-09-2026", 200,
                "{\"codigo\":\"uf\",\"serie\":["
                + "{\"fecha\":\"2026-09-01T04:00:00.000Z\",\"valor\":null}]}");

        assertThrows(RestClientException.class,
                () -> cliente.obtenerValor("UF", LocalDate.of(2026, 9, 1)));
    }

    @Test
    void informaSiLaApiFalla() {
        responder("/api/uf/01-09-2026", 503, "{}");

        assertThrows(RestClientException.class,
                () -> cliente.obtenerValor("UF", LocalDate.of(2026, 9, 1)));
    }

    @Test
    void rechazaUnaUnidadNoPermitida() {
        assertThrows(IllegalArgumentException.class,
                () -> cliente.obtenerValor("EUR", LocalDate.of(2026, 9, 1)));
    }

    private void responder(String ruta, int estado, String json) {
        servidor.createContext(ruta, intercambio -> {
            byte[] contenido = json.getBytes(StandardCharsets.UTF_8);
            intercambio.getResponseHeaders().set("Content-Type", "application/json");
            intercambio.sendResponseHeaders(estado, contenido.length);
            try (OutputStream salida = intercambio.getResponseBody()) {
                salida.write(contenido);
            }
        });
    }
}
