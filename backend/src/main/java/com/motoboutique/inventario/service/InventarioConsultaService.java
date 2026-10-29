package com.motoboutique.inventario.service;

import com.motoboutique.catalogo.domain.Producto;
import com.motoboutique.catalogo.repository.ProductoRepository;
import com.motoboutique.inventario.domain.Almacen;
import com.motoboutique.inventario.domain.EstadoStock;
import com.motoboutique.inventario.domain.MovimientoInventario;
import com.motoboutique.inventario.domain.Stock;
import com.motoboutique.inventario.domain.Transferencia;
import com.motoboutique.inventario.dto.KardexDto;
import com.motoboutique.inventario.dto.KardexLineaDto;
import com.motoboutique.inventario.dto.MovimientoDto;
import com.motoboutique.inventario.dto.StockInventarioDto;
import com.motoboutique.inventario.dto.TransferenciaDto;
import com.motoboutique.inventario.repository.AlmacenRepository;
import com.motoboutique.inventario.repository.MovimientoInventarioRepository;
import com.motoboutique.inventario.repository.StockRepository;
import com.motoboutique.inventario.repository.TransferenciaDetalleRepository;
import com.motoboutique.inventario.repository.TransferenciaRepository;
import com.motoboutique.shared.exception.NegocioException;
import com.motoboutique.shared.exception.RecursoNoEncontradoException;
import com.motoboutique.shared.web.PageResponse;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Subquery;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InventarioConsultaService {
    private static final Set<String> ORDEN_STOCK = Set.of(
            "producto.nombre", "producto.codigo", "cantidad", "almacen.nombre");
    private static final Set<String> ORDEN_MOVIMIENTOS = Set.of(
            "fecha", "tipo", "cantidad", "producto.nombre", "producto.codigo", "almacen.nombre");
    private static final Set<String> ORDEN_TRANSFERENCIAS = Set.of("fecha", "numero", "estado");

    private final ProductoRepository productoRepository;
    private final StockRepository stockRepository;
    private final AlmacenRepository almacenRepository;
    private final MovimientoInventarioRepository movimientoRepository;
    private final TransferenciaRepository transferenciaRepository;
    private final TransferenciaDetalleRepository detalleRepository;

    @Transactional(readOnly = true)
    public List<ProductoInventario> listarProductos() {
        return productoRepository.findAll((root, query, cb) -> cb.and(
                cb.isTrue(root.get("activo")), cb.isFalse(root.get("eliminado"))),
                Sort.by("nombre")).stream().map(producto -> {
                    List<Stock> stocks = stockRepository.findByProductoIdAndEliminadoFalse(producto.getId());
                    BigDecimal cantidad = stocks.stream().map(Stock::getCantidad)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    String almacenes = stocks.stream().filter(stock -> stock.getCantidad().signum() > 0)
                            .map(stock -> stock.getAlmacen().getNombre()).distinct()
                            .reduce((a, b) -> a + ", " + b).orElse("");
                    return new ProductoInventario(producto.getId(), producto.getCodigo(),
                            producto.getNombre(), producto.getCategoria().getNombre(), cantidad,
                            almacenes, EstadoStock.de(cantidad, producto.getStockMinimo()).name());
                }).toList();
    }

    @Transactional(readOnly = true)
    public ResumenInventario resumen() {
        return new ResumenInventario(productoRepository.countByActivoTrueAndEliminadoFalse(),
                stockRepository.valorizarInventario(), stockRepository.contarProductosConStockBajo());
    }

    @Transactional(readOnly = true)
    public PageResponse<StockInventarioDto> listarStock(String q, Long almacenId, Long categoriaId,
                                                        String estado, int page, int size,
                                                        String sort, String direction) {
        Specification<Stock> specification = stockSpecification(q, almacenId, categoriaId, estado);
        Page<Stock> resultado = stockRepository.findAll(specification,
                pagina(page, size, sort, direction, ORDEN_STOCK, "producto.nombre"));
        return PageResponse.de(resultado, this::stockDto);
    }

    @Transactional(readOnly = true)
    public PageResponse<MovimientoDto> listarMovimientos(String tipo, Long almacenId, Long productoId,
                                                         Instant desde, Instant hasta, String q,
                                                         int page, int size, String sort, String direction) {
        validarRango(desde, hasta);
        Specification<MovimientoInventario> specification =
                movimientoSpecification(tipo, almacenId, productoId, desde, hasta, q);
        Page<MovimientoInventario> resultado = movimientoRepository.findAll(specification,
                pagina(page, size, sort, direction, ORDEN_MOVIMIENTOS, "fecha"));
        return PageResponse.de(resultado, this::movimientoDto);
    }

    @Transactional(readOnly = true)
    public byte[] exportarMovimientos(String tipo, Long almacenId, Long productoId,
                                      Instant desde, Instant hasta, String q) {
        validarRango(desde, hasta);
        List<MovimientoInventario> movimientos = movimientoRepository.findAll(
                movimientoSpecification(tipo, almacenId, productoId, desde, hasta, q),
                Sort.by(Sort.Direction.DESC, "fecha", "id"));
        try (ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            salida.write(0xEF);
            salida.write(0xBB);
            salida.write(0xBF);
            salida.write("numero,fecha,tipo,codigo_producto,producto,almacen,cantidad,costo_unitario,saldo,documento_referencia,motivo,responsable\r\n"
                    .getBytes(StandardCharsets.UTF_8));
            for (MovimientoInventario movimiento : movimientos) {
                String fila = csv(movimiento.getReferencia()) + ","
                        + csv(movimiento.getFecha().toString()) + ","
                        + csv(movimiento.getTipo()) + ","
                        + csv(movimiento.getProducto().getCodigo()) + ","
                        + csv(movimiento.getProducto().getNombre()) + ","
                        + csv(movimiento.getAlmacen().getNombre()) + ","
                        + csv(movimiento.getCantidad().toPlainString()) + ","
                        + csv(texto(movimiento.getCostoUnitario())) + ","
                        + csv(movimiento.getSaldoCantidad().toPlainString()) + ","
                        + csv(movimiento.getDocumentoReferencia()) + ","
                        + csv(movimiento.getMotivo()) + ","
                        + csv(movimiento.getCreadoPor()) + "\r\n";
                salida.write(fila.getBytes(StandardCharsets.UTF_8));
            }
            return salida.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo generar el archivo CSV.", e);
        }
    }

    @Transactional(readOnly = true)
    public KardexDto kardex(Long productoId, Long almacenId, Instant desde, Instant hasta) {
        validarRango(desde, hasta);
        Producto producto = productoRepository.findByIdAndEliminadoFalse(productoId)
                .orElseThrow(() -> RecursoNoEncontradoException.de("Producto", productoId));
        Almacen almacen = almacenRepository.findByIdAndEliminadoFalse(almacenId)
                .orElseThrow(() -> RecursoNoEncontradoException.de("Almacén", almacenId));
        BigDecimal saldoInicial = desde == null ? BigDecimal.ZERO
                : movimientoRepository
                        .findFirstByProductoIdAndAlmacenIdAndEliminadoFalseAndFechaBeforeOrderByFechaDescIdDesc(
                                productoId, almacenId, desde)
                        .map(MovimientoInventario::getSaldoCantidad).orElse(BigDecimal.ZERO);
        Specification<MovimientoInventario> specification = (root, query, cb) -> {
            List<Predicate> condiciones = new ArrayList<>();
            condiciones.add(cb.equal(root.get("producto").get("id"), productoId));
            condiciones.add(cb.equal(root.get("almacen").get("id"), almacenId));
            condiciones.add(cb.isFalse(root.get("eliminado")));
            if (desde != null) {
                condiciones.add(cb.greaterThanOrEqualTo(root.get("fecha"), desde));
            }
            if (hasta != null) {
                condiciones.add(cb.lessThanOrEqualTo(root.get("fecha"), hasta));
            }
            return cb.and(condiciones.toArray(Predicate[]::new));
        };
        List<KardexLineaDto> lineas = movimientoRepository.findAll(specification,
                Sort.by(Sort.Direction.ASC, "fecha", "id")).stream().map(movimiento ->
                        new KardexLineaDto(movimiento.getId(), movimiento.getFecha(),
                                movimiento.getTipo(), movimiento.getReferencia(),
                                movimiento.getTipo().equals("ENTRADA")
                                        || movimiento.getTipo().equals("TRANSFERENCIA_ENTRADA")
                                        || movimiento.getTipo().equals("AJUSTE_POSITIVO")
                                        || movimiento.getTipo().equals("RECEPCION")
                                        ? movimiento.getCantidad() : BigDecimal.ZERO,
                                movimiento.getTipo().equals("SALIDA")
                                        || movimiento.getTipo().equals("TRANSFERENCIA_SALIDA")
                                        || movimiento.getTipo().equals("AJUSTE_NEGATIVO")
                                        ? movimiento.getCantidad() : BigDecimal.ZERO,
                                movimiento.getSaldoCantidad(), movimiento.getCostoUnitario(),
                                movimiento.getCostoPromedio(), movimiento.getDocumentoReferencia(),
                                movimiento.getMotivo())).toList();
        return new KardexDto(producto.getId(), producto.getNombre(), almacen.getId(),
                almacen.getNombre(), saldoInicial, lineas);
    }

    @Transactional(readOnly = true)
    public PageResponse<TransferenciaDto> listarTransferencias(String estado, Long almacenId,
                                                               Instant desde, Instant hasta,
                                                               int page, int size, String sort,
                                                               String direction) {
        validarRango(desde, hasta);
        Specification<Transferencia> specification = (root, query, cb) -> {
            List<Predicate> condiciones = new ArrayList<>();
            condiciones.add(cb.isFalse(root.get("eliminado")));
            if (estado != null && !estado.isBlank()) {
                condiciones.add(cb.equal(root.get("estado"), estado.trim().toUpperCase(Locale.ROOT)));
            }
            if (almacenId != null) {
                condiciones.add(cb.or(cb.equal(root.get("almacenOrigen").get("id"), almacenId),
                        cb.equal(root.get("almacenDestino").get("id"), almacenId)));
            }
            if (desde != null) {
                condiciones.add(cb.greaterThanOrEqualTo(root.get("fecha"), desde));
            }
            if (hasta != null) {
                condiciones.add(cb.lessThanOrEqualTo(root.get("fecha"), hasta));
            }
            return cb.and(condiciones.toArray(Predicate[]::new));
        };
        Page<Transferencia> resultado = transferenciaRepository.findAll(specification,
                pagina(page, size, sort, direction, ORDEN_TRANSFERENCIAS, "fecha"));
        return PageResponse.de(resultado, transferencia -> {
            List<TransferenciaDto.Detalle> detalles = detalleRepository
                    .findByTransferenciaIdAndEliminadoFalseOrderById(transferencia.getId()).stream()
                    .map(detalle -> new TransferenciaDto.Detalle(detalle.getProducto().getId(),
                            detalle.getProducto().getCodigo(), detalle.getProducto().getNombre(),
                            detalle.getCantidad())).toList();
            return new TransferenciaDto(transferencia.getId(), transferencia.getNumero(),
                    transferencia.getEstado(), transferencia.getFecha(),
                    transferencia.getAlmacenOrigen().getId(), transferencia.getAlmacenOrigen().getNombre(),
                    transferencia.getAlmacenDestino().getId(), transferencia.getAlmacenDestino().getNombre(),
                    transferencia.getObservaciones(), detalles);
        });
    }

    private Specification<Stock> stockSpecification(String q, Long almacenId, Long categoriaId,
                                                    String estado) {
        return (root, query, cb) -> {
            List<Predicate> condiciones = new ArrayList<>();
            condiciones.add(cb.isFalse(root.get("eliminado")));
            condiciones.add(cb.isTrue(root.get("producto").get("activo")));
            condiciones.add(cb.isFalse(root.get("producto").get("eliminado")));
            condiciones.add(cb.isTrue(root.get("almacen").get("activo")));
            condiciones.add(cb.isFalse(root.get("almacen").get("eliminado")));
            if (q != null && !q.isBlank()) {
                String patron = "%" + q.trim().toLowerCase(Locale.ROOT) + "%";
                condiciones.add(cb.or(cb.like(cb.lower(root.get("producto").get("codigo")), patron),
                        cb.like(cb.lower(root.get("producto").get("nombre")), patron)));
            }
            if (almacenId != null) {
                condiciones.add(cb.equal(root.get("almacen").get("id"), almacenId));
            }
            if (categoriaId != null) {
                condiciones.add(cb.equal(root.get("producto").get("categoria").get("id"), categoriaId));
            }
            if (estado != null && !estado.isBlank()) {
                String estadoNormalizado = estado.trim().toUpperCase(Locale.ROOT);
                Subquery<BigDecimal> suma = query.subquery(BigDecimal.class);
                var stockTotal = suma.from(Stock.class);
                suma.select(cb.sum(stockTotal.<BigDecimal>get("cantidad")))
                        .where(cb.equal(stockTotal.get("producto").get("id"),
                                root.get("producto").get("id")),
                                cb.isFalse(stockTotal.get("eliminado")));
                Expression<BigDecimal> total = cb.coalesce(suma, BigDecimal.ZERO);
                switch (estadoNormalizado) {
                    case "AGOTADO" -> condiciones.add(cb.equal(total, BigDecimal.ZERO));
                    case "BAJO" -> {
                        condiciones.add(cb.lessThanOrEqualTo(total,
                                root.get("producto").<BigDecimal>get("stockMinimo")));
                    }
                    case "OK" -> condiciones.add(cb.greaterThan(total,
                            root.get("producto").<BigDecimal>get("stockMinimo")));
                    default -> throw new NegocioException("El estado de stock indicado no es válido.");
                }
            }
            return cb.and(condiciones.toArray(Predicate[]::new));
        };
    }

    private Specification<MovimientoInventario> movimientoSpecification(
            String tipo, Long almacenId, Long productoId, Instant desde, Instant hasta, String q) {
        return (root, query, cb) -> {
            List<Predicate> condiciones = new ArrayList<>();
            condiciones.add(cb.isFalse(root.get("eliminado")));
            if (tipo != null && !tipo.isBlank()) {
                condiciones.add(cb.equal(root.get("tipo"), tipo.trim().toUpperCase(Locale.ROOT)));
            }
            if (almacenId != null) {
                condiciones.add(cb.equal(root.get("almacen").get("id"), almacenId));
            }
            if (productoId != null) {
                condiciones.add(cb.equal(root.get("producto").get("id"), productoId));
            }
            if (desde != null) {
                condiciones.add(cb.greaterThanOrEqualTo(root.get("fecha"), desde));
            }
            if (hasta != null) {
                condiciones.add(cb.lessThanOrEqualTo(root.get("fecha"), hasta));
            }
            if (q != null && !q.isBlank()) {
                String patron = "%" + q.trim().toLowerCase(Locale.ROOT) + "%";
                condiciones.add(cb.or(cb.like(cb.lower(root.get("producto").get("codigo")), patron),
                        cb.like(cb.lower(root.get("referencia")), patron),
                        cb.like(cb.lower(root.get("documentoReferencia")), patron)));
            }
            return cb.and(condiciones.toArray(Predicate[]::new));
        };
    }

    private StockInventarioDto stockDto(Stock stock) {
        BigDecimal total = stockRepository.cantidadTotalProducto(stock.getProducto().getId());
        return new StockInventarioDto(stock.getProducto().getId(), stock.getProducto().getCodigo(),
                stock.getProducto().getNombre(), stock.getProducto().getCategoria().getNombre(),
                total, stock.getProducto().getStockMinimo(),
                EstadoStock.de(total, stock.getProducto().getStockMinimo()).name(),
                stock.getAlmacen().getId(), stock.getAlmacen().getNombre(),
                stock.getCantidad(), stock.getStockReservado());
    }

    private MovimientoDto movimientoDto(MovimientoInventario movimiento) {
        return new MovimientoDto(movimiento.getId(), movimiento.getReferencia(), movimiento.getFecha(),
                movimiento.getTipo(), movimiento.getProducto().getId(), movimiento.getProducto().getCodigo(),
                movimiento.getProducto().getNombre(), movimiento.getAlmacen().getId(),
                movimiento.getAlmacen().getNombre(), movimiento.getCantidad(), movimiento.getCostoUnitario(),
                movimiento.getSaldoCantidad(), movimiento.getCostoPromedio(),
                movimiento.getDocumentoReferencia(), movimiento.getMotivo(), movimiento.getCreadoPor());
    }

    private Pageable pagina(int page, int size, String sort, String direction,
                            Set<String> permitidos, String predeterminado) {
        int pagina = Math.max(page, 0);
        int tamanio = Math.max(1, Math.min(size, 100));
        String columna = sort != null && permitidos.contains(sort) ? sort : predeterminado;
        Sort.Direction sentido = "asc".equalsIgnoreCase(direction)
                ? Sort.Direction.ASC : Sort.Direction.DESC;
        return PageRequest.of(pagina, tamanio, Sort.by(sentido, columna));
    }

    private void validarRango(Instant desde, Instant hasta) {
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new NegocioException("La fecha desde no puede ser posterior a la fecha hasta.");
        }
    }

    private String csv(String valor) {
        if (valor == null) {
            return "\"\"";
        }
        return "\"" + valor.replace("\"", "\"\"") + "\"";
    }

    private String texto(BigDecimal valor) {
        return valor == null ? "" : valor.toPlainString();
    }

    public record ProductoInventario(Long id, String codigo, String nombre, String categoria,
                                     BigDecimal cantidad, String almacen, String estado) {
    }

    public record ResumenInventario(long productosActivos, BigDecimal stockValorizado,
                                    long alertasStock) {
    }
}
