package cl.ufro.tarea2.controller;

import java.io.IOException;
import java.util.Map;
import java.util.NoSuchElementException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;

import cl.ufro.tarea2.model.CompraConvertida;
import cl.ufro.tarea2.service.ServicioCompras;

@RestController
@RequestMapping("/compras")
public class ComprasDetalleController {

    private ServicioCompras servicioCompras;

    public ComprasDetalleController(ServicioCompras servicioCompras) {
        this.servicioCompras = servicioCompras;
    }

    @GetMapping("/detalle/{id}")
    public ResponseEntity<?> obtenerDetalle(@PathVariable int id) {
        try {
            CompraConvertida compra = servicioCompras.obtenerDetalle(id);
            return ResponseEntity.ok(compra);
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "No se pudo leer el archivo de compras."));
        } catch (RestClientException e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(Map.of("error", "No se pudo obtener el indicador para la fecha de compra."));
        }
    }
}
