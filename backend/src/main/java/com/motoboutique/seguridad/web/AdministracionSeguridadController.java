package com.motoboutique.seguridad.web;

import com.motoboutique.seguridad.dto.PermisoDto;
import com.motoboutique.seguridad.dto.RolDto;
import com.motoboutique.seguridad.service.PermisoService;
import com.motoboutique.seguridad.service.RolService;
import com.motoboutique.shared.audit.AuditoriaService;
import com.motoboutique.shared.web.PageResponse;
import com.motoboutique.shared.exception.NegocioException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AdministracionSeguridadController {

    private final RolService rolService;
    private final PermisoService permisoService;
    private final AuditoriaService auditoriaService;

    @GetMapping("/roles")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('ROL_VER')")
    public List<RolDto> roles() {
        return rolService.listar();
    }

    @GetMapping("/permisos")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('ROL_VER')")
    public List<PermisoDto> permisos() {
        return permisoService.listar();
    }

    @PutMapping("/roles/{id}/permisos")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('ROL_EDITAR')")
    public RolDto actualizarPermisos(@PathVariable Long id,
            @Valid @RequestBody PermisosSolicitud solicitud) {
        return rolService.actualizarPermisos(id, solicitud.permisos());
    }

    @GetMapping("/auditoria")
    @PreAuthorize("hasRole('ADMIN')")
    public PageResponse<AuditoriaService.AuditoriaDto> auditoria(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String usuario,
            @RequestParam(required = false) String accion,
            @RequestParam(required = false) Instant desde,
            @RequestParam(required = false) Instant hasta,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (page < 0 || size < 1 || size > 200) {
            throw new NegocioException("La página o el tamaño no son válidos.");
        }
        return auditoriaService.listar(q, usuario, accion, desde, hasta, page, size);
    }

    public record PermisosSolicitud(@NotNull @Size(max = 100) List<@NotBlank String> permisos) {}
}
