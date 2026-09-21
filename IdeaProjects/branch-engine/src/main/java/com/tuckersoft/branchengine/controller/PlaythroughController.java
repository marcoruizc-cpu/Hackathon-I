package com.tuckersoft.branchengine.controller;

import com.tuckersoft.branchengine.dto.CreatePlaythroughRequest;
import com.tuckersoft.branchengine.dto.PlaythroughPathResponse;
import com.tuckersoft.branchengine.dto.PlaythroughResponse;
import com.tuckersoft.branchengine.model.User;
import com.tuckersoft.branchengine.service.PlaythroughService;
import com.tuckersoft.branchengine.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/playthroughs")
@RequiredArgsConstructor
public class PlaythroughController {

    private final PlaythroughService playthroughService;
    private final UserService userService;

    @PostMapping
    public ResponseEntity<PlaythroughResponse> create(@Valid @RequestBody CreatePlaythroughRequest request,
                                                        Authentication authentication) {
        User currentUser = userService.getByEmail(authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(playthroughService.create(request, currentUser));
    }

    @GetMapping
    public ResponseEntity<List<PlaythroughResponse>> list(Authentication authentication) {
        User currentUser = userService.getByEmail(authentication.getName());
        return ResponseEntity.ok(playthroughService.listForUser(currentUser));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PlaythroughResponse> getById(@PathVariable Long id, Authentication authentication) {
        User currentUser = userService.getByEmail(authentication.getName());
        return ResponseEntity.ok(playthroughService.getById(id, currentUser));
    }

    @GetMapping("/{id}/path")
    public ResponseEntity<PlaythroughPathResponse> getPath(@PathVariable Long id, Authentication authentication) {
        User currentUser = userService.getByEmail(authentication.getName());
        return ResponseEntity.ok(playthroughService.getPath(id, currentUser));
    }
}
