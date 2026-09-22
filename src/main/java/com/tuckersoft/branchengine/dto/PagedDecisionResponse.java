package com.tuckersoft.branchengine.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class PagedDecisionResponse {
    private List<DecisionResponse> content;
    private long totalElements;
    private int totalPages;
    private int currentPage;
    private int size;
}
