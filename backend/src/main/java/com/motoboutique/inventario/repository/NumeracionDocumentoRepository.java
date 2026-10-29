package com.motoboutique.inventario.repository;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class NumeracionDocumentoRepository {
    private final EntityManager entityManager;

    public String siguienteNumero(String prefijo) {
        return (String) entityManager.createNativeQuery("select siguiente_numero(:prefijo)")
                .setParameter("prefijo", prefijo)
                .getSingleResult();
    }
}
