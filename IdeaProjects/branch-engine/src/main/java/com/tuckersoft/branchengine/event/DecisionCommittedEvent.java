package com.tuckersoft.branchengine.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

/**
 * Evento publicado despues de guardar una Decision (no ENTRADA_CORRUPTA).
 * El listener corre en otro hilo tras el commit, por lo que ya no hay
 * usuario autenticado en el contexto de seguridad: aqui va todo lo que
 * el listener necesita para armar y enviar el correo.
 */
@Getter
@Builder
@AllArgsConstructor
public class DecisionCommittedEvent {
    private Long decisionId;
    private Long playthroughId;
    private String playerTag;
    private String recipientEmail;
    private String recipientDisplayName;
    private String branchType;
    private String impactLevel;
    private String handlerUnit;
    private String outcomeCode;
    private String sourceNodeCode;
    private String resolvedNodeCode;
    private String playthroughStatus;
    private Integer lucidity;
    private Integer controlLevel;
    private String endingCode;
    private String rawInput;
    private Instant createdAt;
    private boolean simulateMailFailure;
}
