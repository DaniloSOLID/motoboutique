package com.motoboutique.shared.config;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.MediaType;

/**
 * Respuestas de error JSON (formato ProblemDetail, RFC 9457) para los fallos que ocurren dentro
 * de la cadena de filtros de Spring Security, donde no llegan los manejadores de @ControllerAdvice.
 * Los textos son constantes en español: no se interpolan datos del usuario.
 */
final class RespuestasSeguridad {

    private RespuestasSeguridad() {
    }

    static void escribir(HttpServletResponse respuesta, int codigo, String titulo, String detalle)
            throws IOException {
        respuesta.setStatus(codigo);
        respuesta.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        respuesta.setCharacterEncoding(StandardCharsets.UTF_8.name());
        respuesta.getWriter().write("{\"type\":\"about:blank\",\"title\":\"" + escapar(titulo)
                + "\",\"status\":" + codigo + ",\"detail\":\"" + escapar(detalle) + "\"}");
    }

    private static String escapar(String texto) {
        return texto.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
