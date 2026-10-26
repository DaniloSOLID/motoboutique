package com.motoboutique.seguridad.dto;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Usuario que queda guardado en la sesión después de iniciar sesión.
 *
 * <p>Además de lo que exige Spring Security, guarda el id y el nombre para mostrarlos en la
 * barra superior sin consultar la base de datos en cada pantalla. Las autoridades son los
 * roles (ROLE_ADMIN) y los códigos de permiso (PRODUCTO_EDITAR).</p>
 *
 * <p>Implementa CredentialsContainer: Spring borra el hash de la contraseña de la memoria
 * en cuanto la autenticación termina.</p>
 */
public class UsuarioAutenticado implements UserDetails, CredentialsContainer {

    private static final long serialVersionUID = 1L;

    private final Long id;
    private final String nombre;
    private final String email;
    private String password;
    private final boolean activo;
    private final Set<GrantedAuthority> autoridades;

    public UsuarioAutenticado(Long id, String nombre, String email, String password, boolean activo,
                              Collection<? extends GrantedAuthority> autoridades) {
        this.id = id;
        this.nombre = nombre;
        this.email = email;
        this.password = password;
        this.activo = activo;
        this.autoridades = Collections.unmodifiableSet(new LinkedHashSet<GrantedAuthority>(autoridades));
    }

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getEmail() {
        return email;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return autoridades;
    }

    @Override
    public String getPassword() {
        return password;
    }

    /** El nombre de usuario para iniciar sesión es el correo. */
    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /** Un usuario desactivado no puede iniciar sesión. */
    @Override
    public boolean isEnabled() {
        return activo;
    }

    @Override
    public void eraseCredentials() {
        this.password = null;
    }

    @Override
    public boolean equals(Object otro) {
        if (this == otro) {
            return true;
        }
        if (!(otro instanceof UsuarioAutenticado usuario)) {
            return false;
        }
        return email.equalsIgnoreCase(usuario.email);
    }

    @Override
    public int hashCode() {
        return email.toLowerCase(Locale.ROOT).hashCode();
    }

    /** Sin contraseña a propósito. */
    @Override
    public String toString() {
        return "UsuarioAutenticado[id=" + id + ", email=" + email + ", activo=" + activo + "]";
    }
}