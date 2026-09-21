package com.tuckersoft.branchengine.service;

import com.tuckersoft.branchengine.dto.CreateStoryNodeRequest;
import com.tuckersoft.branchengine.dto.StoryNodeResponse;
import com.tuckersoft.branchengine.exception.ApiException;
import com.tuckersoft.branchengine.model.StoryNode;
import com.tuckersoft.branchengine.repository.StoryNodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StoryNodeService {

    private final StoryNodeRepository storyNodeRepository;

    public StoryNodeResponse create(CreateStoryNodeRequest request) {
        if (storyNodeRepository.existsByNodeCode(request.getNodeCode())) {
            throw ApiException.conflict("El nodeCode ya existe");
        }

        StoryNode node = StoryNode.builder()
                .nodeCode(request.getNodeCode())
                .title(request.getTitle())
                .sceneText(request.getSceneText())
                .branchCapacity(request.getBranchCapacity())
                .currentBranches(0)
                .primaryBranchCode(request.getPrimaryBranchCode())
                .glitchBranchCode(request.getGlitchBranchCode())
                .createdAt(Instant.now())
                .build();

        storyNodeRepository.save(node);
        return toResponse(node);
    }

    public List<StoryNodeResponse> listAll() {
        return storyNodeRepository.findAll().stream().map(this::toResponse).toList();
    }

    public StoryNodeResponse getById(Long id) {
        StoryNode node = storyNodeRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Nodo no encontrado"));
        return toResponse(node);
    }

    public StoryNode getEntityByCode(String nodeCode) {
        return storyNodeRepository.findByNodeCode(nodeCode)
                .orElseThrow(() -> ApiException.notFound("Nodo no encontrado: " + nodeCode));
    }

    private StoryNodeResponse toResponse(StoryNode node) {
        return StoryNodeResponse.builder()
                .id(node.getId())
                .nodeCode(node.getNodeCode())
                .title(node.getTitle())
                .sceneText(node.getSceneText())
                .branchCapacity(node.getBranchCapacity())
                .currentBranches(node.getCurrentBranches())
                .primaryBranchCode(node.getPrimaryBranchCode())
                .glitchBranchCode(node.getGlitchBranchCode())
                .createdAt(node.getCreatedAt())
                .build();
    }
}
