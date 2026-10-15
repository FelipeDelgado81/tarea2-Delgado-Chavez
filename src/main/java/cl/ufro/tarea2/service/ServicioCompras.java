package cl.ufro.tarea2.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import cl.ufro.tarea2.model.Compra;
import cl.ufro.tarea2.model.CompraConvertida;
import cl.ufro.tarea2.model.RespuestaCompras;

@Service
public class ServicioCompras {

    private LectorCompras lectorCompras;
    private ClienteIndicadores clienteIndicadores;

    public ServicioCompras(LectorCompras lectorCompras, ClienteIndicadores clienteIndicadores) {
        this.lectorCompras = lectorCompras;
        this.clienteIndicadores = clienteIndicadores;
    }

    public CompraConvertida convertirCompra(Compra compra) {
        BigDecimal valorUnidad = clienteIndicadores.obtenerValor(compra.getUnidad(), compra.getFecha());
        BigDecimal montoConvertido = compra.getMonto().multiply(valorUnidad);
        long montoPesos = montoConvertido.setScale(0, RoundingMode.HALF_UP).longValueExact();

        return new CompraConvertida(compra, valorUnidad, montoPesos);
    }

    public RespuestaCompras obtenerPorUnidad(String unidad) throws IOException {
        String unidadCsv;

        if ("uf".equals(unidad)) {
            unidadCsv = "UF";
        } else if ("dolar".equals(unidad)) {
            unidadCsv = "USD";
        } else {
            throw new IllegalArgumentException("La unidad debe ser uf o dolar.");
        }

        List<Compra> compras = lectorCompras.leerCompras();
        List<CompraConvertida> comprasConvertidas = new ArrayList<>();
        long totalPesos = 0;

        for (Compra compra : compras) {
            if (unidadCsv.equals(compra.getUnidad())) {
                CompraConvertida compraConvertida = convertirCompra(compra);
                comprasConvertidas.add(compraConvertida);
                totalPesos += compraConvertida.getMontoPesos();
            }
        }

        return new RespuestaCompras(unidadCsv, comprasConvertidas, totalPesos);
    }
}
