package com.motoboutique.inventario.web;

import com.motoboutique.inventario.dto.AlmacenDto;
import com.motoboutique.inventario.dto.AlmacenGuardarDto;
import com.motoboutique.inventario.dto.KardexDto;
import com.motoboutique.inventario.dto.MovimientoDto;
import com.motoboutique.inventario.dto.MovimientoSolicitudDto;
import com.motoboutique.inventario.dto.StockInventarioDto;
import com.motoboutique.inventario.dto.TransferenciaDto;
import com.motoboutique.inventario.dto.TransferenciaSolicitudDto;
import com.motoboutique.inventario.service.AlmacenService;
import com.motoboutique.inventario.service.InventarioConsultaService;
import com.motoboutique.inventario.service.MovimientoInventarioService;
import com.motoboutique.shared.web.PageResponse;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/inventario")
@RequiredArgsConstructor
public class InventarioController {
    private final InventarioConsultaService consultaService;
    private final MovimientoInventarioService movimientoService;
    private final AlmacenService almacenService;

    @GetMapping("/productos")
    @PreAuthorize("hasAuthority('INVENTARIO_VER') or hasAuthority('PRODUCTO_VER') or hasRole('ADMIN')")
    public List<InventarioConsultaService.ProductoInventario> productos() {
        return consultaService.listarProductos();
    }

    @GetMapping("/resumen")
    @PreAuthorize("hasAuthority('INVENTARIO_VER') or hasRole('ADMIN')")
    public InventarioConsultaService.ResumenInventario resumen() {
        return consultaService.resumen();
    }

    @GetMapping("/almacenes")
    @PreAuthorize("hasAuthority('INVENTARIO_VER') or hasRole('ADMIN')")
    public List<AlmacenDto> almacenes() {
        return almacenService.listar();
    }

    @PostMapping("/almacenes")
    @PreAuthorize("(hasAuthority('INVENTARIO_EDITAR') and hasRole('ADMIN'))")
    public AlmacenDto crearAlmacen(@Valid @RequestBody AlmacenGuardarDto solicitud) {
        return almacenService.crear(solicitud);
    }

    @PutMapping("/almacenes/{id}")
    @PreAuthorize("(hasAuthority('INVENTARIO_EDITAR') and hasRole('ADMIN'))")
    public AlmacenDto editarAlmacen(@PathVariable Long id,
                                    @Valid @RequestBody AlmacenGuardarDto solicitud) {
        return almacenService.editar(id, solicitud);
    }

    @DeleteMapping("/almacenes/{id}")
    @PreAuthorize("(hasAuthority('INVENTARIO_EDITAR') and hasRole('ADMIN'))")
    public ResponseEntity<Void> eliminarAlmacen(@PathVariable Long id) {
        almacenService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/stock")
    @PreAuthorize("hasAuthority('INVENTARIO_VER') or hasRole('ADMIN')")
    public PageResponse<StockInventarioDto> stock(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long almacenId,
            @RequestParam(required = false) Long categoriaId,
            @RequestParam(required = false) String estado,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "producto.nombre") String sort,
            @RequestParam(defaultValue = "asc") String direction) {
        return consultaService.listarStock(q, almacenId, categoriaId, estado,
                page, size, sort, direction);
    }

    @GetMapping("/movimientos")
    @PreAuthorize("hasAuthority('INVENTARIO_VER') or hasRole('ADMIN')")
    public PageResponse<MovimientoDto> movimientos(
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false) Long almacenId,
            @RequestParam(required = false) Long productoId,
            @RequestParam(required = false) Instant desde,
            @RequestParam(required = false) Instant hasta,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "fecha") String sort,
            @RequestParam(defaultValue = "desc") String direction) {
        return consultaService.listarMovimientos(tipo, almacenId, productoId, desde, hasta,
                q, page, size, sort, direction);
    }

    @GetMapping(value = "/movimientos/exportar", produces = "text/csv")
    @PreAuthorize("hasAuthority('INVENTARIO_VER') or hasAuthority('REPORTE_EXPORTAR') or hasRole('ADMIN')")
    public ResponseEntity<byte[]> exportarMovimientos(
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false) Long almacenId,
            @RequestParam(required = false) Long productoId,
            @RequestParam(required = false) Instant desde,
            @RequestParam(required = false) Instant hasta,
            @RequestParam(required = false) String q) {
        byte[] csv = consultaService.exportarMovimientos(tipo, almacenId, productoId, desde, hasta, q);
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "csv"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("movimientos-inventario.csv").build().toString())
                .body(csv);
    }

    @PostMapping("/entradas")
    @PreAuthorize("hasAuthority('INVENTARIO_EDITAR') or hasRole('ADMIN')")
    public MovimientoDto entrada(@Valid @RequestBody MovimientoSolicitudDto solicitud) {
        return movimientoService.registrarEntrada(solicitud);
    }

    @PostMapping("/salidas")
    @PreAuthorize("hasAuthority('INVENTARIO_EDITAR') or hasRole('ADMIN')")
    public MovimientoDto salida(@Valid @RequestBody MovimientoSolicitudDto solicitud) {
        return movimientoService.registrarSalida(solicitud);
    }

    @GetMapping("/kardex")
    @PreAuthorize("hasAuthority('INVENTARIO_VER') or hasAuthority('KARDEX_VER') or hasRole('ADMIN')")
    public KardexDto kardex(@RequestParam Long productoId, @RequestParam Long almacenId,
                            @RequestParam(required = false) Instant desde,
                            @RequestParam(required = false) Instant hasta) {
        return consultaService.kardex(productoId, almacenId, desde, hasta);
    }

    @GetMapping("/transferencias")
    @PreAuthorize("hasAuthority('INVENTARIO_VER') or hasAuthority('TRANSFERENCIA_EDITAR') or hasRole('ADMIN')")
    public PageResponse<TransferenciaDto> transferencias(
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) Long almacenId,
            @RequestParam(required = false) Instant desde,
            @RequestParam(required = false) Instant hasta,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "fecha") String sort,
            @RequestParam(defaultValue = "desc") String direction) {
        return consultaService.listarTransferencias(estado, almacenId, desde, hasta,
                page, size, sort, direction);
    }

    @PostMapping("/transferencias")
    @PreAuthorize("hasAuthority('TRANSFERENCIA_EDITAR') or hasRole('ADMIN')")
    public TransferenciaDto crearTransferencia(@Valid @RequestBody TransferenciaSolicitudDto solicitud) {
        return movimientoService.crearTransferencia(solicitud);
    }

    @PostMapping("/transferencias/{id}/despachar")
    @PreAuthorize("hasAuthority('TRANSFERENCIA_EDITAR') or hasRole('ADMIN')")
    public TransferenciaDto despachar(@PathVariable Long id) {
        return movimientoService.despachar(id);
    }

    @PostMapping("/transferencias/{id}/recibir")
    @PreAuthorize("hasAuthority('TRANSFERENCIA_EDITAR') or hasRole('ADMIN')")
    public TransferenciaDto recibir(@PathVariable Long id) {
        return movimientoService.recibir(id);
    }

    @PostMapping("/transferencias/{id}/cancelar")
    @PreAuthorize("hasAuthority('TRANSFERENCIA_EDITAR') or hasRole('ADMIN')")
    public TransferenciaDto cancelar(@PathVariable Long id) {
        return movimientoService.cancelar(id);
    }
}
