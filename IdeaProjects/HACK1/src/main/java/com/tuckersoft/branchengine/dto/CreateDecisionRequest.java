package com.tuckersoft.branchengine.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateDecisionRequest {

    @NotNull(message = "el playthroughId es obligatorio")
    private Long playthroughId;

    @NotBlank(message = "el rawInput es obligatorio")
    @Size(min = 10, message = "el rawInput debe tener al menos 10 caracteres")
    private String rawInput;

    @NotBlank(message = "el impactLevel es obligatorio")
    private String impactLevel;
}
