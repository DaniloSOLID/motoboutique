package com.motoboutique.reportes.web;

import com.motoboutique.reportes.dto.DashboardDto;
import com.motoboutique.reportes.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {
    private final DashboardService service;

    @GetMapping("/{rol}")
    @PreAuthorize("(#rol == 'admin' and hasRole('ADMIN'))"
            + " or (#rol == 'almacen' and (hasAuthority('INVENTARIO_VER') or hasRole('ADMIN')))"
            + " or (#rol == 'finanzas' and (hasAuthority('REPORTE_VER') or hasRole('ADMIN')))"
            + " or (#rol == 'consulta' and (hasAuthority('PRODUCTO_VER') or hasRole('ADMIN')))")
    public DashboardDto dashboard(@PathVariable String rol,
                                  @RequestParam(required = false) Integer dias) {
        return service.obtener(rol, dias);
    }
}
