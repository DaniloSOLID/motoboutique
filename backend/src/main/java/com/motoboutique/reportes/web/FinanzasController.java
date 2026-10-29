package com.motoboutique.reportes.web;

import com.motoboutique.reportes.dto.ValorizacionDto;
import com.motoboutique.reportes.service.ValorizacionService;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/finanzas")
@RequiredArgsConstructor
public class FinanzasController {
    private final ValorizacionService service;

    @GetMapping("/valorizacion")
    @PreAuthorize("hasAuthority('REPORTE_VER') or hasRole('ADMIN')")
    public ValorizacionDto valorizacion(
            @RequestParam(required = false) LocalDate fechaCorte,
            @RequestParam(required = false) Long almacenId,
            @RequestParam(required = false) Long categoriaId,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.valorizar(fechaCorte, almacenId, categoriaId, q, page, size);
    }
}
