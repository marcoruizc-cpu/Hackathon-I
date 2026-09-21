package com.tuckersoft.branchengine.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequest {

    @NotBlank(message = "el email es obligatorio")
    private String email;

    @NotBlank(message = "la contrasena es obligatoria")
    private String password;
}
