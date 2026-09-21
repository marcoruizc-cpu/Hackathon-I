package com.tuckersoft.branchengine.controller;

import com.tuckersoft.branchengine.dto.UpdateRoleRequest;
import com.tuckersoft.branchengine.dto.UserResponse;
import com.tuckersoft.branchengine.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(Authentication authentication) {
        return ResponseEntity.ok(userService.getCurrentUser(authentication.getName()));
    }

    @GetMapping
    public ResponseEntity<List<UserResponse>> listAll() {
        return ResponseEntity.ok(userService.listAll());
    }

    @PatchMapping("/{id}/role")
    public ResponseEntity<UserResponse> updateRole(@PathVariable Long id,
                                                     @Valid @RequestBody UpdateRoleRequest request,
                                                     Authentication authentication) {
        return ResponseEntity.ok(userService.updateRole(id, request, authentication.getName()));
    }
}
