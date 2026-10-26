package com.motoboutique.seguridad.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Datos para crear un usuario (formulario de administración y DataSeeder).
 * Sin toString a propósito: así la contraseña nunca aparece en un log por accidente.
 */
@Getter
@Setter
@NoArgsConstructor
public class UsuarioCrearDto {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 120, message = "El nombre admite hasta 120 caracteres")
    private String nombre;

    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "El correo no tiene un formato válido")
    @Size(max = 150, message = "El correo admite hasta 150 caracteres")
    private String email;

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres")
    private String password;

    @Size(max = 20, message = "El teléfono admite hasta 20 caracteres")
    private String telefono;

    /** Códigos de rol (ADMIN, ALMACENERO...). */
    @NotEmpty(message = "Debe asignar al menos un rol")
    private Set<String> roles = new LinkedHashSet<>();

    public static UsuarioCrearDto de(String nombre, String email, String password,
                                     String telefono, Collection<String> roles) {
        UsuarioCrearDto dto = new UsuarioCrearDto();
        dto.setNombre(nombre);
        dto.setEmail(email);
        dto.setPassword(password);
        dto.setTelefono(telefono);
        dto.setRoles(new LinkedHashSet<>(roles));
        return dto;
    }
}
