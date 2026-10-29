package com.motoboutique.reportes.service;

import com.motoboutique.compras.repository.ConfiguracionEmpresaRepository;
import com.motoboutique.compras.repository.OrdenCompraRepository;
import com.motoboutique.inventario.dto.MovimientoDto;
import com.motoboutique.inventario.dto.StockInventarioDto;
import com.motoboutique.inventario.service.InventarioConsultaService;
import com.motoboutique.reportes.dto.ReporteDto;
import com.motoboutique.reportes.dto.ValorizacionDto;
import com.motoboutique.shared.exception.NegocioException;
import com.motoboutique.shared.web.PageResponse;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReportesService {
    private final InventarioConsultaService inventarioService;
    private final ValorizacionService valorizacionService;
    private final OrdenCompraRepository ordenRepository;
    private final ConfiguracionEmpresaRepository configuracionRepository;

    @Transactional(readOnly = true)
    public ReporteDto generar(String tipo, LocalDate fechaCorte, Long almacenId, Long categoriaId,
                              Long productoId, Instant desde, Instant hasta, String q,
                              int page, int size) {
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new NegocioException("La fecha desde no puede ser posterior a la fecha hasta.");
        }
        String normalizado = tipo.toLowerCase(java.util.Locale.ROOT);
        int pagina = Math.max(page, 0);
        int tamanio = Math.max(1, Math.min(size, 100));
        String busqueda = q == null || q.isBlank() ? null : q.trim();
        Datos datos = switch (normalizado) {
            case "valorizacion" -> valorizacion(fechaCorte, almacenId, categoriaId,
                    busqueda, pagina, tamanio);
            case "kardex" -> kardex(productoId, almacenId, desde, hasta, pagina, tamanio);
            case "stock-bajo" -> stockBajo(almacenId, categoriaId, busqueda, pagina, tamanio);
            case "movimientos-por-periodo" -> movimientos(almacenId, productoId, desde, hasta,
                    busqueda, pagina, tamanio);
            case "compras-por-proveedor" -> comprasProveedor(desde, hasta, busqueda, pagina, tamanio);
            case "por-categoria" -> categorias(fechaCorte, almacenId, categoriaId,
                    busqueda, pagina, tamanio);
            default -> throw new NegocioException("El tipo de reporte indicado no existe.");
        };
        var empresa = configuracionRepository.findById((short) 1).orElse(null);
        return new ReporteDto(normalizado, empresa == null ? "" : empresa.getRazonSocial(),
                empresa == null ? "" : empresa.getRuc(), Instant.now(), datos.columnas(),
                datos.pagina(), datos.totales());
    }

    private Datos valorizacion(LocalDate fechaCorte, Long almacenId, Long categoriaId,
                               String q, int page, int size) {
        ValorizacionDto reporte = valorizacionService.valorizar(fechaCorte, almacenId,
                categoriaId, q, page, size);
        List<List<String>> filas = reporte.productos().getContenido().stream().map(p -> List.of(
                texto(p.codigo()), texto(p.producto()), texto(p.categoria()),
                texto(p.cantidad()), texto(p.costoPromedio()), texto(p.valor()))).toList();
        Map<String, String> totales = new LinkedHashMap<>();
        totales.put("cantidadTotal", texto(reporte.cantidadTotal()));
        totales.put("valorTotal", texto(reporte.valorTotal()));
        return new Datos(List.of("Código", "Producto", "Categoría", "Cantidad",
                "Costo promedio", "Valor"), reporte.productos().getTotalElementos(),
                reporte.productos().getTotalPaginas(), page, size, filas, totales);
    }

    private Datos kardex(Long productoId, Long almacenId, Instant desde, Instant hasta,
                        int page, int size) {
        if (productoId == null || almacenId == null) {
            throw new NegocioException("El reporte de kardex requiere productoId y almacenId.");
        }
        var kardex = inventarioService.kardex(productoId, almacenId, desde, hasta);
        var lineas = kardex.movimientos();
        List<List<String>> filas = lineas.stream().map(linea -> List.of(
                linea.fecha().toString(), linea.tipo(), texto(linea.entrada()),
                texto(linea.salida()), texto(linea.saldo()), texto(linea.costoPromedio()))).toList();
        int desdeFila = (int) Math.min((long) page * size, filas.size());
        int hastaFila = Math.min(desdeFila + size, filas.size());
        BigDecimal entradas = lineas.stream().map(linea -> linea.entrada() == null
                ? BigDecimal.ZERO : linea.entrada()).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal salidas = lineas.stream().map(linea -> linea.salida() == null
                ? BigDecimal.ZERO : linea.salida()).reduce(BigDecimal.ZERO, BigDecimal::add);
        Map<String, String> totales = Map.of(
                "saldoInicial", texto(kardex.saldoInicial()),
                "saldoFinal", texto(lineas.isEmpty() ? kardex.saldoInicial()
                        : lineas.get(lineas.size() - 1).saldo()),
                "entradas", texto(entradas), "salidas", texto(salidas));
        return datos(List.of("Fecha", "Tipo", "Entrada", "Salida", "Saldo", "Costo promedio"),
                filas.subList(desdeFila, hastaFila), page, size, totales, filas.size());
    }

    private Datos stockBajo(Long almacenId, Long categoriaId, String q, int page, int size) {
        PageResponse<StockInventarioDto> stock = inventarioService.listarStock(q, almacenId,
                categoriaId, "BAJO", page, size, "cantidad", "asc");
        List<List<String>> filas = stock.getContenido().stream().map(item -> List.of(
                item.codigo(), item.nombre(), item.categoria(), texto(item.cantidadTotal()),
                texto(item.stockMinimo()), item.estado(), item.almacen())).toList();
        return new Datos(List.of("Código", "Producto", "Categoría", "Stock total",
                "Mínimo", "Estado", "Almacén"), stock.getTotalElementos(),
                stock.getTotalPaginas(), page, size, filas, Map.of("productos", 
                Long.toString(stock.getTotalElementos())));
    }

    private Datos movimientos(Long almacenId, Long productoId, Instant desde, Instant hasta,
                              String q, int page, int size) {
        PageResponse<MovimientoDto> movimientos = inventarioService.listarMovimientos(null,
                almacenId, productoId, desde, hasta, q, page, size, "fecha", "desc");
        List<List<String>> filas = movimientos.getContenido().stream().map(m -> List.of(
                texto(m.numero()), m.fecha().toString(), m.tipo(), m.productoCodigo(),
                m.productoNombre(), texto(m.cantidad()), texto(m.costoUnitario()),
                texto(m.saldoCantidad()))).toList();
        BigDecimal entradas = movimientos.getContenido().stream()
                .filter(m -> esEntrada(m.tipo())).map(MovimientoDto::cantidad)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal salidas = movimientos.getContenido().stream()
                .filter(m -> esSalida(m.tipo())).map(MovimientoDto::cantidad)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new Datos(List.of("Documento", "Fecha", "Tipo", "Código", "Producto",
                "Cantidad", "Costo unitario", "Saldo"), movimientos.getTotalElementos(),
                movimientos.getTotalPaginas(), page, size,
                filas, Map.of("entradasPagina", texto(entradas), "salidasPagina", texto(salidas)));
    }

    private Datos comprasProveedor(Instant desde, Instant hasta, String q, int page, int size) {
        var filas = ordenRepository.totalesPorProveedor(desde, hasta, q, size, (long) page * size);
        long totalFilas = ordenRepository.contarProveedoresParaReporte(desde, hasta, q);
        List<List<String>> contenido = filas.stream().map(f -> List.of(f.getProveedor(),
                Long.toString(f.getOrdenes()), texto(f.getTotal()))).toList();
        BigDecimal total = ordenRepository.totalProveedoresParaReporte(desde, hasta, q);
        return new Datos(List.of("Proveedor", "Órdenes", "Total"), totalFilas,
                paginas(totalFilas, size), page, size, contenido,
                Map.of("totalCompras", texto(total)));
    }

    private Datos categorias(LocalDate fechaCorte, Long almacenId, Long categoriaId,
                             String q, int page, int size) {
        ValorizacionDto valores = valorizacionService.valorizar(fechaCorte, almacenId,
                categoriaId, q, 0, 100);
        List<List<String>> todas = valores.categorias().stream().map(c -> List.of(
                c.categoria(), texto(c.cantidad()), texto(c.valor()))).toList();
        int desdeFila = (int) Math.min((long) page * size, todas.size());
        int hastaFila = Math.min(desdeFila + size, todas.size());
        return datos(List.of("Categoría", "Cantidad", "Valor"),
                todas.subList(desdeFila, hastaFila), page, size,
                Map.of("cantidadTotal", texto(valores.cantidadTotal()),
                        "valorTotal", texto(valores.valorTotal())),
                todas.size());
    }

    private Datos datos(List<String> columnas, List<List<String>> filas, int page, int size,
                        Map<String, String> totales) {
        return datos(columnas, filas, page, size, totales, filas.size());
    }

    private Datos datos(List<String> columnas, List<List<String>> filas, int page, int size,
                        Map<String, String> totales, long total) {
        return new Datos(columnas, total, paginas(total, size), page, size, filas, totales);
    }

    private int paginas(long total, int size) {
        return (int) Math.ceil((double) total / size);
    }

    private String texto(Object value) {
        return value == null ? "" : value.toString();
    }

    private boolean esEntrada(String tipo) {
        return List.of("ENTRADA", "TRANSFERENCIA_ENTRADA", "AJUSTE_POSITIVO", "RECEPCION")
                .contains(tipo);
    }

    private boolean esSalida(String tipo) {
        return List.of("SALIDA", "TRANSFERENCIA_SALIDA", "AJUSTE_NEGATIVO").contains(tipo);
    }

    private record Datos(List<String> columnas, long totalElementos, int totalPaginas,
                         int page, int size, List<List<String>> filas, Map<String, String> totales) {
        PageResponse<List<String>> pagina() {
            return new PageResponse<>(filas, page, size, totalElementos, totalPaginas);
        }
    }
}
