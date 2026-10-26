package com.motoboutique.seguridad.domain;

import com.motoboutique.shared.audit.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Permiso granular del sistema (ejemplo: PRODUCTO_EDITAR, AJUSTE_APROBAR).
 *
 * <p>El código es inmutable porque se usa escrito en las anotaciones @PreAuthorize.
 * Los permisos los carga el DataSeeder; no se crean desde pantalla.</p>
 */
@Entity
@Table(name = "permisos")
@Getter
@Setter
@NoArgsConstructor
public class Permiso extends AuditableEntity {

    @Column(name = "codigo", nullable = false, length = 60, updatable = false)
    private String codigo;

    @Column(name = "descripcion", nullable = false, length = 200)
    private String descripcion;

    /** Módulo al que pertenece (catalogo, inventario, compras...), para agruparlos en pantalla. */
    @Column(name = "modulo", nullable = false, length = 40)
    private String modulo;
}
