package com.motoboutique.compras.repository;

import com.motoboutique.compras.domain.RecepcionCompra;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface RecepcionCompraRepository extends JpaRepository<RecepcionCompra, Long>,
        JpaSpecificationExecutor<RecepcionCompra> {
}
