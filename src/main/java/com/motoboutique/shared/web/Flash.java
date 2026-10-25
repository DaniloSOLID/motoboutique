package com.motoboutique.shared.web;

import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Mensajes flash: sobreviven a una redirección y se muestran una sola vez.
 * Las plantillas leen los atributos "mensaje" (éxito) y "error" desde el fragment toast-flash.
 */
public final class Flash {

    /** Atributo para mensajes de éxito o informativos. */
    public static final String MENSAJE = "mensaje";

    /** Atributo para mensajes de error. */
    public static final String ERROR = "error";

    private Flash() {
    }

    public static void exito(RedirectAttributes atributos, String texto) {
        atributos.addFlashAttribute(MENSAJE, texto);
    }

    public static void error(RedirectAttributes atributos, String texto) {
        atributos.addFlashAttribute(ERROR, texto);
    }
}
