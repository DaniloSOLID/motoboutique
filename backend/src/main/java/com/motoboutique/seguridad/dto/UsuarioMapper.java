package com.motoboutique.seguridad.dto;

import com.motoboutique.seguridad.domain.Rol;
import com.motoboutique.seguridad.domain.Usuario;
import java.util.List;
import java.util.Set;
import org.mapstruct.Mapper;

@Mapper
public interface UsuarioMapper {

    UsuarioDto toDto(Usuario usuario);

    List<UsuarioDto> toDtoList(List<Usuario> usuarios);

    /** MapStruct usa este método para convertir Set&lt;Rol&gt; en la lista de códigos del DTO. */
    default List<String> rolesACodigos(Set<Rol> roles) {
        if (roles == null) {
            return List.of();
        }
        return roles.stream().map(Rol::getCodigo).sorted().toList();
    }
}
