package com.motoboutique.catalogo.repository;

import com.motoboutique.catalogo.domain.Importacion;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImportacionRepository extends JpaRepository<Importacion, Long> {
    List<Importacion> findAllByEliminadoFalseOrderByFechaDesc();
}
