package com.motoboutique.seguridad.service;

import com.motoboutique.seguridad.dto.RolDto;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** Interfaz pública del módulo seguridad para consultar y cargar roles. */
public interface RolService {

    /**
     * Crea el rol si no existe y garantiza que tenga, como mínimo, los permisos indicados.
     * Nunca quita permisos ni cambia el nombre o la descripción de un rol ya existente, para
     * respetar lo que haya configurado el administrador. Es idempotente (DataSeeder).
     *
     * @throws com.motoboutique.shared.exception.NegocioException si algún permiso no existe
     */
    RolDto asegurar(String codigo, String nombre, String descripcion, boolean sistema,
                    Collection<String> codigosPermisos);

    List<RolDto> listar();

    Optional<RolDto> buscarPorCodigo(String codigo);
}
