package com.motoboutique.seguridad.service;

import com.motoboutique.seguridad.dto.RolDto;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** Interfaz pública del módulo seguridad para consultar y cargar roles. */
public interface RolService {

    /**
     * Crea el rol con los permisos iniciales cuando no existe. No sincroniza roles ya creados:
     * los permisos asignados manualmente por administración deben persistir entre reinicios.
     *
     * @throws com.motoboutique.shared.exception.NegocioException si algún permiso no existe
     */
    RolDto asegurar(String codigo, String nombre, String descripcion, boolean sistema,
                    Collection<String> codigosPermisos);

    List<RolDto> listar();

    Optional<RolDto> buscarPorCodigo(String codigo);

    RolDto actualizarPermisos(Long id, Collection<String> codigosPermisos);
}
