package com.motoboutique.seguridad.repository;

import com.motoboutique.seguridad.domain.Permiso;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Solo lo usa el módulo seguridad. Los demás módulos pasan por los Service. */
public interface PermisoRepository extends JpaRepository<Permiso, Long> {

    Optional<Permiso> findByCodigo(String codigo);

    List<Permiso> findByCodigoIn(Collection<String> codigos);

    List<Permiso> findByEliminadoFalseOrderByModuloAscCodigoAsc();
}
