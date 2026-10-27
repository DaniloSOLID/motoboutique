package com.motoboutique.seguridad.repository;

import com.motoboutique.seguridad.domain.Rol;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

/** Solo lo usa el módulo seguridad. Los permisos se cargan junto con el rol (sin consultas extra). */
public interface RolRepository extends JpaRepository<Rol, Long> {

    @EntityGraph(attributePaths = "permisos")
    Optional<Rol> findByCodigo(String codigo);

    @EntityGraph(attributePaths = "permisos")
    List<Rol> findByCodigoIn(Collection<String> codigos);

    @EntityGraph(attributePaths = "permisos")
    List<Rol> findByEliminadoFalseOrderByNombreAsc();
}
