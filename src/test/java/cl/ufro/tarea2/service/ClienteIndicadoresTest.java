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
        String url = "http://127.0.0.1:" + servidor.getAddress().getPort();
        cliente = new ClienteIndicadores(url + "/uf-2026.json", url + "/dolar-2026.json");
    }

    @AfterEach
    void detenerServidor() {
        if (servidor != null) {
            servidor.stop(0);
        }
    }

    @Test
    void consultaLaUfEnLaFechaDeLaCompra() {
        responder("/uf-2026.json", 200,
                "{\"codigo\":\"uf\",\"nombre\":\"Unidad de fomento\",\"serie\":["
                + "{\"fecha\":\"2026-09-01T04:00:00.000Z\",\"valor\":40000.25}]}");

        BigDecimal valor = cliente.obtenerValor("UF", LocalDate.of(2026, 9, 1));

        assertEquals(new BigDecimal("40000.25"), valor);
    }

    @Test
    void consultaUsdComoDolarEnLaFechaDeLaCompra() {
        responder("/dolar-2026.json", 200,
                "{\"codigo\":\"dolar\",\"serie\":["
                + "{\"fecha\":\"2026-09-02T04:00:00.000Z\",\"valor\":950.50}]}");

        BigDecimal valor = cliente.obtenerValor("USD", LocalDate.of(2026, 9, 2));

        assertEquals(new BigDecimal("950.50"), valor);
    }

    @Test
    void informaSiNoHayDatosParaLaFecha() {
        responder("/uf-2026.json", 200, "{\"codigo\":\"uf\",\"serie\":[]}");

        assertThrows(RestClientException.class,
                () -> cliente.obtenerValor("UF", LocalDate.of(2026, 9, 1)));
    }

    @Test
    void rechazaUnValorDeOtraFecha() {
        responder("/uf-2026.json", 200,
                "{\"codigo\":\"uf\",\"serie\":["
                + "{\"fecha\":\"2026-08-31T04:00:00.000Z\",\"valor\":40000}]}");

        assertThrows(RestClientException.class,
                () -> cliente.obtenerValor("UF", LocalDate.of(2026, 9, 1)));
    }

    @Test
    void rechazaUnValorNulo() {
        responder("/uf-2026.json", 200,
                "{\"codigo\":\"uf\",\"serie\":["
                + "{\"fecha\":\"2026-09-01T04:00:00.000Z\",\"valor\":null}]}");

        assertThrows(RestClientException.class,
                () -> cliente.obtenerValor("UF", LocalDate.of(2026, 9, 1)));
    }

    @Test
    void informaSiLaApiFalla() {
        responder("/uf-2026.json", 503, "{}");

        assertThrows(RestClientException.class,
                () -> cliente.obtenerValor("UF", LocalDate.of(2026, 9, 1)));
    }

    @Test
    void rechazaUnaUnidadNoPermitida() {
        assertThrows(IllegalArgumentException.class,
                () -> cliente.obtenerValor("EUR", LocalDate.of(2026, 9, 1)));
    }

    @Test
    void buscaLaFechaExactaEntreVariosDias() {
        responder("/uf-2026.json", 200, """
                {"codigo":"uf","serie":[
                    {"fecha":"2026-09-02T04:00:00.000Z","valor":40002},
                    {"fecha":"2026-08-31T04:00:00.000Z","valor":40000},
                    {"fecha":"2026-09-01T04:00:00.000Z","valor":40001}
                ]}
                """);

        assertEquals(new BigDecimal("40001"),
                cliente.obtenerValor("UF", LocalDate.of(2026, 9, 1)));
    }

    @Test
    void usaElUltimoDiaHabilAnteriorParaElDolarSinDependerDelOrden() {
        responder("/dolar-2026.json", 200, """
                {"codigo":"dolar","serie":[
                    {"fecha":"2026-09-03T04:00:00.000Z","valor":920},
                    {"fecha":"2026-09-07T03:00:00.000Z","valor":940},
                    {"fecha":"2026-09-04T04:00:00.000Z","valor":930}
                ]}
                """);

        assertEquals(new BigDecimal("930"),
                cliente.obtenerValor("USD", LocalDate.of(2026, 9, 6)));
    }

    @Test
    void prefiereElDolarDeLaFechaExactaSiEstaDisponible() {
        responder("/dolar-2026.json", 200, """
                {"codigo":"dolar","serie":[
                    {"fecha":"2026-09-01T04:00:00.000Z","valor":930},
                    {"fecha":"2026-09-02T04:00:00.000Z","valor":940}
                ]}
                """);

        assertEquals(new BigDecimal("940"),
                cliente.obtenerValor("USD", LocalDate.of(2026, 9, 2)));
    }

    @Test
    void usaElDiaAnteriorDisponibleCuandoHayUnFeriado() {
        responder("/dolar-2026.json", 200, """
                {"codigo":"dolar","serie":[
                    {"fecha":"2026-09-17T03:00:00.000Z","valor":930},
                    {"fecha":"2026-09-21T03:00:00.000Z","valor":940}
                ]}
                """);

        assertEquals(new BigDecimal("930"),
                cliente.obtenerValor("USD", LocalDate.of(2026, 9, 18)));
    }

    @Test
    void noUsaUnDolarDeUnaFechaFutura() {
        responder("/dolar-2026.json", 200, """
                {"codigo":"dolar","serie":[
                    {"fecha":"2026-09-07T03:00:00.000Z","valor":940}
                ]}
                """);

        assertThrows(RestClientException.class,
                () -> cliente.obtenerValor("USD", LocalDate.of(2026, 9, 6)));
    }

    @Test
    void informaSiElTextoDescargadoNoEsJsonValido() {
        responder("/uf-2026.json", 200, "Servicio no disponible");

        assertThrows(RestClientException.class,
                () -> cliente.obtenerValor("UF", LocalDate.of(2026, 9, 1)));
    }

    private void responder(String ruta, int estado, String json) {
        servidor.createContext(ruta, intercambio -> {
            byte[] contenido = json.getBytes(StandardCharsets.UTF_8);
            intercambio.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
            intercambio.sendResponseHeaders(estado, contenido.length);
            try (OutputStream salida = intercambio.getResponseBody()) {
                salida.write(contenido);
            }
        });
    }
}
