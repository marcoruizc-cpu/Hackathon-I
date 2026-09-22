package com.tuckersoft.branchengine.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreatePlaythroughRequest {

    @NotBlank(message = "el playerTag es obligatorio")
    @Size(min = 2, max = 40, message = "el playerTag debe tener entre 2 y 40 caracteres")
    private String playerTag;

    @NotBlank(message = "el startNodeCode es obligatorio")
    private String startNodeCode;
}
