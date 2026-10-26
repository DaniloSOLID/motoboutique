package com.motoboutique.seguridad.dto;

import com.motoboutique.seguridad.domain.Permiso;
import com.motoboutique.seguridad.domain.Rol;
import java.util.List;
import java.util.Set;
import org.mapstruct.Mapper;

@Mapper
public interface RolMapper {

    RolDto toDto(Rol rol);

    List<RolDto> toDtoList(List<Rol> roles);

    /** MapStruct usa este método para convertir Set&lt;Permiso&gt; en la lista de códigos del DTO. */
    default List<String> permisosACodigos(Set<Permiso> permisos) {
        if (permisos == null) {
            return List.of();
        }
        return permisos.stream().map(Permiso::getCodigo).sorted().toList();
    }
}
