package com.motoboutique.seguridad.dto;

import com.motoboutique.seguridad.domain.Permiso;
import java.util.List;
import org.mapstruct.Mapper;

@Mapper
public interface PermisoMapper {

    PermisoDto toDto(Permiso permiso);

    List<PermisoDto> toDtoList(List<Permiso> permisos);
}
