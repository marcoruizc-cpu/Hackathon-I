package com.tuckersoft.branchengine.controller;

import com.tuckersoft.branchengine.dto.*;
import com.tuckersoft.branchengine.model.User;
import com.tuckersoft.branchengine.service.DecisionService;
import com.tuckersoft.branchengine.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/decisions")
@RequiredArgsConstructor
public class DecisionController {

    private static final String SIMULATE_HEADER = "X-Bandersnatch-Simulate";
    private static final String SIMULATE_MAIL_FAILURE = "MAIL_FAILURE";

    private final DecisionService decisionService;
    private final UserService userService;

    @PostMapping
    public ResponseEntity<DecisionResponse> create(@Valid @RequestBody CreateDecisionRequest request,
                                                     @RequestHeader(value = SIMULATE_HEADER, required = false) String simulateHeader,
                                                     Authentication authentication) {
        User currentUser = userService.getByEmail(authentication.getName());
        boolean simulateFailure = SIMULATE_MAIL_FAILURE.equals(simulateHeader);
        DecisionResponse response = decisionService.createDecision(request, currentUser, simulateFailure);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<PagedDecisionResponse> list(
            @RequestParam(required = false) String branchType,
            @RequestParam(required = false) String impactLevel,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long playthroughId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        User currentUser = userService.getByEmail(authentication.getName());
        return ResponseEntity.ok(decisionService.list(branchType, impactLevel, status, playthroughId, page, size, currentUser));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DecisionResponse> getById(@PathVariable Long id, Authentication authentication) {
        User currentUser = userService.getByEmail(authentication.getName());
        return ResponseEntity.ok(decisionService.getById(id, currentUser));
    }

    @GetMapping("/{id}/reality-logs")
    public ResponseEntity<List<RealityLogResponse>> getRealityLogs(@PathVariable Long id, Authentication authentication) {
        User currentUser = userService.getByEmail(authentication.getName());
        return ResponseEntity.ok(decisionService.getRealityLogs(id, currentUser));
    }
}
