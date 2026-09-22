package com.tuckersoft.branchengine.service;

import com.tuckersoft.branchengine.dto.CreateDecisionRequest;
import com.tuckersoft.branchengine.model.Playthrough;
import com.tuckersoft.branchengine.model.StoryNode;
import com.tuckersoft.branchengine.model.User;
import com.tuckersoft.branchengine.repository.DecisionRepository;
import com.tuckersoft.branchengine.repository.PlaythroughRepository;
import com.tuckersoft.branchengine.repository.RealityLogRepository;
import com.tuckersoft.branchengine.repository.StoryNodeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios de DecisionService: corren sin PostgreSQL ni red,
 * usando Mockito para simular los repositorios y el publisher de eventos.
 */
@ExtendWith(MockitoExtension.class)
class DecisionServiceTest {

    @Mock private PlaythroughRepository playthroughRepository;
    @Mock private StoryNodeRepository storyNodeRepository;
    @Mock private DecisionRepository decisionRepository;
    @Mock private RealityLogRepository realityLogRepository;
    @Mock private ApplicationEventPublisher eventPublisher;

    private ClassificationService classificationService;
    private PlaythroughService playthroughService;
    private DecisionService decisionService;

    private User owner;
    private StoryNode originNode;
    private StoryNode primaryNode;
    private StoryNode glitchNode;
    private Playthrough playthrough;

    @BeforeEach
    void setUp() {
        classificationService = new ClassificationService();
        playthroughService = new PlaythroughService(playthroughRepository, storyNodeRepository, decisionRepository);
        decisionService = new DecisionService(
                playthroughRepository, storyNodeRepository, decisionRepository, realityLogRepository,
                classificationService, playthroughService, eventPublisher);

        owner = User.builder().id(1L).email("qa@tuckersoft.test").displayName("Ada").role("ROLE_USER")
                .createdAt(Instant.now()).build();

        primaryNode = StoryNode.builder().id(2L).nodeCode("NODE-BUS").title("Bus").sceneText("texto texto")
                .branchCapacity(5).currentBranches(0).createdAt(Instant.now()).build();

        glitchNode = StoryNode.builder().id(3L).nodeCode("NODE-ESPEJO").title("Espejo").sceneText("texto texto")
                .branchCapacity(5).currentBranches(0).createdAt(Instant.now()).build();

        originNode = StoryNode.builder().id(1L).nodeCode("NODE-CEREAL").title("Cereal").sceneText("texto texto")
                .branchCapacity(5).currentBranches(1)
                .primaryBranchCode("NODE-BUS").glitchBranchCode("NODE-ESPEJO")
                .createdAt(Instant.now()).build();

        playthrough = Playthrough.builder().id(10L).playerTag("STEFAN-01").user(owner)
                .startNodeCode("NODE-CEREAL").currentNode(originNode)
                .lucidity(100).controlLevel(0).status("ACTIVA")
                .createdAt(Instant.now()).updatedAt(Instant.now()).build();
    }

    @Test
    void clasificaRupturaCuartaParedConPrecedenciaSobreRebeldia() {
        ClassificationResult result = classificationService.classify("Stefan destruye la camara");
        assertEquals("RUPTURA_CUARTA_PARED", result.branchType());
    }

    @Test
    void entradaCorruptaNoModificaLaPartida() {
        when(playthroughRepository.findById(10L)).thenReturn(java.util.Optional.of(playthrough));
        when(decisionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CreateDecisionRequest request = new CreateDecisionRequest();
        request.setPlaythroughId(10L);
        request.setRawInput("12345678900000");
        request.setImpactLevel("LEVE");

        decisionService.createDecision(request, owner, false);

        assertEquals(100, playthrough.getLucidity());
        assertEquals(0, playthrough.getControlLevel());
        assertEquals("ACTIVA", playthrough.getStatus());
        assertEquals(originNode, playthrough.getCurrentNode());
        verify(eventPublisher, never()).publishEvent(any());
        verify(playthroughRepository, never()).save(any());
    }

    @Test
    void impactoCriticoAplicaLosLimitesCorrectos() {
        when(playthroughRepository.findById(10L)).thenReturn(java.util.Optional.of(playthrough));
        when(storyNodeRepository.findByNodeCode("NODE-ESPEJO")).thenReturn(java.util.Optional.of(glitchNode));
        when(playthroughRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(decisionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CreateDecisionRequest request = new CreateDecisionRequest();
        request.setPlaythroughId(10L);
        request.setRawInput("Stefan acepta la oferta de Mohan");
        request.setImpactLevel("CRITICO");

        decisionService.createDecision(request, owner, false);

        assertEquals(60, playthrough.getLucidity());   // 100 - 40, dentro de limites
        assertEquals(45, playthrough.getControlLevel()); // 0 + 45, dentro de limites
    }

    @Test
    void controlLevel100TerminaLaPartidaConEndingPacSymbolAunqueLucidezTambienSeaCero() {
        playthrough.setLucidity(40);
        playthrough.setControlLevel(60);

        when(playthroughRepository.findById(10L)).thenReturn(java.util.Optional.of(playthrough));
        when(playthroughRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(decisionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CreateDecisionRequest request = new CreateDecisionRequest();
        request.setPlaythroughId(10L);
        request.setRawInput("Stefan acepta la oferta de Mohan");
        request.setImpactLevel("CRITICO"); // lucidity -> 0, controlLevel -> 105 -> 100

        decisionService.createDecision(request, owner, false);

        assertEquals("FINALIZADA", playthrough.getStatus());
        assertEquals("ENDING_PAC_SYMBOL", playthrough.getEndingCode());
        assertEquals(0, playthrough.getLucidity());
        assertEquals(100, playthrough.getControlLevel());
    }

    @Test
    void publishEventSeLlamaUnaVezEnDecisionNormalYCeroVecesEnEntradaCorrupta() {
        when(playthroughRepository.findById(10L)).thenReturn(java.util.Optional.of(playthrough));
        when(storyNodeRepository.findByNodeCode("NODE-BUS")).thenReturn(java.util.Optional.of(primaryNode));
        when(playthroughRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(decisionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CreateDecisionRequest normal = new CreateDecisionRequest();
        normal.setPlaythroughId(10L);
        normal.setRawInput("Stefan acepta la oferta de Mohan");
        normal.setImpactLevel("LEVE");

        decisionService.createDecision(normal, owner, false);
        verify(eventPublisher, times(1)).publishEvent(any());

        reset(eventPublisher);
        when(playthroughRepository.findById(10L)).thenReturn(java.util.Optional.of(playthrough));

        CreateDecisionRequest corrupta = new CreateDecisionRequest();
        corrupta.setPlaythroughId(10L);
        corrupta.setRawInput("00000000000000");
        corrupta.setImpactLevel("LEVE");

        decisionService.createDecision(corrupta, owner, false);
        verify(eventPublisher, never()).publishEvent(any());
    }
}
