package com.tuckersoft.branchengine.service;

import com.tuckersoft.branchengine.dto.*;
import com.tuckersoft.branchengine.event.DecisionCommittedEvent;
import com.tuckersoft.branchengine.exception.ApiException;
import com.tuckersoft.branchengine.model.*;
import com.tuckersoft.branchengine.repository.DecisionRepository;
import com.tuckersoft.branchengine.repository.PlaythroughRepository;
import com.tuckersoft.branchengine.repository.RealityLogRepository;
import com.tuckersoft.branchengine.repository.StoryNodeRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class DecisionService {

    private static final Set<String> VALID_IMPACT_LEVELS = Set.of("LEVE", "MODERADO", "GRAVE", "CRITICO");

    private static final Map<String, int[]> STAT_DELTAS = Map.of(
            "LEVE", new int[]{-5, 5},
            "MODERADO", new int[]{-15, 10},
            "GRAVE", new int[]{-30, 20},
            "CRITICO", new int[]{-40, 45}
    );

    private static final String ROLE_ADMIN = "ROLE_ADMIN";

    private final PlaythroughRepository playthroughRepository;
    private final StoryNodeRepository storyNodeRepository;
    private final DecisionRepository decisionRepository;
    private final RealityLogRepository realityLogRepository;
    private final ClassificationService classificationService;
    private final PlaythroughService playthroughService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public DecisionResponse createDecision(CreateDecisionRequest request, User currentUser, boolean simulateMailFailure) {
        Playthrough playthrough = playthroughRepository.findById(request.getPlaythroughId())
                .orElseThrow(() -> ApiException.notFound("Playthrough no encontrado"));

        playthroughService.assertCanDecide(playthrough, currentUser);

        if (!"ACTIVA".equals(playthrough.getStatus())) {
            throw ApiException.conflict("La partida ya esta FINALIZADA");
        }

        if (!VALID_IMPACT_LEVELS.contains(request.getImpactLevel())) {
            throw ApiException.badRequest("impactLevel invalido");
        }

        ClassificationResult classification = classificationService.classify(request.getRawInput());
        StoryNode originNode = playthrough.getCurrentNode();
        Instant now = Instant.now();

        Decision decision = Decision.builder()
                .playthrough(playthrough)
                .node(originNode)
                .rawInput(request.getRawInput())
                .branchType(classification.branchType())
                .impactLevel(request.getImpactLevel())
                .handlerUnit(classification.handlerUnit())
                .outcomeCode(classification.outcomeCode())
                .createdAt(now)
                .updatedAt(now)
                .build();

        if ("ENTRADA_CORRUPTA".equals(classification.branchType())) {
            decision.setResolvedNodeCode(null);
            decision.setStatus("ERROR");
            decisionRepository.save(decision);
            // No se toca la partida ni se publica el evento.
            return toResponse(decision, playthrough);
        }

        applyStatsAndResolveState(playthrough, originNode, classification, request.getImpactLevel(), now);

        playthroughRepository.save(playthrough);

        decision.setResolvedNodeCode(resolveTargetCode(originNode, classification.branchType(), request.getImpactLevel()));
        decision.setStatus("REGISTRADA");
        decisionRepository.save(decision);

        eventPublisher.publishEvent(DecisionCommittedEvent.builder()
                .decisionId(decision.getId())
                .playthroughId(playthrough.getId())
                .playerTag(playthrough.getPlayerTag())
                .recipientEmail(playthrough.getUser().getEmail())
                .recipientDisplayName(playthrough.getUser().getDisplayName())
                .branchType(decision.getBranchType())
                .impactLevel(decision.getImpactLevel())
                .handlerUnit(decision.getHandlerUnit())
                .outcomeCode(decision.getOutcomeCode())
                .sourceNodeCode(originNode.getNodeCode())
                .resolvedNodeCode(decision.getResolvedNodeCode())
                .playthroughStatus(playthrough.getStatus())
                .lucidity(playthrough.getLucidity())
                .controlLevel(playthrough.getControlLevel())
                .endingCode(playthrough.getEndingCode())
                .rawInput(decision.getRawInput())
                .createdAt(decision.getCreatedAt())
                .simulateMailFailure(simulateMailFailure)
                .build());

        return toResponse(decision, playthrough);
    }

    private String resolveTargetCode(StoryNode originNode, String branchType, String impactLevel) {
        boolean useGlitch = "RUPTURA_CUARTA_PARED".equals(branchType) || "CRITICO".equals(impactLevel);
        return useGlitch ? originNode.getGlitchBranchCode() : originNode.getPrimaryBranchCode();
    }

    private void applyStatsAndResolveState(Playthrough playthrough, StoryNode originNode,
                                            ClassificationResult classification, String impactLevel, Instant now) {
        int[] deltas = STAT_DELTAS.get(impactLevel);
        int newLucidity = clamp(playthrough.getLucidity() + deltas[0]);
        int newControl = clamp(playthrough.getControlLevel() + deltas[1]);

        playthrough.setLucidity(newLucidity);
        playthrough.setControlLevel(newControl);

        String targetCode = resolveTargetCode(originNode, classification.branchType(), impactLevel);

        if (newControl >= 100) {
            playthrough.setStatus("FINALIZADA");
            playthrough.setEndingCode("ENDING_PAC_SYMBOL");
        } else if (newLucidity <= 0) {
            playthrough.setStatus("FINALIZADA");
            playthrough.setEndingCode("ENDING_WHITE_BEAR");
        } else {
            StoryNode targetNode = (targetCode == null) ? null
                    : storyNodeRepository.findByNodeCode(targetCode).orElse(null);

            if (targetNode == null) {
                playthrough.setStatus("FINALIZADA");
                playthrough.setEndingCode("ENDING_NETFLIX_CUT");
            } else {
                playthrough.setStatus("ACTIVA");
                playthrough.setCurrentNode(targetNode);
            }
        }

        playthrough.setUpdatedAt(now);
    }

    private int clamp(int value) {
        return Math.max(0, Math.min(100, value));
    }

    public DecisionResponse getById(Long id, User currentUser) {
        Decision decision = decisionRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Decision no encontrada"));
        playthroughService.assertCanView(decision.getPlaythrough(), currentUser);
        return toResponse(decision, decision.getPlaythrough());
    }

    public List<RealityLogResponse> getRealityLogs(Long decisionId, User currentUser) {
        Decision decision = decisionRepository.findById(decisionId)
                .orElseThrow(() -> ApiException.notFound("Decision no encontrada"));
        playthroughService.assertCanView(decision.getPlaythrough(), currentUser);

        return realityLogRepository.findByDecisionOrderByCreatedAtAsc(decision).stream()
                .map(this::toLogResponse)
                .toList();
    }

    public PagedDecisionResponse list(String branchType, String impactLevel, String status, Long playthroughId,
                                       int page, int size, User currentUser) {
        Pageable pageable = PageRequest.of(page, size);

        Specification<Decision> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (!ROLE_ADMIN.equals(currentUser.getRole())) {
                predicates.add(cb.equal(root.get("playthrough").get("user").get("id"), currentUser.getId()));
            }
            if (branchType != null) {
                predicates.add(cb.equal(root.get("branchType"), branchType));
            }
            if (impactLevel != null) {
                predicates.add(cb.equal(root.get("impactLevel"), impactLevel));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (playthroughId != null) {
                predicates.add(cb.equal(root.get("playthrough").get("id"), playthroughId));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        org.springframework.data.domain.Page<Decision> result = decisionRepository.findAll(spec, pageable);

        List<DecisionResponse> content = result.getContent().stream()
                .map(d -> toResponse(d, d.getPlaythrough()))
                .toList();

        return PagedDecisionResponse.builder()
                .content(content)
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .currentPage(page)
                .size(size)
                .build();
    }

    private DecisionResponse toResponse(Decision decision, Playthrough playthrough) {
        return DecisionResponse.builder()
                .id(decision.getId())
                .playthroughId(playthrough.getId())
                .playerTag(playthrough.getPlayerTag())
                .sourceNodeCode(decision.getNode().getNodeCode())
                .resolvedNodeCode(decision.getResolvedNodeCode())
                .rawInput(decision.getRawInput())
                .branchType(decision.getBranchType())
                .impactLevel(decision.getImpactLevel())
                .handlerUnit(decision.getHandlerUnit())
                .outcomeCode(decision.getOutcomeCode())
                .status(decision.getStatus())
                .playthroughStatus(playthrough.getStatus())
                .lucidity(playthrough.getLucidity())
                .controlLevel(playthrough.getControlLevel())
                .endingCode(playthrough.getEndingCode())
                .createdAt(decision.getCreatedAt())
                .updatedAt(decision.getUpdatedAt())
                .build();
    }

    private RealityLogResponse toLogResponse(RealityLog log) {
        return RealityLogResponse.builder()
                .id(log.getId())
                .decisionId(log.getDecision().getId())
                .recipientEmail(log.getRecipientEmail())
                .subject(log.getSubject())
                .logStatus(log.getLogStatus())
                .errorMessage(log.getErrorMessage())
                .sentAt(log.getSentAt())
                .createdAt(log.getCreatedAt())
                .build();
    }
}
