package com.tuckersoft.branchengine.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequest {

    @NotBlank(message = "el email es obligatorio")
    @Email(message = "formato de email invalido")
    private String email;

    @NotBlank(message = "la contrasena es obligatoria")
    @Size(min = 6, message = "la contrasena debe tener al menos 6 caracteres")
    private String password;

    @NotBlank(message = "el displayName es obligatorio")
    @Size(min = 3, max = 60, message = "el displayName debe tener entre 3 y 60 caracteres")
    private String displayName;

    // Si el JSON trae un campo "role", se ignora: el registro siempre queda como ROLE_USER.
    private String role;
}
