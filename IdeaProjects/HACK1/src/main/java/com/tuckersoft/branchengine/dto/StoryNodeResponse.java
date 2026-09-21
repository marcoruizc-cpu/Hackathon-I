package com.tuckersoft.branchengine.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
@AllArgsConstructor
public class StoryNodeResponse {
    private Long id;
    private String nodeCode;
    private String title;
    private String sceneText;
    private Integer branchCapacity;
    private Integer currentBranches;
    private String primaryBranchCode;
    private String glitchBranchCode;
    private Instant createdAt;
}
