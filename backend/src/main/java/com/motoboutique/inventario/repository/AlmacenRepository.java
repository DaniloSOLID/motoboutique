package com.motoboutique.inventario.repository;

import com.motoboutique.inventario.domain.Almacen;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface AlmacenRepository extends JpaRepository<Almacen, Long>,
        JpaSpecificationExecutor<Almacen> {
    Optional<Almacen> findByIdAndEliminadoFalse(Long id);
    boolean existsByCodigoIgnoreCase(String codigo);
    boolean existsByNombreIgnoreCase(String nombre);
    boolean existsByCodigoIgnoreCaseAndEliminadoFalse(String codigo);
    List<Almacen> findByEliminadoFalseOrderByNombre();
}
