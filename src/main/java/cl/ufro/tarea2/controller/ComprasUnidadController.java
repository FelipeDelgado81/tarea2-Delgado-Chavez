package cl.ufro.tarea2.controller;

import java.io.IOException;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;

import cl.ufro.tarea2.model.RespuestaCompras;
import cl.ufro.tarea2.service.ServicioCompras;

@RestController
@RequestMapping("/compras")
public class ComprasUnidadController {

    private ServicioCompras servicioCompras;

    public ComprasUnidadController(ServicioCompras servicioCompras) {
        this.servicioCompras = servicioCompras;
    }

    @GetMapping("/{unidad}")
    public ResponseEntity<?> obtenerPorUnidad(@PathVariable String unidad) {
        try {
            RespuestaCompras respuesta = servicioCompras.obtenerPorUnidad(unidad);
            return ResponseEntity.ok(respuesta);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
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
