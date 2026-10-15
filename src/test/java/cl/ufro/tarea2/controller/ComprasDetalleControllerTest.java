package cl.ufro.tarea2.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.RestClientException;

import cl.ufro.tarea2.service.ClienteIndicadores;
import cl.ufro.tarea2.service.LectorCompras;
import cl.ufro.tarea2.service.ServicioCompras;
import tools.jackson.databind.json.JsonMapper;

class ComprasDetalleControllerTest {

    @TempDir
    Path carpetaTemporal;

    private Path archivo;
    private ClienteIndicadores cliente;
    private MockMvc mvc;
    private JsonMapper jsonMapper = new JsonMapper();

    @BeforeEach
    void prepararControlador() throws IOException {
        archivo = carpetaTemporal.resolve("compras.csv");
        Files.writeString(archivo, "id,producto,monto,unidad,fecha\n"
                + "3,Escritorio,8,UF,2026-09-04\n"
                + "2,Monitor,180,USD,2026-09-02\n"
                + "1,Notebook,25.5,UF,2026-09-01\n"
                + "4,Teclado mecanico,95.9,USD,2026-09-08\n");

        cliente = mock(ClienteIndicadores.class);
        LectorCompras lector = new LectorCompras(archivo.toString());
        ServicioCompras servicio = new ServicioCompras(lector, cliente);
        ComprasDetalleController controlador = new ComprasDetalleController(servicio);
        mvc = MockMvcBuilders.standaloneSetup(controlador).build();
    }

    @Test
    void devuelveLaCompraConvertidaConLosCamposExactos() throws Exception {
        when(cliente.obtenerValor("UF", LocalDate.of(2026, 9, 4)))
                .thenReturn(new BigDecimal("40000.25"));

        String respuesta = mvc.perform(get("/compras/detalle/3"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andReturn().getResponse().getContentAsString();

        String esperado = """
                {
                    "id": 3,
                    "producto": "Escritorio",
                    "monto": 8,
                    "unidad": "UF",
                    "fecha": "2026-09-04",
                    "valor_unidad": 40000.25,
                    "monto_pesos": 320002
                }
                """;

        assertEquals(jsonMapper.readTree(esperado), jsonMapper.readTree(respuesta));
    }

    @Test
    void convierteElDetalleUsandoElIndicadorDeSuFecha() throws Exception {
        when(cliente.obtenerValor("USD", LocalDate.of(2026, 9, 8)))
                .thenReturn(new BigDecimal("950.50"));

        String respuesta = mvc.perform(get("/compras/detalle/4"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String esperado = """
                {
                    "id": 4,
                    "producto": "Teclado mecanico",
                    "monto": 95.9,
                    "unidad": "USD",
                    "fecha": "2026-09-08",
                    "valor_unidad": 950.50,
                    "monto_pesos": 91153
                }
                """;

        assertEquals(jsonMapper.readTree(esperado), jsonMapper.readTree(respuesta));
    }

    @Test
    void devuelve404SiLaCompraNoExiste() throws Exception {
        mvc.perform(get("/compras/detalle/99"))
                .andExpect(status().isNotFound())
                .andExpect(content().json("{\"error\":\"No existe una compra con id 99.\"}"));
    }

    @Test
    void devuelve500SiNoSePuedeLeerElArchivo() throws Exception {
        Files.delete(archivo);

        mvc.perform(get("/compras/detalle/3"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().json("{\"error\":\"No se pudo leer el archivo de compras.\"}"));
    }

    @Test
    void devuelve502SiFallaLaConsultaDelIndicador() throws Exception {
        when(cliente.obtenerValor("UF", LocalDate.of(2026, 9, 4)))
                .thenThrow(new RestClientException("El proveedor no responde."));

        mvc.perform(get("/compras/detalle/3"))
                .andExpect(status().isBadGateway())
                .andExpect(content().json(
                        "{\"error\":\"No se pudo obtener el indicador para la fecha de compra.\"}"));
    }
}
