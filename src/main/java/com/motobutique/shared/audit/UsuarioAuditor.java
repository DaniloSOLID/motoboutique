package com.motobutique.shared.audit;

import java.util.Optional;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Indica a Spring Data quién es el "auditor" actual (el usuario autenticado).
 * Cuando no hay sesión (por ejemplo el DataSeeder al arrancar), registra "sistema".
 */
@Component("auditorAware")
public class UsuarioAuditor implements AuditorAware<String> {

    public static final String SISTEMA = "sistema";

    @Override
    public Optional<String> getCurrentAuditor() {
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacion == null
                || !autenticacion.isAuthenticated()
                || autenticacion instanceof AnonymousAuthenticationToken) {
            return Optional.of(SISTEMA);
        }
        return Optional.of(autenticacion.getName());
    }
}
