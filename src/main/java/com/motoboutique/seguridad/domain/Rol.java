package com.motoboutique.seguridad.domain;

import com.motoboutique.shared.audit.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Rol de usuario (ADMIN, ALMACENERO, CONTADOR, CONSULTA...). Agrupa permisos.
 */
@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
public class Rol extends AuditableEntity {

    @Column(name = "codigo", nullable = false, length = 40, updatable = false)
    private String codigo;

    @Column(name = "nombre", nullable = false, length = 80)
    private String nombre;

    @Column(name = "descripcion", length = 200)
    private String descripcion;

    /** Los roles de sistema son los base: no se pueden borrar desde la pantalla de administración. */
    @Column(name = "sistema", nullable = false)
    private boolean sistema = false;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "roles_permisos",
            joinColumns = @JoinColumn(name = "rol_id"),
            inverseJoinColumns = @JoinColumn(name = "permiso_id"))
    private Set<Permiso> permisos = new HashSet<>();

    public boolean tienePermiso(String codigoPermiso) {
        return permisos.stream().anyMatch(permiso -> permiso.getCodigo().equals(codigoPermiso));
    }
}
