package com.motoboutique.shared.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Fuerza la generación del token CSRF en cada petición para que la cookie XSRF-TOKEN llegue al
 * navegador (Spring solo la escribe cuando alguien lee el token). El frontend la lee y la envía
 * en la cabecera X-XSRF-TOKEN en todas las peticiones que modifican datos.
 */
final class CsrfCookieFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain cadena) throws ServletException, IOException {
        CsrfToken token = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (token != null) {
            token.getToken();
        }
        cadena.doFilter(request, response);
    }
}
