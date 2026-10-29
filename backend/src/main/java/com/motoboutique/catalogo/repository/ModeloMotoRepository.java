package com.motoboutique.catalogo.repository;

import com.motoboutique.catalogo.domain.ModeloMoto;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ModeloMotoRepository extends JpaRepository<ModeloMoto, Long>, JpaSpecificationExecutor<ModeloMoto> {
    Optional<ModeloMoto> findByIdAndEliminadoFalse(Long id);
    List<ModeloMoto> findByEliminadoFalseAndMarcaActivoTrueOrderByNombre();
}
