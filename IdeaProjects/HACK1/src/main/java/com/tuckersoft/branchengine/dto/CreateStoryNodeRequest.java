package com.tuckersoft.branchengine.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateStoryNodeRequest {

    @NotBlank(message = "el nodeCode es obligatorio")
    @Size(min = 3, max = 40, message = "el nodeCode debe tener entre 3 y 40 caracteres")
    private String nodeCode;

    @NotBlank(message = "el title es obligatorio")
    @Size(min = 3, max = 80, message = "el title debe tener entre 3 y 80 caracteres")
    private String title;

    @NotBlank(message = "el sceneText es obligatorio")
    @Size(min = 10, message = "el sceneText debe tener al menos 10 caracteres")
    private String sceneText;

    @NotNull(message = "el branchCapacity es obligatorio")
    @Positive(message = "el branchCapacity debe ser mayor a 0")
    private Integer branchCapacity;

    private String primaryBranchCode;

    private String glitchBranchCode;
}
