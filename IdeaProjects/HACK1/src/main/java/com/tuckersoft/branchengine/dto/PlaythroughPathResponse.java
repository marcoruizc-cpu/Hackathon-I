package com.tuckersoft.branchengine.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class PlaythroughPathResponse {
    private Long playthroughId;
    private String playerTag;
    private String status;
    private String endingCode;
    private String startNodeCode;
    private String currentNodeCode;
    private List<PathStep> steps;

    @Getter
    @Builder
    @AllArgsConstructor
    public static class PathStep {
        private Integer order;
        private Long decisionId;
        private String fromNodeCode;
        private String toNodeCode;
        private String branchType;
        private String impactLevel;
        private java.time.Instant createdAt;
    }
}
