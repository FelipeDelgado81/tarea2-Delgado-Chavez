package cl.ufro.tarea2.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import cl.ufro.tarea2.model.Compra;

@Service
public class LectorCompras {

    private String rutaCsv;

    public LectorCompras(@Value("${compras.csv.path}") String rutaCsv) {
        this.rutaCsv = rutaCsv;
    }

    public List<Compra> leerCompras() throws IOException {
        List<Compra> compras = new ArrayList<>();

        try (BufferedReader lector = Files.newBufferedReader(Path.of(rutaCsv))) {
            String encabezado = lector.readLine();
            if (!"id,producto,monto,unidad,fecha".equals(encabezado)) {
                throw new IOException("El encabezado del CSV debe ser id,producto,monto,unidad,fecha.");
            }

            String linea;
            int numeroLinea = 1;

            while ((linea = lector.readLine()) != null) {
                numeroLinea++;
                if (linea.isBlank()) {
                    continue;
                }

                String[] datos = linea.split(",", -1);
                if (datos.length != 5) {
                    throw new IOException("La linea " + numeroLinea + " del CSV debe tener cinco columnas.");
                }

                try {
                    int id = Integer.parseInt(datos[0].trim());
                    String producto = datos[1].trim();
                    BigDecimal monto = new BigDecimal(datos[2].trim());
                    String unidad = datos[3].trim();
                    LocalDate fecha = LocalDate.parse(datos[4].trim());

                    Compra compra = new Compra(id, producto, monto, unidad, fecha);
                    compras.add(compra);
                } catch (NumberFormatException | DateTimeParseException e) {
                    throw new IOException("Datos invalidos en la linea " + numeroLinea + " del CSV.", e);
                }
            }
        }

        return compras;
    }
}
