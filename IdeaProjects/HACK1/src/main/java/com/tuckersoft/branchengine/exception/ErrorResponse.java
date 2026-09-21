package com.tuckersoft.branchengine.exception;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;

@Getter
@AllArgsConstructor
public class ErrorResponse {
    private String error;
    private String message;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant timestamp;
    private String path;
}
