package com.tuckersoft.branchengine.listener;

import com.tuckersoft.branchengine.event.DecisionCommittedEvent;
import com.tuckersoft.branchengine.model.Decision;
import com.tuckersoft.branchengine.model.RealityLog;
import com.tuckersoft.branchengine.repository.DecisionRepository;
import com.tuckersoft.branchengine.repository.RealityLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Instant;
import java.time.format.DateTimeFormatter;

/**
 * Escucha DecisionCommittedEvent solo DESPUES del commit de la transaccion
 * que guarda la Decision (AFTER_COMMIT), corre en un hilo del pool "branchExecutor"
 * (@Async) y necesita su propia transaccion REQUIRES_NEW para poder persistir
 * sus propios cambios (status de la Decision + RealityLog).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BranchNotificationListener {

    private final DecisionRepository decisionRepository;
    private final RealityLogRepository realityLogRepository;
    private final JavaMailSender mailSender;

    @Async("branchExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void alCommit(DecisionCommittedEvent evento) {
        Decision decision = decisionRepository.findById(evento.getDecisionId()).orElse(null);
        if (decision == null) {
            log.error("[BRANCH-LOG] No se encontro la Decision {} para notificar", evento.getDecisionId());
            return;
        }

        decision.setStatus("PROCESANDO");
        decision.setUpdatedAt(Instant.now());
        decisionRepository.save(decision);

        String subject = String.format("[TUCKERSOFT] %s en %s | Impacto %s",
                evento.getBranchType(), evento.getPlayerTag(), evento.getImpactLevel());

        String body = buildBody(evento);

        RealityLog.RealityLogBuilder logBuilder = RealityLog.builder()
                .decision(decision)
                .recipientEmail(evento.getRecipientEmail())
                .subject(subject)
                .createdAt(Instant.now());

        String finalStatus;
        try {
            if (evento.isSimulateMailFailure()) {
                throw new RuntimeException("Fallo simulado de correo (X-Bandersnatch-Simulate: MAIL_FAILURE)");
            }

            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(evento.getRecipientEmail());
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);

            RealityLog realityLog = logBuilder
                    .logStatus("SENT")
                    .sentAt(Instant.now())
                    .build();
            realityLogRepository.save(realityLog);

            finalStatus = "ESTABILIZADA";
        } catch (Exception ex) {
            RealityLog realityLog = logBuilder
                    .logStatus("FAILED")
                    .errorMessage(ex.getMessage())
                    .build();
            realityLogRepository.save(realityLog);

            log.error("[BRANCH-LOG] Fallo el envio del Informe de Realidad para Decision {}: {}",
                    evento.getDecisionId(), ex.getMessage());

            finalStatus = "ERROR";
        }

        decision.setStatus(finalStatus);
        decision.setUpdatedAt(Instant.now());
        decisionRepository.save(decision);

        log.info("[BRANCH-LOG] Decision ID: {} | Player: {} | Branch: {} | Impact: {} | Unit: {} | Node: {} -> {} | Thread: {} | Status: {}",
                evento.getDecisionId(), evento.getPlayerTag(), evento.getBranchType(), evento.getImpactLevel(),
                evento.getHandlerUnit(), evento.getSourceNodeCode(), evento.getResolvedNodeCode(),
                Thread.currentThread().getName(), finalStatus);
    }

    private String buildBody(DecisionCommittedEvent evento) {
        String ending = evento.getEndingCode() == null ? "-" : evento.getEndingCode();
        String createdAtStr = DateTimeFormatter.ISO_INSTANT.format(evento.getCreatedAt());

        return "Hola " + evento.getRecipientDisplayName() + ",\n\n" +
                "Una partida de prueba acaba de ramificarse.\n\n" +
                "\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\n" +
                "Decision ID      : #" + evento.getDecisionId() + "\n" +
                "Jugador          : " + evento.getPlayerTag() + "\n" +
                "Rama             : " + evento.getBranchType() + "\n" +
                "Impacto          : " + evento.getImpactLevel() + "\n" +
                "Departamento     : " + evento.getHandlerUnit() + "\n" +
                "Consecuencia     : " + evento.getOutcomeCode() + "\n" +
                "Nodo origen      : " + evento.getSourceNodeCode() + "\n" +
                "Nodo destino     : " + evento.getResolvedNodeCode() + "\n" +
                "Estado partida   : " + evento.getPlaythroughStatus() + "\n" +
                "Lucidez          : " + evento.getLucidity() + "/100\n" +
                "Nivel de control : " + evento.getControlLevel() + "/100\n" +
                "Final            : " + ending + "\n" +
                "Registrada       : " + createdAtStr + "\n" +
                "\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\n\n" +
                "Decisi\u00f3n original del jugador:\n" +
                "\"" + evento.getRawInput() + "\"\n\n" +
                "\u2014 Tuckersoft Branch Engine, 1984";
    }
}
