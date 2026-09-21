package com.tuckersoft.branchengine.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateRoleRequest {

    @NotBlank(message = "el role es obligatorio")
    private String role;
}
