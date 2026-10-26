package com.motoboutique.shared.audit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class AuditableEntityTest {

    private static class EntidadA extends AuditableEntity {
    }

    private static class EntidadB extends AuditableEntity {
    }

    /** El setter de id es protegido a propósito, así que en las pruebas se asigna por reflexión. */
    private static <T extends AuditableEntity> T conId(T entidad, Long id) {
        ReflectionTestUtils.setField(entidad, "id", id);
        return entidad;
    }

    @Test
    void entidadesDeLaMismaClaseConElMismoIdSonIguales() {
        EntidadA primera = conId(new EntidadA(), 1L);
        EntidadA segunda = conId(new EntidadA(), 1L);

        assertEquals(primera, segunda);
        assertEquals(primera.hashCode(), segunda.hashCode());
    }

    @Test
    void entidadesConIdDistintoNoSonIguales() {
        assertNotEquals(conId(new EntidadA(), 1L), conId(new EntidadA(), 2L));
    }

    @Test
    void mismoIdPeroClaseDistintaNoSonIguales() {
        assertNotEquals(conId(new EntidadA(), 1L), conId(new EntidadB(), 1L));
    }

    @Test
    void entidadesNuevasSinIdSoloSonIgualesASiMismas() {
        EntidadA nueva = new EntidadA();

        assertEquals(nueva, nueva);
        assertNotEquals(new EntidadA(), new EntidadA());
    }
}
