package com.motoboutique.inventario.repository;

import com.motoboutique.inventario.domain.TransferenciaDetalle;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransferenciaDetalleRepository extends JpaRepository<TransferenciaDetalle, Long> {
    List<TransferenciaDetalle> findByTransferenciaIdAndEliminadoFalseOrderById(Long transferenciaId);
}
