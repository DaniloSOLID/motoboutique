package com.motoboutique.compras.service;

import com.motoboutique.catalogo.domain.Producto;
import com.motoboutique.catalogo.repository.ProductoRepository;
import com.motoboutique.compras.domain.ConfiguracionEmpresa;
import com.motoboutique.compras.domain.OrdenCompra;
import com.motoboutique.compras.domain.OrdenCompraDetalle;
import com.motoboutique.compras.domain.Proveedor;
import com.motoboutique.compras.dto.OrdenCompraDetalleDto;
import com.motoboutique.compras.dto.OrdenCompraDetalleGuardarDto;
import com.motoboutique.compras.dto.OrdenCompraDto;
import com.motoboutique.compras.dto.OrdenCompraGuardarDto;
import com.motoboutique.compras.repository.ConfiguracionEmpresaRepository;
import com.motoboutique.compras.repository.OrdenCompraDetalleRepository;
import com.motoboutique.compras.repository.OrdenCompraRepository;
import com.motoboutique.compras.repository.ProveedorRepository;
import com.motoboutique.inventario.domain.Almacen;
import com.motoboutique.inventario.repository.AlmacenRepository;
import com.motoboutique.inventario.repository.NumeracionDocumentoRepository;
import com.motoboutique.shared.exception.NegocioException;
import com.motoboutique.shared.exception.RecursoNoEncontradoException;
import com.motoboutique.shared.web.PageResponse;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
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
public class OrdenCompraService {
    private static final Short CONFIGURACION_ID = 1;
    private static final Set<String> ORDENES = Set.of(
            "numero", "fecha", "estado", "total", "proveedor.razonSocial");
    private final OrdenCompraRepository ordenRepository;
    private final OrdenCompraDetalleRepository detalleRepository;
    private final ProveedorRepository proveedorRepository;
    private final ProductoRepository productoRepository;
    private final AlmacenRepository almacenRepository;
    private final ConfiguracionEmpresaRepository configuracionRepository;
    private final NumeracionDocumentoRepository numeracionRepository;

    @Transactional(readOnly = true)
    public PageResponse<OrdenCompraDto> listar(String estado, Long proveedorId, Instant desde,
                                               Instant hasta, String q, int page, int size,
                                               String sort, String direction) {
        validarFechas(desde, hasta);
        Specification<OrdenCompra> specification = (root, query, cb) -> {
            List<Predicate> condiciones = new ArrayList<>();
            condiciones.add(cb.isFalse(root.get("eliminado")));
            if (estado != null && !estado.isBlank()) {
                condiciones.add(cb.equal(root.get("estado"), estado.trim().toUpperCase(Locale.ROOT)));
            }
            if (proveedorId != null) {
                condiciones.add(cb.equal(root.get("proveedor").get("id"), proveedorId));
            }
            if (desde != null) {
                condiciones.add(cb.greaterThanOrEqualTo(root.get("fecha"), desde));
            }
            if (hasta != null) {
                condiciones.add(cb.lessThanOrEqualTo(root.get("fecha"), hasta));
            }
            if (q != null && !q.isBlank()) {
                String patron = "%" + q.trim().toLowerCase(Locale.ROOT) + "%";
                condiciones.add(cb.or(cb.like(cb.lower(root.get("numero")), patron),
                        cb.like(cb.lower(root.get("proveedor").get("razonSocial")), patron)));
            }
            return cb.and(condiciones.toArray(Predicate[]::new));
        };
        Page<OrdenCompra> resultado = ordenRepository.findAll(specification,
                pagina(page, size, sort, direction));
        return PageResponse.de(resultado, this::dto);
    }

    @Transactional(readOnly = true)
    public OrdenCompraDto obtener(Long id) {
        return dto(buscar(id));
    }

    @Transactional
    public OrdenCompraDto crear(OrdenCompraGuardarDto solicitud) {
        OrdenCompra orden = new OrdenCompra();
        orden.setNumero(numeracionRepository.siguienteNumero("OC"));
        orden.setProveedor(proveedorActivo(solicitud.proveedorId()));
        orden.setAlmacen(almacen(solicitud.almacenId()));
        orden.setObservaciones(limpiar(solicitud.observaciones()));
        orden = ordenRepository.saveAndFlush(orden);
        reemplazarDetalles(orden, solicitud.detalles());
        recalcularTotales(orden);
        return dto(ordenRepository.saveAndFlush(orden));
    }

    @Transactional
    public OrdenCompraDto editar(Long id, OrdenCompraGuardarDto solicitud) {
        OrdenCompra orden = bloquear(id);
        exigirEstado(orden, "BORRADOR", "Solo se puede editar una orden en estado BORRADOR.");
        orden.setProveedor(proveedorActivo(solicitud.proveedorId()));
        orden.setAlmacen(almacen(solicitud.almacenId()));
        orden.setObservaciones(limpiar(solicitud.observaciones()));
        reemplazarDetalles(orden, solicitud.detalles());
        recalcularTotales(orden);
        return dto(ordenRepository.saveAndFlush(orden));
    }

    @Transactional
    public OrdenCompraDto confirmar(Long id) {
        OrdenCompra orden = bloquear(id);
        exigirEstado(orden, "BORRADOR", "Solo se puede confirmar una orden en estado BORRADOR.");
        if (detalleRepository.findByOrdenIdAndEliminadoFalseOrderById(id).isEmpty()) {
            throw new NegocioException("Agrega al menos un producto antes de confirmar la orden.");
        }
        orden.setEstado("CONFIRMADA");
        return dto(orden);
    }

    @Transactional
    public OrdenCompraDto anular(Long id) {
        OrdenCompra orden = bloquear(id);
        if (!"BORRADOR".equals(orden.getEstado()) && !"CONFIRMADA".equals(orden.getEstado())) {
            throw new NegocioException("Solo se puede anular una orden BORRADOR o CONFIRMADA.");
        }
        boolean recibida = detalleRepository.findByOrdenIdAndEliminadoFalseOrderById(id).stream()
                .anyMatch(detalle -> detalle.getCantidadRecibida().signum() > 0);
        if (recibida) {
            throw new NegocioException("No se puede anular una orden que ya tiene productos recibidos.");
        }
        orden.setEstado("ANULADA");
        return dto(orden);
    }

    private void reemplazarDetalles(OrdenCompra orden, List<OrdenCompraDetalleGuardarDto> solicitudes) {
        Set<Long> productos = new HashSet<>();
        for (OrdenCompraDetalleGuardarDto solicitud : solicitudes) {
            if (!productos.add(solicitud.productoId())) {
                throw new NegocioException("No repitas un producto dentro de la orden de compra.");
            }
        }
        detalleRepository.findByOrdenIdAndEliminadoFalseOrderById(orden.getId())
                .forEach(OrdenCompraDetalle::marcarEliminado);
        List<OrdenCompraDetalle> nuevos = new ArrayList<>();
        for (OrdenCompraDetalleGuardarDto solicitud : solicitudes) {
            Producto producto = productoRepository.bloquearActivo(solicitud.productoId())
                    .orElseThrow(() -> new RecursoNoEncontradoException(
                            "No se encontró un producto activo con id " + solicitud.productoId()));
            OrdenCompraDetalle detalle = new OrdenCompraDetalle();
            detalle.setOrden(orden);
            detalle.setProducto(producto);
            detalle.setCantidad(solicitud.cantidad());
            detalle.setCantidadRecibida(BigDecimal.ZERO.setScale(3));
            detalle.setCostoUnitario(solicitud.costoUnitario().setScale(2, RoundingMode.HALF_UP));
            detalle.setSubtotal(solicitud.cantidad().multiply(detalle.getCostoUnitario())
                    .setScale(2, RoundingMode.HALF_UP));
            nuevos.add(detalle);
        }
        detalleRepository.saveAll(nuevos);
        detalleRepository.flush();
    }

    private void recalcularTotales(OrdenCompra orden) {
        BigDecimal subtotal = detalleRepository.findByOrdenIdAndEliminadoFalseOrderById(orden.getId())
                .stream().map(OrdenCompraDetalle::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
        BigDecimal tasaIgv = configuracionRepository.findById(CONFIGURACION_ID)
                .map(ConfiguracionEmpresa::getIgv)
                .orElseThrow(() -> new NegocioException("No está configurado el IGV de la empresa."));
        BigDecimal igv = subtotal.multiply(tasaIgv)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        orden.setSubtotal(subtotal);
        orden.setIgv(igv);
        orden.setTotal(subtotal.add(igv).setScale(2, RoundingMode.HALF_UP));
    }

    private OrdenCompraDto dto(OrdenCompra orden) {
        List<OrdenCompraDetalleDto> detalles = detalleRepository
                .findByOrdenIdAndEliminadoFalseOrderById(orden.getId()).stream()
                .map(detalle -> new OrdenCompraDetalleDto(detalle.getId(), detalle.getProducto().getId(),
                        detalle.getProducto().getCodigo(), detalle.getProducto().getNombre(),
                        detalle.getCantidad(), detalle.getCantidadRecibida(),
                        detalle.getCantidad().subtract(detalle.getCantidadRecibida()),
                        detalle.getCostoUnitario(), detalle.getSubtotal()))
                .toList();
        return new OrdenCompraDto(orden.getId(), orden.getNumero(), orden.getEstado(), orden.getFecha(),
                orden.getProveedor().getId(), orden.getProveedor().getRazonSocial(),
                orden.getProveedor().getRuc(), orden.getAlmacen().getId(), orden.getAlmacen().getNombre(),
                orden.getObservaciones(), orden.getSubtotal(), orden.getIgv(), orden.getTotal(), detalles);
    }

    private OrdenCompra bloquear(Long id) {
        return ordenRepository.bloquearActiva(id)
                .orElseThrow(() -> RecursoNoEncontradoException.de("Orden de compra", id));
    }

    private OrdenCompra buscar(Long id) {
        return ordenRepository.findById(id).filter(orden -> !orden.isEliminado())
                .orElseThrow(() -> RecursoNoEncontradoException.de("Orden de compra", id));
    }

    private Proveedor proveedorActivo(Long id) {
        Proveedor proveedor = proveedorRepository.findById(id)
                .filter(p -> !p.isEliminado())
                .orElseThrow(() -> RecursoNoEncontradoException.de("Proveedor", id));
        if (!proveedor.isActivo()) {
            throw new NegocioException("El proveedor seleccionado está inactivo.");
        }
        return proveedor;
    }

    private Almacen almacen(Long id) {
        ConfiguracionEmpresa configuracion = configuracionRepository.findById(CONFIGURACION_ID)
                .orElseThrow(() -> new NegocioException("No existe la configuración de la empresa."));
        Almacen seleccionado = id == null ? configuracion.getAlmacenDefecto()
                : almacenRepository.findByIdAndEliminadoFalse(id)
                        .orElseThrow(() -> RecursoNoEncontradoException.de("Almacén", id));
        if (seleccionado == null || !seleccionado.isActivo() || seleccionado.isEliminado()) {
            throw new NegocioException("Selecciona un almacén activo para la orden.");
        }
        return seleccionado;
    }

    private void exigirEstado(OrdenCompra orden, String esperado, String mensaje) {
        if (!esperado.equals(orden.getEstado())) {
            throw new NegocioException(mensaje);
        }
    }

    private void validarFechas(Instant desde, Instant hasta) {
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new NegocioException("La fecha desde no puede ser posterior a la fecha hasta.");
        }
    }

    private String limpiar(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    private Pageable pagina(int page, int size, String sort, String direction) {
        String columna = sort != null && ORDENES.contains(sort) ? sort : "fecha";
        Sort.Direction sentido = "asc".equalsIgnoreCase(direction)
                ? Sort.Direction.ASC : Sort.Direction.DESC;
        return PageRequest.of(Math.max(0, page), Math.max(1, Math.min(100, size)),
                Sort.by(sentido, columna));
    }
}
