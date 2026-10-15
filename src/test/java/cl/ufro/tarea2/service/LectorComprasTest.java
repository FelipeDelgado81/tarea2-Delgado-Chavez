package cl.ufro.tarea2.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import cl.ufro.tarea2.model.Compra;

class LectorComprasTest {

    @TempDir
    Path carpetaTemporal;

    @Test
    void leeArchivoExternoYConservaElOrden() throws IOException {
        Path archivo = carpetaTemporal.resolve("compras.csv");
        Files.writeString(archivo, "id,producto,monto,unidad,fecha\n"
                + "3,Escritorio,8,UF,2026-09-04\n"
                + "1,Notebook,25.5,UF,2026-09-01\n");

        LectorCompras lector = new LectorCompras(archivo.toString());
        List<Compra> compras = lector.leerCompras();

        assertEquals(2, compras.size());
        assertEquals(3, compras.get(0).getId());
        assertEquals(1, compras.get(1).getId());
        assertEquals("Notebook", compras.get(1).getProducto());
        assertEquals(new BigDecimal("25.5"), compras.get(1).getMonto());
        assertEquals("UF", compras.get(1).getUnidad());
        assertEquals(LocalDate.of(2026, 9, 1), compras.get(1).getFecha());
    }

    @Test
    void leeLosCambiosDelArchivoEnLaSiguienteConsulta() throws IOException {
        Path archivo = carpetaTemporal.resolve("compras.csv");
        Files.writeString(archivo, "id,producto,monto,unidad,fecha\n"
                + "1,Notebook,25.5,UF,2026-09-01\n");
        LectorCompras lector = new LectorCompras(archivo.toString());
        assertEquals("Notebook", lector.leerCompras().get(0).getProducto());

        Files.writeString(archivo, "id,producto,monto,unidad,fecha\n"
                + "2,Monitor,180,USD,2026-09-02\n");

        assertEquals("Monitor", lector.leerCompras().get(0).getProducto());
    }

    @Test
    void informaSiElArchivoNoExiste() {
        Path archivo = carpetaTemporal.resolve("no-existe.csv");
        LectorCompras lector = new LectorCompras(archivo.toString());

        assertThrows(IOException.class, () -> lector.leerCompras());
    }

    @Test
    void informaSiUnaFilaEstaIncompleta() throws IOException {
        Path archivo = carpetaTemporal.resolve("compras.csv");
        Files.writeString(archivo, "id,producto,monto,unidad,fecha\n"
                + "1,Notebook,25.5,UF\n");
        LectorCompras lector = new LectorCompras(archivo.toString());

        IOException error = assertThrows(IOException.class, () -> lector.leerCompras());

        assertEquals("La linea 2 del CSV debe tener cinco columnas.", error.getMessage());
    }
}
