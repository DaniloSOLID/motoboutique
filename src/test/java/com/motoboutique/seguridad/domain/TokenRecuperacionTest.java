package com.motoboutique.seguridad.domain;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class TokenRecuperacionTest {

    private static final Instant AHORA = Instant.parse("2026-10-25T12:00:00Z");

    private TokenRecuperacion tokenQueExpiraEn(Instant expiraEn) {
        TokenRecuperacion token = new TokenRecuperacion();
        token.setExpiraEn(expiraEn);
        return token;
    }

    @Test
    void tokenSinUsarYAntesDeExpirarEsVigente() {
        TokenRecuperacion token = tokenQueExpiraEn(AHORA.plus(Duration.ofMinutes(30)));

        assertTrue(token.estaVigente(AHORA));
    }

    @Test
    void tokenExpiradoNoEsVigente() {
        TokenRecuperacion token = tokenQueExpiraEn(AHORA.plus(Duration.ofMinutes(30)));

        assertFalse(token.estaVigente(AHORA.plus(Duration.ofMinutes(31))));
        assertFalse(token.estaVigente(AHORA.plus(Duration.ofMinutes(30))));
    }

    @Test
    void tokenYaUsadoNoEsVigenteAunqueNoHayaExpirado() {
        TokenRecuperacion token = tokenQueExpiraEn(AHORA.plus(Duration.ofMinutes(30)));

        token.marcarUsado(AHORA);

        assertFalse(token.estaVigente(AHORA.plusSeconds(1)));
    }
}
