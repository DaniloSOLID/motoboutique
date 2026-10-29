package com.motoboutique.reportes.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import com.motoboutique.inventario.repository.MovimientoInventarioRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ValorizacionServiceTest {
    @Mock private MovimientoInventarioRepository movimientoRepository;
    @InjectMocks private ValorizacionService service;

    @Test
    void totalDeValorizacionCoincideConLaSumaDelKardexPorCategoria() {
        var producto = new MovimientoInventarioRepository.ValorizacionProducto() {
            public Long getProductoId() { return 1L; }
            public String getCodigo() { return "P-001"; }
            public String getProducto() { return "Producto"; }
            public Long getCategoriaId() { return 2L; }
            public String getCategoria() { return "Motor"; }
            public BigDecimal getCantidad() { return new BigDecimal("3.000"); }
            public BigDecimal getCostoPromedio() { return new BigDecimal("12.50"); }
            public BigDecimal getValor() { return new BigDecimal("37.50"); }
            public Long getTotalFilas() { return 1L; }
        };
        var categoria = new MovimientoInventarioRepository.ValorizacionCategoria() {
            public Long getCategoriaId() { return 2L; }
            public String getCategoria() { return "Motor"; }
            public BigDecimal getCantidad() { return new BigDecimal("3.000"); }
            public BigDecimal getValor() { return new BigDecimal("37.50"); }
        };
        when(movimientoRepository.valorizarAlCorte(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.eq(20),
                org.mockito.ArgumentMatchers.eq(0L))).thenReturn(List.of(producto));
        when(movimientoRepository.contarValorizacionAlCorte(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.isNull())).thenReturn(1L);
        when(movimientoRepository.valorizarPorCategoriaAlCorte(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.isNull()))
                .thenReturn(List.of(categoria));

        var resultado = service.valorizar(LocalDate.parse("2026-10-28"), null, null, null, 0, 20);

        assertEquals(new BigDecimal("37.50"), resultado.valorTotal());
        assertEquals(resultado.valorTotal(), resultado.categorias().stream()
                .map(com.motoboutique.reportes.dto.ValorizacionDto.Categoria::valor)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        assertEquals(resultado.cantidadTotal(), resultado.productos().getContenido().stream()
                .map(com.motoboutique.reportes.dto.ValorizacionDto.Producto::cantidad)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }
}
