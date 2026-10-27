package com.motoboutique.seguridad.service;

import com.motoboutique.seguridad.dto.PermisoDto;
import java.util.List;

/** Interfaz pública del módulo seguridad para consultar y cargar permisos. */
public interface PermisoService {

    /**
     * Crea el permiso si no existe; si existe, actualiza su descripción y módulo.
     * Es idempotente: se puede ejecutar en cada arranque (DataSeeder).
     */
    PermisoDto asegurar(String codigo, String descripcion, String modulo);

    List<PermisoDto> listar();
}
