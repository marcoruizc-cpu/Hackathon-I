package com.tuckersoft.branchengine.service;

import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.regex.Pattern;

/**
 * Clasifica el rawInput del jugador en una de las cinco ramas del guion,
 * aplicando las reglas EN ORDEN. La primera regla que se cumple gana.
 */
@Service
public class ClassificationService {

    private static final Pattern HAS_LETTER = Pattern.compile("[a-z]");

    private static final String[] RUPTURA_KEYWORDS = {"netflix", "camara", "espectador", "videojuego"};
    private static final String[] SOSPECHA_KEYWORDS = {"vigilan", "simbolo", "conspiracion"};
    private static final String[] REBELDIA_KEYWORDS = {"rechaza", "destruye", "desobedece", "renuncia"};

    public ClassificationResult classify(String rawInput) {
        String normalized = normalize(rawInput);
        String branchType = determineBranchType(normalized);
        return switch (branchType) {
            case "OBEDIENCIA" -> new ClassificationResult("OBEDIENCIA", "Mesa de Guion", "ADVANCE_MAIN_PATH");
            case "REBELDIA" -> new ClassificationResult("REBELDIA", "Control de Continuidad", "FORK_TIMELINE");
            case "SOSPECHA" -> new ClassificationResult("SOSPECHA", "Oficina de Seguridad", "INJECT_WHITE_BEAR_SYMBOL");
            case "RUPTURA_CUARTA_PARED" -> new ClassificationResult("RUPTURA_CUARTA_PARED", "Departamento Netflix", "BREAK_FOURTH_WALL");
            default -> new ClassificationResult("ENTRADA_CORRUPTA", "Archivo de Errores", "DISCARD_INPUT");
        };
    }

    private String determineBranchType(String normalized) {
        if (!HAS_LETTER.matcher(normalized).find()) {
            return "ENTRADA_CORRUPTA";
        }
        if (containsAny(normalized, RUPTURA_KEYWORDS)) {
            return "RUPTURA_CUARTA_PARED";
        }
        if (containsAny(normalized, SOSPECHA_KEYWORDS)) {
            return "SOSPECHA";
        }
        if (containsAny(normalized, REBELDIA_KEYWORDS)) {
            return "REBELDIA";
        }
        return "OBEDIENCIA";
    }

    private boolean containsAny(String text, String[] keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    public String normalize(String rawInput) {
        String normalized = Normalizer.normalize(rawInput, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return normalized.toLowerCase();
    }
}
