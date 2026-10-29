package com.motoboutique.compras.repository;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class CodigoProveedorRepository {
    private final EntityManager entityManager;

    public String siguienteCodigo() {
        Number numero = (Number) entityManager.createNativeQuery(
                "select nextval('seq_numero_proveedor')").getSingleResult();
        return "PROV-" + String.format("%06d", numero.longValue());
    }
}
