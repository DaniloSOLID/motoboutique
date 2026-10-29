package com.motoboutique.compras.repository;

import com.motoboutique.compras.domain.OrdenCompraDetalle;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrdenCompraDetalleRepository extends JpaRepository<OrdenCompraDetalle, Long> {
    List<OrdenCompraDetalle> findByOrdenIdAndEliminadoFalseOrderById(Long ordenId);
    Optional<OrdenCompraDetalle> findByIdAndOrdenIdAndEliminadoFalse(Long id, Long ordenId);
}
