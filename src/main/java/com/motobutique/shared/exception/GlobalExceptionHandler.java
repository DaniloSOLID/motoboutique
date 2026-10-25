package com.motobutique.shared.exception;

import com.motobutique.shared.web.Flash;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.support.RequestContextUtils;
import org.springframework.web.servlet.view.RedirectView;

/**
 * Manejo global de errores para las pantallas (Thymeleaf).
 *
 * <ul>
 *   <li>Errores de negocio en un POST: vuelve a la página anterior con el mensaje flash "error".</li>
 *   <li>Errores en un GET: muestra la página de error con el código HTTP correcto.</li>
 *   <li>Errores inesperados (500): nunca muestran detalles técnicos; solo un código de referencia
 *       que también queda en el log.</li>
 * </ul>
 *
 * <p>Los endpoints JSON (@RestController) los atiende {@link ApiExceptionHandler}.</p>
 */
@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final String VISTA_ERROR = "error/general";

    @ExceptionHandler(NegocioException.class)
    public ModelAndView manejarNegocio(NegocioException ex, HttpServletRequest request) {
        log.warn("Regla de negocio incumplida en {} {}: {}",
                request.getMethod(), request.getRequestURI(), ex.getMessage());
        return responder(request, 422, ex.getMessage());
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ModelAndView manejarNoEncontrado(RecursoNoEncontradoException ex) {
        return vistaError(404, ex.getMessage(), null);
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ModelAndView manejarBloqueoOptimista(OptimisticLockingFailureException ex,
                                                HttpServletRequest request) {
        log.warn("Conflicto de versión en {} {}", request.getMethod(), request.getRequestURI());
        return responder(request, 409, MensajesHttp.detalle(409));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ModelAndView manejarIntegridad(DataIntegrityViolationException ex,
                                          HttpServletRequest request) {
        log.warn("Violación de integridad en {} {}: {}", request.getMethod(),
                request.getRequestURI(), ex.getMostSpecificCause().getMessage());
        return responder(request, 409,
                "No se pudo guardar: el dato ya existe o está relacionado con otros registros.");
    }

    /** Se maneja de forma explícita; si no, el manejador general lo convertiría en un 500. */
    @ExceptionHandler(AccessDeniedException.class)
    public ModelAndView manejarAccesoDenegado(AccessDeniedException ex, HttpServletRequest request) {
        log.warn("Acceso denegado a {} {} (usuario: {})", request.getMethod(),
                request.getRequestURI(), request.getRemoteUser());
        return vistaError(403, MensajesHttp.detalle(403), null);
    }

    @ExceptionHandler(Exception.class)
    public ModelAndView manejarGeneral(Exception ex, HttpServletRequest request) {
        // Las excepciones de Spring MVC (404, 405, 415...) traen su propio código HTTP.
        int codigo = ex instanceof ErrorResponse respuesta ? respuesta.getStatusCode().value() : 500;
        String referencia = null;
        if (codigo >= 500) {
            referencia = MensajesHttp.nuevaReferencia();
            log.error("Error inesperado [ref {}] en {} {}", referencia,
                    request.getMethod(), request.getRequestURI(), ex);
        } else {
            log.debug("Respuesta {} en {} {}: {}", codigo, request.getMethod(),
                    request.getRequestURI(), ex.getMessage());
        }
        return vistaError(codigo, MensajesHttp.detalle(codigo), referencia);
    }

    // ------------------------------------------------------------------
    // Utilidades internas
    // ------------------------------------------------------------------

    /** GET/HEAD: muestra la página de error. Otros métodos: vuelve atrás con mensaje flash. */
    private ModelAndView responder(HttpServletRequest request, int codigo, String mensaje) {
        if (esLectura(request)) {
            return vistaError(codigo, mensaje, null);
        }
        return redirigirConError(request, mensaje);
    }

    private ModelAndView vistaError(int codigo, String detalle, String referencia) {
        ModelAndView mav = new ModelAndView(VISTA_ERROR);
        mav.setStatus(HttpStatusCode.valueOf(codigo));
        mav.addObject("codigo", codigo);
        mav.addObject("titulo", MensajesHttp.titulo(codigo));
        mav.addObject("detalle", detalle);
        mav.addObject("referencia", referencia);
        return mav;
    }

    private ModelAndView redirigirConError(HttpServletRequest request, String mensaje) {
        RequestContextUtils.getOutputFlashMap(request).put(Flash.ERROR, mensaje);
        RedirectView vista = new RedirectView(destinoSeguro(request), true);
        vista.setExpandUriTemplateVariables(false);
        vista.setExposeModelAttributes(false);
        vista.setStatusCode(HttpStatus.SEE_OTHER);
        return new ModelAndView(vista);
    }

    /**
     * Toma la página anterior (cabecera Referer) pero SOLO ruta y parámetros, nunca el dominio,
     * para que nadie pueda usarla como redirección hacia un sitio externo.
     */
    private String destinoSeguro(HttpServletRequest request) {
        String referer = request.getHeader(HttpHeaders.REFERER);
        if (referer == null || referer.isBlank()) {
            return "/";
        }
        try {
            URI uri = URI.create(referer);
            if (uri.getHost() != null && !uri.getHost().equalsIgnoreCase(request.getServerName())) {
                return "/";
            }
            String ruta = uri.getRawPath();
            if (ruta == null || !ruta.startsWith("/") || ruta.startsWith("//")) {
                return "/";
            }
            String consulta = uri.getRawQuery();
            return consulta == null ? ruta : ruta + "?" + consulta;
        } catch (IllegalArgumentException e) {
            return "/";
        }
    }

    private boolean esLectura(HttpServletRequest request) {
        String metodo = request.getMethod();
        return "GET".equalsIgnoreCase(metodo) || "HEAD".equalsIgnoreCase(metodo);
    }
}
