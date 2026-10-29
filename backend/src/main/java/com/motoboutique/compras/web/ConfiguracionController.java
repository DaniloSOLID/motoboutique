package com.motoboutique.compras.web;

import com.motoboutique.compras.domain.ConfiguracionEmpresa;
import com.motoboutique.compras.repository.ConfiguracionEmpresaRepository;
import com.motoboutique.inventario.domain.Almacen;
import com.motoboutique.inventario.repository.AlmacenRepository;
import com.motoboutique.shared.audit.AuditoriaService;
import com.motoboutique.shared.exception.NegocioException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/configuracion")
@RequiredArgsConstructor
public class ConfiguracionController {

    private final ConfiguracionEmpresaRepository configuracionRepository;
    private final AlmacenRepository almacenRepository;
    private final AuditoriaService auditoriaService;

    @GetMapping
    @PreAuthorize("hasAuthority('CONFIG_VER') or hasRole('ADMIN')")
    public ConfiguracionDto obtener() {
        return convertir(configuracion());
    }

    @PutMapping
    @PreAuthorize("hasAuthority('CONFIG_EDITAR') or hasRole('ADMIN')")
    public ConfiguracionDto actualizar(@Valid @RequestBody ConfiguracionSolicitud solicitud) {
        if (solicitud.ruc() != null && !solicitud.ruc().isBlank()
                && !solicitud.ruc().matches("\\d{11}")) {
            throw new NegocioException("El RUC debe tener exactamente 11 dígitos.");
        }
        ConfiguracionEmpresa config = configuracion();
        config.setRazonSocial(limpiar(solicitud.razonSocial()));
        config.setRuc(limpiar(solicitud.ruc()));
        config.setMoneda(solicitud.moneda().toUpperCase(java.util.Locale.ROOT));
        config.setIgv(solicitud.igv());
        config.setStockMinimoDefault(solicitud.stockMinimoDefault());
        if (solicitud.almacenDefectoId() == null) {
            config.setAlmacenDefecto(null);
        } else {
            Almacen almacen = almacenRepository.findById(solicitud.almacenDefectoId())
                    .filter(a -> a.isActivo() && !a.isEliminado())
                    .orElseThrow(() -> new NegocioException("El almacén por defecto no existe o está inactivo."));
            config.setAlmacenDefecto(almacen);
        }
        config.setActualizadoEn(Instant.now());
        ConfiguracionEmpresa guardada = configuracionRepository.save(config);
        auditoriaService.registrar("ACTUALIZAR_CONFIGURACION", "CONFIGURACION", "1",
                "Se actualizaron los datos de configuración de la empresa.");
        return convertir(guardada);
    }

    private ConfiguracionEmpresa configuracion() {
        return configuracionRepository.findById((short) 1)
                .orElseThrow(() -> new NegocioException("No se encontró la configuración de empresa."));
    }

    private ConfiguracionDto convertir(ConfiguracionEmpresa c) {
        return new ConfiguracionDto(c.getRazonSocial(), c.getRuc(), c.getMoneda(), c.getIgv(),
                c.getAlmacenDefecto() == null ? null : c.getAlmacenDefecto().getId(),
                c.getAlmacenDefecto() == null ? null : c.getAlmacenDefecto().getNombre(),
                c.getStockMinimoDefault(), c.getActualizadoEn());
    }

    private String limpiar(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    public record ConfiguracionSolicitud(
            @Size(max = 180) String razonSocial,
            @Pattern(regexp = "^$|^\\d{11}$", message = "El RUC debe tener exactamente 11 dígitos.")
            String ruc,
            @NotBlank @Pattern(regexp = "[A-Za-z]{3}", message = "La moneda debe usar un código de tres letras.")
            String moneda,
            @DecimalMin("0.00") @DecimalMax("100.00") @Digits(integer = 3, fraction = 2)
            @NotNull BigDecimal igv,
            Long almacenDefectoId,
            @NotNull @DecimalMin("0.000") @Digits(integer = 11, fraction = 3) BigDecimal stockMinimoDefault) {
        public ConfiguracionSolicitud {
            if (moneda != null) {
                moneda = moneda.trim();
            }
        }
    }

    public record ConfiguracionDto(String razonSocial, String ruc, String moneda, BigDecimal igv,
            Long almacenDefectoId, String almacenDefecto, BigDecimal stockMinimoDefault,
            Instant actualizadoEn) {}
}
