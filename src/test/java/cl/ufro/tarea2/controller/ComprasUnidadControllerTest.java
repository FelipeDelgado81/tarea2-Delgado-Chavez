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

class ComprasUnidadControllerTest {

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
        ComprasUnidadController controlador = new ComprasUnidadController(servicio);
        mvc = MockMvcBuilders.standaloneSetup(controlador).build();
    }

    @Test
    void devuelveLasComprasUfConLosCamposExactosYEnOrden() throws Exception {
        when(cliente.obtenerValor("UF", LocalDate.of(2026, 9, 4)))
                .thenReturn(new BigDecimal("40000.25"));
        when(cliente.obtenerValor("UF", LocalDate.of(2026, 9, 1)))
                .thenReturn(new BigDecimal("40001.50"));

        String respuesta = mvc.perform(get("/compras/uf"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andReturn().getResponse().getContentAsString();

        String esperado = """
                {
                    "unidad": "UF",
                    "compras": [
                        {
                            "id": 3,
                            "producto": "Escritorio",
                            "monto": 8,
                            "fecha": "2026-09-04",
                            "valor_unidad": 40000.25,
                            "monto_pesos": 320002
                        },
                        {
                            "id": 1,
                            "producto": "Notebook",
                            "monto": 25.5,
                            "fecha": "2026-09-01",
                            "valor_unidad": 40001.50,
                            "monto_pesos": 1020038
                        }
                    ],
                    "total_pesos": 1340040
                }
                """;

        assertEquals(jsonMapper.readTree(esperado), jsonMapper.readTree(respuesta));
    }

    @Test
    void devuelveLasComprasUsdAlConsultarDolar() throws Exception {
        when(cliente.obtenerValor("USD", LocalDate.of(2026, 9, 2)))
                .thenReturn(new BigDecimal("900.10"));
        when(cliente.obtenerValor("USD", LocalDate.of(2026, 9, 8)))
                .thenReturn(new BigDecimal("901.20"));

        String respuesta = mvc.perform(get("/compras/dolar"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andReturn().getResponse().getContentAsString();

        String esperado = """
                {
                    "unidad": "USD",
                    "compras": [
                        {
                            "id": 2,
                            "producto": "Monitor",
                            "monto": 180,
                            "fecha": "2026-09-02",
                            "valor_unidad": 900.10,
                            "monto_pesos": 162018
                        },
                        {
                            "id": 4,
                            "producto": "Teclado mecanico",
                            "monto": 95.9,
                            "fecha": "2026-09-08",
                            "valor_unidad": 901.20,
                            "monto_pesos": 86425
                        }
                    ],
                    "total_pesos": 248443
                }
                """;

        assertEquals(jsonMapper.readTree(esperado), jsonMapper.readTree(respuesta));
    }

    @Test
    void devuelve400ParaUnaUnidadNoPermitida() throws Exception {
        mvc.perform(get("/compras/euro"))
                .andExpect(status().isBadRequest())
                .andExpect(content().json("{\"error\":\"La unidad debe ser uf o dolar.\"}"));
    }

    @Test
    void devuelve500SiNoSePuedeLeerElArchivo() throws Exception {
        Files.delete(archivo);

        mvc.perform(get("/compras/uf"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().json("{\"error\":\"No se pudo leer el archivo de compras.\"}"));
    }

    @Test
    void devuelve502SiFallaLaConsultaDelIndicador() throws Exception {
        when(cliente.obtenerValor("UF", LocalDate.of(2026, 9, 4)))
                .thenThrow(new RestClientException("El proveedor no responde."));

        mvc.perform(get("/compras/uf"))
                .andExpect(status().isBadGateway())
                .andExpect(content().json(
                        "{\"error\":\"No se pudo obtener el indicador para la fecha de compra.\"}"));
    }

    @Test
    void devuelveListaVaciaYTotalCeroSiElCsvNoTieneCompras() throws Exception {
        Files.writeString(archivo, "id,producto,monto,unidad,fecha\n");

        mvc.perform(get("/compras/uf"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"unidad\":\"UF\",\"compras\":[],\"total_pesos\":0}"));
    }
}
