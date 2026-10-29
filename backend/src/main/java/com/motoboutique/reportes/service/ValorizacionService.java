package com.motoboutique.reportes.service;

import com.motoboutique.inventario.repository.MovimientoInventarioRepository;
import com.motoboutique.reportes.dto.ValorizacionDto;
import com.motoboutique.shared.web.PageResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ValorizacionService {
    private static final ZoneId ZONA = ZoneId.of("America/Lima");
    private final MovimientoInventarioRepository movimientoRepository;

    @Transactional(readOnly = true)
    public ValorizacionDto valorizar(LocalDate fechaCorte, Long almacenId, Long categoriaId,
                                     String q, int page, int size) {
        LocalDate fecha = fechaCorte == null ? LocalDate.now(ZONA) : fechaCorte;
        int pagina = Math.max(0, page);
        int tamanio = Math.max(1, Math.min(size, 100));
        String busqueda = q == null || q.isBlank() ? null : q.trim();
        var corteExclusivo = fecha.plusDays(1).atStartOfDay(ZONA).toInstant();
        List<MovimientoInventarioRepository.ValorizacionProducto> resultados =
                movimientoRepository.valorizarAlCorte(corteExclusivo, almacenId, categoriaId,
                        busqueda, tamanio, (long) pagina * tamanio);
        long totalElementos = movimientoRepository.contarValorizacionAlCorte(
                corteExclusivo, almacenId, categoriaId, busqueda);
        List<ValorizacionDto.Producto> productos = resultados.stream().map(fila ->
                new ValorizacionDto.Producto(fila.getProductoId(), fila.getCodigo(),
                        fila.getProducto(), fila.getCategoriaId(), fila.getCategoria(),
                        fila.getCantidad(), fila.getCostoPromedio(), fila.getValor())).toList();
        List<ValorizacionDto.Categoria> categorias =
                movimientoRepository.valorizarPorCategoriaAlCorte(
                                corteExclusivo, almacenId, categoriaId, busqueda)
                        .stream().map(fila -> new ValorizacionDto.Categoria(fila.getCategoriaId(),
                                fila.getCategoria(), fila.getCantidad(), fila.getValor())).toList();
        BigDecimal cantidad = categorias.stream().map(ValorizacionDto.Categoria::cantidad)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal valor = categorias.stream().map(ValorizacionDto.Categoria::valor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        PageResponse<ValorizacionDto.Producto> paginaProductos = new PageResponse<>(
                productos, pagina, tamanio, totalElementos,
                (int) Math.ceil((double) totalElementos / tamanio));
        return new ValorizacionDto(fecha, cantidad, valor, paginaProductos, categorias);
    }
}
