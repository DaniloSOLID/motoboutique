package com.motoboutique.inventario.repository;

import com.motoboutique.inventario.domain.ConteoDetalle;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConteoDetalleRepository extends JpaRepository<ConteoDetalle, Long> {
    List<ConteoDetalle> findByConteoIdAndEliminadoFalseOrderByProductoNombre(Long conteoId);
    Optional<ConteoDetalle> findByConteoIdAndProductoIdAndEliminadoFalse(Long conteoId, Long productoId);
}
