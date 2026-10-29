package com.motoboutique.reportes.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

import com.motoboutique.catalogo.repository.ProductoRepository;
import com.motoboutique.compras.repository.OrdenCompraRepository;
import com.motoboutique.inventario.repository.AjusteInventarioRepository;
import com.motoboutique.inventario.repository.MovimientoInventarioRepository;
import com.motoboutique.inventario.repository.StockRepository;
import com.motoboutique.inventario.repository.TransferenciaRepository;
import com.motoboutique.inventario.service.InventarioConsultaService;
import com.motoboutique.seguridad.repository.UsuarioRepository;
import com.motoboutique.shared.audit.AuditoriaEventoRepository;
import com.motoboutique.shared.exception.NegocioException;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.motoboutique.shared.web.PageResponse;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private ProductoRepository productoRepository;
    @Mock private StockRepository stockRepository;
    @Mock private MovimientoInventarioRepository movimientoRepository;
    @Mock private TransferenciaRepository transferenciaRepository;
    @Mock private AjusteInventarioRepository ajusteRepository;
    @Mock private OrdenCompraRepository ordenRepository;
    @Mock private AuditoriaEventoRepository auditoriaRepository;
    @Mock private InventarioConsultaService inventarioConsultaService;
    @InjectMocks private DashboardService service;

    @Test
    void serieDeAlmacenIncluyeDiasSinMovimientosConCeros() {
        var totales = new MovimientoInventarioRepository.TotalesMovimiento() {
            public BigDecimal getEntradas() { return BigDecimal.ZERO; }
            public BigDecimal getSalidas() { return BigDecimal.ZERO; }
        };
        when(movimientoRepository.totalesEntre(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any())).thenReturn(totales);
        when(movimientoRepository.serieDiaria(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any())).thenReturn(List.of());
        when(transferenciaRepository.countByEstadoAndEliminadoFalse("EN_TRANSITO")).thenReturn(0L);
        when(stockRepository.contarProductosConStockBajo()).thenReturn(0L);
        when(inventarioConsultaService.listarStock(null, null, null, "BAJO", 0, 10,
                "cantidad", "asc")).thenReturn(PageResponse.vacia(10));

        var resultado = service.obtener("almacen", 7);

        assertEquals(7, resultado.serie().size());
        assertEquals(BigDecimal.ZERO, resultado.serie().get(0).entradas());
        assertEquals(BigDecimal.ZERO, resultado.serie().get(0).salidas());
        assertEquals(BigDecimal.ZERO, resultado.kpis().get("entradasHoy"));
        assertEquals(BigDecimal.ZERO, resultado.kpis().get("salidasHoy"));
    }

    @Test
    void dashboardAdminIncluyeSerieCriticosYConteoDeAjustesPendientes() {
        when(usuarioRepository.countByActivoTrueAndEliminadoFalse()).thenReturn(3L);
        when(productoRepository.countByActivoTrueAndEliminadoFalse()).thenReturn(5L);
        when(stockRepository.valorizarInventario()).thenReturn(new BigDecimal("125.50"));
        when(stockRepository.contarProductosConStockBajo()).thenReturn(2L);
        when(ajusteRepository.countByEstadoAndEliminadoFalse("PENDIENTE")).thenReturn(4L);
        when(auditoriaRepository.findTop10ByEliminadoFalseOrderByFechaDescIdDesc()).thenReturn(List.of());
        when(movimientoRepository.serieDiaria(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any())).thenReturn(List.of());
        when(inventarioConsultaService.listarStock(null, null, null, "BAJO", 0, 10,
                "cantidad", "asc")).thenReturn(PageResponse.vacia(10));

        var resultado = service.obtener("admin", 30);

        assertEquals(30, resultado.serie().size());
        assertEquals(4L, resultado.kpis().get("ajustesPendientes"));
        assertEquals(new BigDecimal("125.50"), resultado.kpis().get("stockValorizado"));
        assertEquals(0, resultado.criticos().size());
        verify(movimientoRepository).serieDiaria(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void rangoNoPermitidoSeRechaza() {
        assertThrows(NegocioException.class, () -> service.obtener("almacen", 14));
    }
}
