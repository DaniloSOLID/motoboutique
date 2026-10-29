package com.motoboutique.compras.repository;

import com.motoboutique.compras.domain.RecepcionDetalle;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecepcionDetalleRepository extends JpaRepository<RecepcionDetalle, Long> {
    List<RecepcionDetalle> findByRecepcionIdAndEliminadoFalseOrderById(Long recepcionId);
}
