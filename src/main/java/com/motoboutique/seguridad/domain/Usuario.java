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
 * Usuario del sistema (personal de la tienda). No se borra: se desactiva.
 *
 * <p>El correo se guarda siempre en minúsculas y es único sin distinguir mayúsculas
 * (índice uq_usuarios_email sobre lower(email)).</p>
 */
@Entity
@Table(name = "usuarios")
@Getter
@Setter
@NoArgsConstructor
public class Usuario extends AuditableEntity {

    @Column(name = "nombre", nullable = false, length = 120)
    private String nombre;

    @Column(name = "email", nullable = false, length = 150)
    private String email;

    /** Hash BCrypt. Nunca la contraseña en claro. */
    @Column(name = "password", nullable = false, length = 100)
    private String password;

    @Column(name = "telefono", length = 20)
    private String telefono;

    @Column(name = "activo", nullable = false)
    private boolean activo = true;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "usuarios_roles",
            joinColumns = @JoinColumn(name = "usuario_id"),
            inverseJoinColumns = @JoinColumn(name = "rol_id"))
    private Set<Rol> roles = new HashSet<>();

    public boolean tieneRol(String codigoRol) {
        return roles.stream().anyMatch(rol -> rol.getCodigo().equals(codigoRol));
    }
}
