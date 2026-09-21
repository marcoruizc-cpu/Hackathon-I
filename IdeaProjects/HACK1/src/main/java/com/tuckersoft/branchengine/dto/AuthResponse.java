package com.tuckersoft.branchengine.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private String type;
    private String email;
    private String displayName;
    private String role;
}
