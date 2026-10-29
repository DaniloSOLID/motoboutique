package com.motoboutique.compras.web;

import com.motoboutique.compras.dto.OrdenCompraDto;
import com.motoboutique.compras.dto.OrdenCompraGuardarDto;
import com.motoboutique.compras.dto.ProveedorDto;
import com.motoboutique.compras.dto.ProveedorGuardarDto;
import com.motoboutique.compras.dto.RecepcionDto;
import com.motoboutique.compras.dto.RecepcionGuardarDto;
import com.motoboutique.compras.service.OrdenCompraService;
import com.motoboutique.compras.service.ProveedorService;
import com.motoboutique.compras.service.RecepcionCompraService;
import com.motoboutique.shared.web.PageResponse;
import jakarta.validation.Valid;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
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
@RequestMapping("/api/compras")
@RequiredArgsConstructor
public class ComprasController {
    private final ProveedorService proveedorService;
    private final OrdenCompraService ordenService;
    private final RecepcionCompraService recepcionService;

    @GetMapping("/proveedores")
    @PreAuthorize("hasAuthority('COMPRA_VER') or hasRole('ADMIN')")
    public PageResponse<ProveedorDto> proveedores(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Boolean activo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "razonSocial") String sort,
            @RequestParam(defaultValue = "asc") String direction) {
        return proveedorService.listar(q, activo, page, size, sort, direction);
    }

    @PostMapping("/proveedores")
    @PreAuthorize("hasAuthority('COMPRA_EDITAR') or hasRole('ADMIN')")
    public ProveedorDto crearProveedor(@Valid @RequestBody ProveedorGuardarDto solicitud) {
        return proveedorService.crear(solicitud);
    }

    @PutMapping("/proveedores/{id}")
    @PreAuthorize("hasAuthority('COMPRA_EDITAR') or hasRole('ADMIN')")
    public ProveedorDto editarProveedor(@PathVariable Long id,
                                        @Valid @RequestBody ProveedorGuardarDto solicitud) {
        return proveedorService.editar(id, solicitud);
    }

    @DeleteMapping("/proveedores/{id}")
    @PreAuthorize("hasAuthority('COMPRA_EDITAR') or hasRole('ADMIN')")
    public ResponseEntity<Void> eliminarProveedor(@PathVariable Long id) {
        proveedorService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/ordenes")
    @PreAuthorize("hasAuthority('COMPRA_VER') or hasRole('ADMIN')")
    public PageResponse<OrdenCompraDto> ordenes(
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) Long proveedorId,
            @RequestParam(required = false) Instant desde,
            @RequestParam(required = false) Instant hasta,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "fecha") String sort,
            @RequestParam(defaultValue = "desc") String direction) {
        return ordenService.listar(estado, proveedorId, desde, hasta, q,
                page, size, sort, direction);
    }

    @GetMapping("/ordenes/{id}")
    @PreAuthorize("hasAuthority('COMPRA_VER') or hasRole('ADMIN')")
    public OrdenCompraDto obtenerOrden(@PathVariable Long id) {
        return ordenService.obtener(id);
    }

    @PostMapping("/ordenes")
    @PreAuthorize("hasAuthority('COMPRA_EDITAR') or hasRole('ADMIN')")
    public OrdenCompraDto crearOrden(@Valid @RequestBody OrdenCompraGuardarDto solicitud) {
        return ordenService.crear(solicitud);
    }

    @PutMapping("/ordenes/{id}")
    @PreAuthorize("hasAuthority('COMPRA_EDITAR') or hasRole('ADMIN')")
    public OrdenCompraDto editarOrden(@PathVariable Long id,
                                      @Valid @RequestBody OrdenCompraGuardarDto solicitud) {
        return ordenService.editar(id, solicitud);
    }

    @PostMapping("/ordenes/{id}/confirmar")
    @PreAuthorize("hasAuthority('COMPRA_EDITAR') or hasRole('ADMIN')")
    public OrdenCompraDto confirmarOrden(@PathVariable Long id) {
        return ordenService.confirmar(id);
    }

    @PostMapping("/ordenes/{id}/anular")
    @PreAuthorize("hasAuthority('COMPRA_EDITAR') or hasRole('ADMIN')")
    public OrdenCompraDto anularOrden(@PathVariable Long id) {
        return ordenService.anular(id);
    }

    @GetMapping("/recepciones")
    @PreAuthorize("hasAuthority('COMPRA_VER') or hasRole('ADMIN')")
    public PageResponse<RecepcionDto> recepciones(
            @RequestParam(required = false) Long ordenId,
            @RequestParam(required = false) Instant desde,
            @RequestParam(required = false) Instant hasta,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "fecha") String sort,
            @RequestParam(defaultValue = "desc") String direction) {
        return recepcionService.listar(ordenId, desde, hasta, page, size, sort, direction);
    }

    @PostMapping("/recepciones")
    @PreAuthorize("hasAuthority('COMPRA_EDITAR') or hasRole('ADMIN')")
    public RecepcionDto recibir(@Valid @RequestBody RecepcionGuardarDto solicitud) {
        return recepcionService.recibir(solicitud);
    }
}
