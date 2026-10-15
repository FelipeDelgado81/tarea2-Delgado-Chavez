package cl.ufro.tarea2.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClientException;

import cl.ufro.tarea2.model.Compra;
import cl.ufro.tarea2.model.CompraConvertida;
import cl.ufro.tarea2.model.CompraDetalle;
import cl.ufro.tarea2.model.RespuestaCompras;

class ServicioComprasTest {

    private LectorCompras lector;
    private ClienteIndicadores cliente;
    private ServicioCompras servicio;

    @BeforeEach
    void prepararServicio() {
        lector = mock(LectorCompras.class);
        cliente = mock(ClienteIndicadores.class);
        servicio = new ServicioCompras(lector, cliente);
    }

    @Test
    void filtraUfConservaElOrdenYTotalizaLosMontosRedondeados() throws IOException {
        Compra primera = new Compra(3, "Escritorio", new BigDecimal("1"), "UF", LocalDate.of(2026, 9, 4));
        Compra segunda = new Compra(2, "Monitor", new BigDecimal("180"), "USD", LocalDate.of(2026, 9, 2));
        Compra tercera = new Compra(1, "Notebook", new BigDecimal("1"), "UF", LocalDate.of(2026, 9, 1));
        when(lector.leerCompras()).thenReturn(List.of(primera, segunda, tercera));
        when(cliente.obtenerValor("UF", primera.getFecha())).thenReturn(new BigDecimal("10.49"));
        when(cliente.obtenerValor("UF", tercera.getFecha())).thenReturn(new BigDecimal("10.50"));

        RespuestaCompras respuesta = servicio.obtenerPorUnidad("uf");

        assertEquals("UF", respuesta.getUnidad());
        assertEquals(2, respuesta.getCompras().size());
        assertEquals(3, respuesta.getCompras().get(0).getId());
        assertEquals(1, respuesta.getCompras().get(1).getId());
        assertEquals(10, respuesta.getCompras().get(0).getMontoPesos());
        assertEquals(11, respuesta.getCompras().get(1).getMontoPesos());
        assertEquals(21, respuesta.getTotalPesos());
    }

    @Test
    void convierteComprasEnUsdAlConsultarDolar() throws IOException {
        Compra compra = new Compra(4, "Teclado mecanico", new BigDecimal("95.9"), "USD", LocalDate.of(2026, 9, 8));
        when(lector.leerCompras()).thenReturn(List.of(compra));
        when(cliente.obtenerValor("USD", compra.getFecha())).thenReturn(new BigDecimal("950.50"));

        RespuestaCompras respuesta = servicio.obtenerPorUnidad("dolar");

        assertEquals("USD", respuesta.getUnidad());
        assertEquals(1, respuesta.getCompras().size());
        assertEquals(new BigDecimal("950.50"), respuesta.getCompras().get(0).getValorUnidad());
        assertEquals(91153, respuesta.getCompras().get(0).getMontoPesos());
        assertEquals(91153, respuesta.getTotalPesos());
    }

    @Test
    void devuelveListaVaciaYTotalCeroSiNoHayComprasDeLaUnidad() throws IOException {
        Compra compra = new Compra(2, "Monitor", new BigDecimal("180"), "USD", LocalDate.of(2026, 9, 2));
        when(lector.leerCompras()).thenReturn(List.of(compra));

        RespuestaCompras respuesta = servicio.obtenerPorUnidad("uf");

        assertEquals("UF", respuesta.getUnidad());
        assertEquals(0, respuesta.getCompras().size());
        assertEquals(0, respuesta.getTotalPesos());
    }

    @Test
    void rechazaUnaUnidadNoPermitida() {
        assertThrows(IllegalArgumentException.class, () -> servicio.obtenerPorUnidad("euro"));
    }

    @Test
    void permiteConvertirUnaCompraIndividual() {
        Compra compra = new Compra(1, "Notebook", new BigDecimal("25.5"), "UF", LocalDate.of(2026, 9, 1));
        when(cliente.obtenerValor("UF", compra.getFecha())).thenReturn(new BigDecimal("40000.25"));

        CompraConvertida resultado = servicio.convertirCompra(compra);

        assertEquals(1, resultado.getId());
        assertEquals("Notebook", resultado.getProducto());
        assertEquals(new BigDecimal("25.5"), resultado.getMonto());
        assertEquals(compra.getFecha(), resultado.getFecha());
        assertEquals(new BigDecimal("40000.25"), resultado.getValorUnidad());
        assertEquals(1020006, resultado.getMontoPesos());
    }

    @Test
    void devuelveLaCompraConvertidaConSuUnidadAlBuscarDetallePorId() throws IOException {
        Compra primera = new Compra(3, "Escritorio", new BigDecimal("8"), "UF", LocalDate.of(2026, 9, 4));
        Compra segunda = new Compra(1, "Notebook", new BigDecimal("25.5"), "UF", LocalDate.of(2026, 9, 1));
        when(lector.leerCompras()).thenReturn(List.of(primera, segunda));
        when(cliente.obtenerValor("UF", segunda.getFecha())).thenReturn(new BigDecimal("40000.25"));

        CompraDetalle resultado = servicio.obtenerDetalle(1);

        assertEquals(1, resultado.getId());
        assertEquals("Notebook", resultado.getProducto());
        assertEquals(new BigDecimal("25.5"), resultado.getMonto());
        assertEquals("UF", resultado.getUnidad());
        assertEquals(LocalDate.of(2026, 9, 1), resultado.getFecha());
        assertEquals(new BigDecimal("40000.25"), resultado.getValorUnidad());
        assertEquals(1020006, resultado.getMontoPesos());
    }

    @Test
    void informaLaUnidadEnDolarDelDetalleDeUnaCompraUsd() throws IOException {
        Compra compra = new Compra(2, "Monitor", new BigDecimal("180"), "USD", LocalDate.of(2026, 9, 2));
        when(lector.leerCompras()).thenReturn(List.of(compra));
        when(cliente.obtenerValor("USD", compra.getFecha())).thenReturn(new BigDecimal("900.10"));

        CompraDetalle resultado = servicio.obtenerDetalle(2);

        assertEquals("USD", resultado.getUnidad());
        assertEquals(162018, resultado.getMontoPesos());
    }

    @Test
    void rechazaElDetalleDeUnaCompraInexistente() throws IOException {
        Compra compra = new Compra(1, "Notebook", new BigDecimal("25.5"), "UF", LocalDate.of(2026, 9, 1));
        when(lector.leerCompras()).thenReturn(List.of(compra));

        assertThrows(NoSuchElementException.class, () -> servicio.obtenerDetalle(99));
    }

    @Test
    void informaElErrorSiFallaLaConsultaDelIndicadorDelDetalle() throws IOException {
        Compra compra = new Compra(1, "Notebook", new BigDecimal("25.5"), "UF", LocalDate.of(2026, 9, 1));
        when(lector.leerCompras()).thenReturn(List.of(compra));
        when(cliente.obtenerValor("UF", compra.getFecha()))
                .thenThrow(new RestClientException("No hay datos para la fecha."));

        assertThrows(RestClientException.class, () -> servicio.obtenerDetalle(1));
    }

    @Test
    void informaElErrorSiFallaLaConsultaDelIndicador() throws IOException {
        Compra compra = new Compra(1, "Notebook", new BigDecimal("25.5"), "UF", LocalDate.of(2026, 9, 1));
        when(lector.leerCompras()).thenReturn(List.of(compra));
        when(cliente.obtenerValor("UF", compra.getFecha()))
                .thenThrow(new RestClientException("No hay datos para la fecha."));

        assertThrows(RestClientException.class, () -> servicio.obtenerPorUnidad("uf"));
    }
}
