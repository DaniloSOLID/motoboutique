package com.motoboutique.compras.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.motoboutique.catalogo.domain.Producto;
import com.motoboutique.compras.domain.OrdenCompra;
import com.motoboutique.compras.domain.OrdenCompraDetalle;
import com.motoboutique.compras.domain.Proveedor;
import com.motoboutique.compras.domain.RecepcionCompra;
import com.motoboutique.compras.dto.RecepcionDetalleSolicitudDto;
import com.motoboutique.compras.dto.RecepcionGuardarDto;
import com.motoboutique.compras.repository.OrdenCompraDetalleRepository;
import com.motoboutique.compras.repository.OrdenCompraRepository;
import com.motoboutique.compras.repository.RecepcionCompraRepository;
import com.motoboutique.compras.repository.RecepcionDetalleRepository;
import com.motoboutique.inventario.domain.Almacen;
import com.motoboutique.inventario.repository.NumeracionDocumentoRepository;
import com.motoboutique.inventario.service.MovimientoInventarioService;
import com.motoboutique.shared.exception.NegocioException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class RecepcionCompraServiceTest {
    @Mock private OrdenCompraRepository ordenes;
    @Mock private OrdenCompraDetalleRepository ordenDetalles;
    @Mock private RecepcionCompraRepository recepciones;
    @Mock private RecepcionDetalleRepository recepcionDetalles;
    @Mock private NumeracionDocumentoRepository numeracion;
    @Mock private MovimientoInventarioService movimientos;
    @InjectMocks private RecepcionCompraService servicio;

    private OrdenCompra orden;
    private OrdenCompraDetalle detalle;

    @BeforeEach
    void preparar() {
        Proveedor proveedor = new Proveedor();
        proveedor.setRazonSocial("Proveedor");
        Almacen almacen = new Almacen();
        ReflectionTestUtils.setField(almacen, "id", 5L);
        almacen.setNombre("Principal");
        orden = new OrdenCompra();
        ReflectionTestUtils.setField(orden, "id", 1L);
        orden.setNumero("OC-000001");
        orden.setEstado("CONFIRMADA");
        orden.setProveedor(proveedor);
        orden.setAlmacen(almacen);
        Producto producto = new Producto();
        ReflectionTestUtils.setField(producto, "id", 2L);
        producto.setCodigo("P-2");
        producto.setNombre("Producto");
        detalle = new OrdenCompraDetalle();
        ReflectionTestUtils.setField(detalle, "id", 3L);
        detalle.setOrden(orden);
        detalle.setProducto(producto);
        detalle.setCantidad(new BigDecimal("5.000"));
        detalle.setCantidadRecibida(new BigDecimal("1.000"));
        detalle.setCostoUnitario(new BigDecimal("12.50"));
        when(ordenes.bloquearActiva(1L)).thenReturn(Optional.of(orden));
        when(ordenDetalles.findByIdAndOrdenIdAndEliminadoFalse(3L, 1L))
                .thenReturn(Optional.of(detalle));
    }

    @Test
    void registraRecepcionParcialConCostoDeLaOrden() {
        when(numeracion.siguienteNumero("REC")).thenReturn("REC-000001");
        when(recepciones.saveAndFlush(any())).thenAnswer(invocacion -> {
            RecepcionCompra recepcion = invocacion.getArgument(0);
            ReflectionTestUtils.setField(recepcion, "id", 4L);
            return recepcion;
        });
        when(recepcionDetalles.findByRecepcionIdAndEliminadoFalseOrderById(4L))
                .thenReturn(List.of());
        when(ordenDetalles.findByOrdenIdAndEliminadoFalseOrderById(1L))
                .thenReturn(List.of(detalle));

        servicio.recibir(new RecepcionGuardarDto(1L,
                List.of(new RecepcionDetalleSolicitudDto(3L, new BigDecimal("2.000"))), null));

        assertEquals(new BigDecimal("3.000"), detalle.getCantidadRecibida());
        assertEquals("CONFIRMADA", orden.getEstado());
        verify(movimientos).registrarRecepcion(2L, 5L, new BigDecimal("2.000"),
                new BigDecimal("12.50"), "REC-000001", "OC-000001");
    }

    @Test
    void rechazaRecepcionQueExcedeLoPendienteAntesDeMoverStock() {
        assertThrows(NegocioException.class, () -> servicio.recibir(new RecepcionGuardarDto(1L,
                List.of(new RecepcionDetalleSolicitudDto(3L, new BigDecimal("4.001"))), null)));

        verifyNoInteractions(movimientos, recepciones, numeracion);
        assertEquals(new BigDecimal("1.000"), detalle.getCantidadRecibida());
    }
}
