package com.motoboutique.catalogo.web;

import com.motoboutique.catalogo.dto.ImportacionDto;
import com.motoboutique.catalogo.dto.ImportacionMapeoDto;
import com.motoboutique.catalogo.dto.ImportacionResultadoDto;
import com.motoboutique.catalogo.service.ImportacionCatalogoService;
import com.motoboutique.seguridad.dto.UsuarioAutenticado;
import com.motoboutique.shared.exception.NegocioException;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/catalogo/importaciones")
@RequiredArgsConstructor
class ImportacionCatalogoController {
    private final ImportacionCatalogoService servicio;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('IMPORTAR_CATALOGO') or hasRole('ADMIN')")
    ImportacionResultadoDto detectar(@RequestParam("archivo") MultipartFile archivo) {
        return servicio.detectar(archivo);
    }

    @PostMapping(value = "/validar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('IMPORTAR_CATALOGO') or hasRole('ADMIN')")
    ImportacionResultadoDto validar(@RequestPart("archivo") MultipartFile archivo,
                                    @Valid @RequestPart("mapeo") ImportacionMapeoDto mapeo) {
        return servicio.validar(archivo, mapeo);
    }

    @PostMapping(value = "/confirmar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('IMPORTAR_CATALOGO') or hasRole('ADMIN')")
    ImportacionResultadoDto confirmar(@RequestPart("archivo") MultipartFile archivo,
                                      @Valid @RequestPart("mapeo") ImportacionMapeoDto mapeo,
                                      @AuthenticationPrincipal Object principal) {
        if (!(principal instanceof UsuarioAutenticado usuario)) {
            throw new NegocioException("No se pudo identificar al usuario que confirma la importación.");
        }
        return servicio.confirmar(archivo, mapeo, usuario.getId());
    }

    @GetMapping
    @PreAuthorize("hasAuthority('IMPORTAR_CATALOGO') or hasRole('ADMIN')")
    List<ImportacionDto> historial() { return servicio.historial(); }

    @GetMapping("/plantilla")
    @PreAuthorize("hasAuthority('IMPORTAR_CATALOGO') or hasRole('ADMIN')")
    ResponseEntity<byte[]> plantilla() {
        byte[] contenido = "codigo,nombre,descripcion,categoria_id,marca_id,precio_compra,precio_venta,stock_minimo,unidad,imagen_url,especificaciones\n".getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=plantilla_catalogo.csv")
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8")).body(contenido);
    }
}
