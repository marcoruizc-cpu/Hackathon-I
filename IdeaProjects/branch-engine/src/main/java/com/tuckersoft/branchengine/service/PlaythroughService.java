package com.tuckersoft.branchengine.service;

import com.tuckersoft.branchengine.dto.CreatePlaythroughRequest;
import com.tuckersoft.branchengine.dto.PlaythroughPathResponse;
import com.tuckersoft.branchengine.dto.PlaythroughResponse;
import com.tuckersoft.branchengine.exception.ApiException;
import com.tuckersoft.branchengine.model.Decision;
import com.tuckersoft.branchengine.model.Playthrough;
import com.tuckersoft.branchengine.model.StoryNode;
import com.tuckersoft.branchengine.model.User;
import com.tuckersoft.branchengine.repository.DecisionRepository;
import com.tuckersoft.branchengine.repository.PlaythroughRepository;
import com.tuckersoft.branchengine.repository.StoryNodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PlaythroughService {

    private static final String ROLE_ADMIN = "ROLE_ADMIN";

    private final PlaythroughRepository playthroughRepository;
    private final StoryNodeRepository storyNodeRepository;
    private final DecisionRepository decisionRepository;

    @Transactional
    public PlaythroughResponse create(CreatePlaythroughRequest request, User currentUser) {
        StoryNode node = storyNodeRepository.findByNodeCode(request.getStartNodeCode())
                .orElseThrow(() -> ApiException.notFound("startNodeCode no existe"));

        if (playthroughRepository.existsByPlayerTag(request.getPlayerTag())) {
            throw ApiException.conflict("El playerTag ya existe");
        }

        if (node.getCurrentBranches() >= node.getBranchCapacity()) {
            throw ApiException.badRequest("El nodo esta lleno");
        }

        Instant now = Instant.now();
        Playthrough playthrough = Playthrough.builder()
                .playerTag(request.getPlayerTag())
                .user(currentUser)
                .startNodeCode(node.getNodeCode())
                .currentNode(node)
                .lucidity(100)
                .controlLevel(0)
                .status("ACTIVA")
                .endingCode(null)
                .createdAt(now)
                .updatedAt(now)
                .build();

        node.setCurrentBranches(node.getCurrentBranches() + 1);
        storyNodeRepository.save(node);

        playthroughRepository.save(playthrough);
        return toResponse(playthrough);
    }

    public List<PlaythroughResponse> listForUser(User currentUser) {
        List<Playthrough> playthroughs = ROLE_ADMIN.equals(currentUser.getRole())
                ? playthroughRepository.findAllByOrderByCreatedAtDesc()
                : playthroughRepository.findByUserOrderByCreatedAtDesc(currentUser);

        return playthroughs.stream().map(this::toResponse).toList();
    }

    public PlaythroughResponse getById(Long id, User currentUser) {
        Playthrough playthrough = getEntity(id);
        assertCanView(playthrough, currentUser);
        return toResponse(playthrough);
    }

    public Playthrough getEntity(Long id) {
        return playthroughRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Partida no encontrada"));
    }

    public void assertCanView(Playthrough playthrough, User currentUser) {
        boolean isOwner = playthrough.getUser().getId().equals(currentUser.getId());
        boolean isAdmin = ROLE_ADMIN.equals(currentUser.getRole());
        if (!isOwner && !isAdmin) {
            throw ApiException.forbidden("No puedes ver una partida ajena");
        }
    }

    /**
     * El administrador supervisa, no juega: puede leer cualquier partida,
     * pero decidir sobre una ajena le da 403 como a cualquier otro usuario.
     */
    public void assertCanDecide(Playthrough playthrough, User currentUser) {
        boolean isOwner = playthrough.getUser().getId().equals(currentUser.getId());
        if (!isOwner) {
            throw ApiException.forbidden("No puedes decidir sobre una partida ajena");
        }
    }

    public PlaythroughPathResponse getPath(Long id, User currentUser) {
        Playthrough playthrough = getEntity(id);
        assertCanView(playthrough, currentUser);

        List<Decision> decisions = decisionRepository
                .findByPlaythroughAndResolvedNodeCodeIsNotNullOrderByCreatedAtAsc(playthrough);

        List<PlaythroughPathResponse.PathStep> steps = new java.util.ArrayList<>();
        int order = 1;
        for (Decision decision : decisions) {
            steps.add(PlaythroughPathResponse.PathStep.builder()
                    .order(order++)
                    .decisionId(decision.getId())
                    .fromNodeCode(decision.getNode().getNodeCode())
                    .toNodeCode(decision.getResolvedNodeCode())
                    .branchType(decision.getBranchType())
                    .impactLevel(decision.getImpactLevel())
                    .createdAt(decision.getCreatedAt())
                    .build());
        }

        return PlaythroughPathResponse.builder()
                .playthroughId(playthrough.getId())
                .playerTag(playthrough.getPlayerTag())
                .status(playthrough.getStatus())
                .endingCode(playthrough.getEndingCode())
                .startNodeCode(playthrough.getStartNodeCode())
                .currentNodeCode(playthrough.getCurrentNode().getNodeCode())
                .steps(steps)
                .build();
    }

    private PlaythroughResponse toResponse(Playthrough playthrough) {
        return PlaythroughResponse.builder()
                .id(playthrough.getId())
                .playerTag(playthrough.getPlayerTag())
                .ownerEmail(playthrough.getUser().getEmail())
                .startNodeCode(playthrough.getStartNodeCode())
                .currentNodeCode(playthrough.getCurrentNode().getNodeCode())
                .lucidity(playthrough.getLucidity())
                .controlLevel(playthrough.getControlLevel())
                .status(playthrough.getStatus())
                .endingCode(playthrough.getEndingCode())
                .createdAt(playthrough.getCreatedAt())
                .updatedAt(playthrough.getUpdatedAt())
                .build();
    }
}
