package com.motoboutique.reportes.service;

import com.motoboutique.catalogo.repository.ProductoRepository;
import com.motoboutique.compras.repository.OrdenCompraRepository;
import com.motoboutique.inventario.dto.StockInventarioDto;
import com.motoboutique.inventario.repository.AjusteInventarioRepository;
import com.motoboutique.inventario.repository.MovimientoInventarioRepository;
import com.motoboutique.inventario.repository.StockRepository;
import com.motoboutique.inventario.repository.TransferenciaRepository;
import com.motoboutique.inventario.service.InventarioConsultaService;
import com.motoboutique.reportes.dto.DashboardDto;
import com.motoboutique.seguridad.repository.UsuarioRepository;
import com.motoboutique.shared.audit.AuditoriaEvento;
import com.motoboutique.shared.audit.AuditoriaEventoRepository;
import com.motoboutique.shared.exception.NegocioException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DashboardService {
    private static final ZoneId ZONA = ZoneId.of("America/Lima");

    private final UsuarioRepository usuarioRepository;
    private final ProductoRepository productoRepository;
    private final StockRepository stockRepository;
    private final MovimientoInventarioRepository movimientoRepository;
    private final TransferenciaRepository transferenciaRepository;
    private final AjusteInventarioRepository ajusteRepository;
    private final OrdenCompraRepository ordenRepository;
    private final AuditoriaEventoRepository auditoriaRepository;
    private final InventarioConsultaService inventarioConsultaService;

    @Transactional(readOnly = true)
    public DashboardDto obtener(String rol, Integer dias) {
        int rango = dias == null ? 7 : dias;
        if (!List.of(7, 30, 90).contains(rango)) {
            throw new NegocioException("El rango del dashboard debe ser 7, 30 o 90 días.");
        }
        return switch (rol.toLowerCase(java.util.Locale.ROOT)) {
            case "admin" -> administrador(rango);
            case "almacen" -> almacen(rango);
            case "finanzas" -> finanzas();
            case "consulta" -> consulta();
            default -> throw new NegocioException("El rol de dashboard indicado no es válido.");
        };
    }

    private DashboardDto administrador(int dias) {
        Map<String, Object> kpis = new LinkedHashMap<>();
        kpis.put("usuariosActivos", usuarioRepository.countByActivoTrueAndEliminadoFalse());
        kpis.put("productosActivos", productoRepository.countByActivoTrueAndEliminadoFalse());
        kpis.put("stockValorizado", stockRepository.valorizarInventario());
        kpis.put("alertasStock", stockRepository.contarProductosConStockBajo());
        kpis.put("ajustesPendientes", ajusteRepository.countByEstadoAndEliminadoFalse("PENDIENTE"));
        List<DashboardDto.Actividad> actividad = auditoriaRepository
                .findTop10ByEliminadoFalseOrderByFechaDescIdDesc().stream()
                .map(this::actividad).toList();
        LocalDate hoy = LocalDate.now(ZONA);
        List<DashboardDto.Serie> series = serieMovimientos(dias, hoy);
        List<DashboardDto.Critico> criticos = inventarioConsultaService.listarStock(
                        null, null, null, "BAJO", 0, 10, "cantidad", "asc")
                .getContenido().stream().map(this::critico).distinct().toList();
        return new DashboardDto("admin", Map.copyOf(kpis), series, criticos, actividad);
    }

    private DashboardDto almacen(int dias) {
        LocalDate hoy = LocalDate.now(ZONA);
        Instant inicioHoy = hoy.atStartOfDay(ZONA).toInstant();
        Instant inicioManana = hoy.plusDays(1).atStartOfDay(ZONA).toInstant();
        MovimientoInventarioRepository.TotalesMovimiento totales =
                movimientoRepository.totalesEntre(inicioHoy, inicioManana);
        List<DashboardDto.Serie> series = serieMovimientos(dias, hoy);
        Map<String, Object> kpis = new LinkedHashMap<>();
        kpis.put("entradasHoy", totales.getEntradas());
        kpis.put("salidasHoy", totales.getSalidas());
        kpis.put("transferenciasEnTransito",
                transferenciaRepository.countByEstadoAndEliminadoFalse("EN_TRANSITO"));
        kpis.put("productosBajoMinimo", stockRepository.contarProductosConStockBajo());
        List<DashboardDto.Critico> criticos = inventarioConsultaService.listarStock(
                        null, null, null, "BAJO", 0, 10, "cantidad", "asc")
                .getContenido().stream().map(this::critico).distinct().toList();
        return new DashboardDto("almacen", Map.copyOf(kpis), series, criticos, List.of());
    }

    private List<DashboardDto.Serie> serieMovimientos(int dias, LocalDate hoy) {
        Instant inicioRango = hoy.minusDays(dias - 1L).atStartOfDay(ZONA).toInstant();
        Instant inicioManana = hoy.plusDays(1).atStartOfDay(ZONA).toInstant();
        Map<LocalDate, BigDecimal[]> porDia = new TreeMap<>();
        for (int i = 0; i < dias; i++) {
            porDia.put(hoy.minusDays(dias - 1L - i), new BigDecimal[] {BigDecimal.ZERO, BigDecimal.ZERO});
        }
        for (MovimientoInventarioRepository.SerieDiaria fila :
                movimientoRepository.serieDiaria(inicioRango, inicioManana)) {
            LocalDate fecha = fila.getDia();
            BigDecimal[] valores = porDia.get(fecha);
            if (valores != null) {
                if (esEntrada(fila.getTipo())) {
                    valores[0] = valores[0].add(fila.getCantidad());
                } else if (esSalida(fila.getTipo())) {
                    valores[1] = valores[1].add(fila.getCantidad());
                }
            }
        }
        return porDia.entrySet().stream()
                .map(e -> new DashboardDto.Serie(e.getKey(), e.getValue()[0], e.getValue()[1], BigDecimal.ZERO))
                .toList();
    }

    private DashboardDto finanzas() {
        LocalDate hoy = LocalDate.now(ZONA);
        YearMonth mesActual = YearMonth.from(hoy);
        Instant inicioMes = mesActual.atDay(1).atStartOfDay(ZONA).toInstant();
        Instant inicioManana = hoy.plusDays(1).atStartOfDay(ZONA).toInstant();
        Instant inicioSerie = mesActual.minusMonths(11).atDay(1).atStartOfDay(ZONA).toInstant();
        Map<YearMonth, BigDecimal> comprasPorMes = new TreeMap<>();
        for (int i = 0; i < 12; i++) {
            comprasPorMes.put(mesActual.minusMonths(11L - i), BigDecimal.ZERO);
        }
        for (OrdenCompraRepository.SerieMensual fila :
                ordenRepository.serieMensual(inicioSerie, inicioManana)) {
            YearMonth mes = YearMonth.from(fila.getMes());
            if (comprasPorMes.containsKey(mes)) {
                comprasPorMes.put(mes, fila.getTotal());
            }
        }
        BigDecimal inventario = stockRepository.valorizarInventario();
        BigDecimal costoSalidas = movimientoRepository.costoSalidasEntre(
                hoy.minusMonths(12).atStartOfDay(ZONA).toInstant(), inicioManana);
        BigDecimal rotacion = inventario.signum() == 0 ? BigDecimal.ZERO
                : costoSalidas.divide(inventario, 4, RoundingMode.HALF_UP);
        Map<String, Object> kpis = new LinkedHashMap<>();
        kpis.put("valorInventario", inventario);
        kpis.put("comprasDelMes", ordenRepository.totalComprasEntre(inicioMes, inicioManana));
        kpis.put("rotacion12Meses", rotacion);
        List<DashboardDto.Serie> series = comprasPorMes.entrySet().stream()
                .map(e -> new DashboardDto.Serie(e.getKey().atDay(1), BigDecimal.ZERO,
                        BigDecimal.ZERO, e.getValue())).toList();
        return new DashboardDto("finanzas", Map.copyOf(kpis), series, List.of(), List.of());
    }

    private DashboardDto consulta() {
        return new DashboardDto("consulta", Map.of(
                "productosDisponibles", productoRepository.contarDisponibles(),
                "productosAgotados", productoRepository.contarAgotados()),
                List.of(), List.of(), List.of());
    }

    private DashboardDto.Actividad actividad(AuditoriaEvento evento) {
        return new DashboardDto.Actividad(evento.getActor(), evento.getAccion(),
                evento.getEntidad(), evento.getEntidadId(), evento.getFecha());
    }

    private DashboardDto.Critico critico(StockInventarioDto stock) {
        return new DashboardDto.Critico(stock.productoId(), stock.codigo(), stock.nombre(),
                stock.categoria(), stock.cantidadTotal(), stock.stockMinimo(), stock.estado());
    }

    private boolean esEntrada(String tipo) {
        return List.of("ENTRADA", "TRANSFERENCIA_ENTRADA", "AJUSTE_POSITIVO", "RECEPCION")
                .contains(tipo);
    }

    private boolean esSalida(String tipo) {
        return List.of("SALIDA", "TRANSFERENCIA_SALIDA", "AJUSTE_NEGATIVO").contains(tipo);
    }
}
