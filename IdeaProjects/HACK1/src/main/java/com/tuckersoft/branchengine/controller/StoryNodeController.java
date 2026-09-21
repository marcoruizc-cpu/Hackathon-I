package com.tuckersoft.branchengine.controller;

import com.tuckersoft.branchengine.dto.CreateStoryNodeRequest;
import com.tuckersoft.branchengine.dto.StoryNodeResponse;
import com.tuckersoft.branchengine.service.StoryNodeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/nodes")
@RequiredArgsConstructor
public class StoryNodeController {

    private final StoryNodeService storyNodeService;

    @PostMapping
    public ResponseEntity<StoryNodeResponse> create(@Valid @RequestBody CreateStoryNodeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(storyNodeService.create(request));
    }

    @GetMapping
    public ResponseEntity<List<StoryNodeResponse>> listAll() {
        return ResponseEntity.ok(storyNodeService.listAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<StoryNodeResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(storyNodeService.getById(id));
    }
}
