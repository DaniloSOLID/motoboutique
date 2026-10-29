package com.motoboutique.catalogo.web;

import com.motoboutique.catalogo.dto.UnidadMotoDto;
import com.motoboutique.catalogo.dto.UnidadMotoGuardarDto;
import com.motoboutique.catalogo.service.UnidadMotoService;
import com.motoboutique.shared.web.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/motos/unidades")
@RequiredArgsConstructor
class UnidadMotoController {
    private final UnidadMotoService servicio;

    @GetMapping
    @PreAuthorize("hasAuthority('PRODUCTO_VER') or hasRole('ADMIN')")
    PageResponse<UnidadMotoDto> listar(String q, Long modeloId, String estado, @PageableDefault(size=20, sort="chasis") Pageable pageable) { return servicio.listar(q, modeloId, estado, pageable); }
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PRODUCTO_VER') or hasRole('ADMIN')")
    UnidadMotoDto obtener(@PathVariable Long id) { return servicio.obtener(id); }
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('PRODUCTO_EDITAR') or hasRole('ADMIN')")
    UnidadMotoDto crear(@Valid @RequestBody UnidadMotoGuardarDto dto) { return servicio.crear(dto); }
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PRODUCTO_EDITAR') or hasRole('ADMIN')")
    UnidadMotoDto actualizar(@PathVariable Long id, @Valid @RequestBody UnidadMotoGuardarDto dto) { return servicio.actualizar(id, dto); }
    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasAuthority('PRODUCTO_EDITAR') or hasRole('ADMIN')")
    UnidadMotoDto estado(@PathVariable Long id, @RequestParam String estado) { return servicio.cambiarEstado(id, estado); }
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('PRODUCTO_EDITAR') or hasRole('ADMIN')")
    void eliminar(@PathVariable Long id) { servicio.eliminar(id); }
}
