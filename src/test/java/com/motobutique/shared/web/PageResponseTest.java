package com.motobutique.shared.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

class PageResponseTest {

    @Test
    void convierteUnaPaginaIntermedia() {
        PageImpl<String> page = new PageImpl<>(List.of("a", "b"), PageRequest.of(1, 2), 5);

        PageResponse<String> respuesta = PageResponse.de(page);

        assertEquals(List.of("a", "b"), respuesta.getContenido());
        assertEquals(1, respuesta.getNumero());
        assertEquals(2, respuesta.getPaginaActual());
        assertEquals(2, respuesta.getTamanio());
        assertEquals(5L, respuesta.getTotalElementos());
        assertEquals(3, respuesta.getTotalPaginas());
        assertEquals(3L, respuesta.getDesde());
        assertEquals(4L, respuesta.getHasta());
        assertFalse(respuesta.isPrimera());
        assertFalse(respuesta.isUltima());
        assertFalse(respuesta.isVacia());
    }

    @Test
    void ultimaPaginaIncompletaCalculaBienElRango() {
        PageImpl<String> page = new PageImpl<>(List.of("e"), PageRequest.of(2, 2), 5);

        PageResponse<String> respuesta = PageResponse.de(page);

        assertEquals(5L, respuesta.getDesde());
        assertEquals(5L, respuesta.getHasta());
        assertTrue(respuesta.isUltima());
        assertFalse(respuesta.isPrimera());
    }

    @Test
    void aplicaElConversorAlContenido() {
        PageImpl<String> page = new PageImpl<>(List.of("aa", "b"), PageRequest.of(0, 10), 2);

        PageResponse<Integer> respuesta = PageResponse.de(page, String::length);

        assertEquals(List.of(2, 1), respuesta.getContenido());
        assertEquals(1, respuesta.getTotalPaginas());
        assertTrue(respuesta.isPrimera());
        assertTrue(respuesta.isUltima());
    }

    @Test
    void paginaVaciaNoMuestraRango() {
        PageResponse<String> respuesta = PageResponse.vacia(20);

        assertTrue(respuesta.isVacia());
        assertEquals(0L, respuesta.getDesde());
        assertEquals(0L, respuesta.getHasta());
        assertEquals(0, respuesta.getTotalPaginas());
        assertTrue(respuesta.isPrimera());
        assertTrue(respuesta.isUltima());
    }
}
