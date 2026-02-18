package com.gestion.entities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Classe pour gérer les résultats de validation
 */
public class ValidationResult {
    private final Map<String, List<String>> errors = new HashMap<>();
    private final List<String> globalErrors = new ArrayList<>();

    public void addError(String field, String message) {
        errors.computeIfAbsent(field, k -> new ArrayList<>()).add(message);
    }

    public void addGlobalError(String message) {
        globalErrors.add(message);
    }

    public boolean hasErrors() {
        return !errors.isEmpty() || !globalErrors.isEmpty();
    }

    public Map<String, List<String>> getErrors() {
        return errors;
    }

    public List<String> getGlobalErrors() {
        return globalErrors;
    }

    public List<String> getFieldErrors(String field) {
        return errors.getOrDefault(field, new ArrayList<>());
    }

    public String getFirstError(String field) {
        List<String> fieldErrors = errors.get(field);
        return fieldErrors != null && !fieldErrors.isEmpty() ? fieldErrors.get(0) : null;
    }

    public String getAllErrorsAsString() {
        StringBuilder sb = new StringBuilder();
        for (List<String> fieldErrors : errors.values()) {
            for (String error : fieldErrors) {
                if (sb.length() > 0) sb.append("\n");
                sb.append("• ").append(error);
            }
        }
        for (String error : globalErrors) {
            if (sb.length() > 0) sb.append("\n");
            sb.append("• ").append(error);
        }
        return sb.toString();
    }

    public void clear() {
        errors.clear();
        globalErrors.clear();
    }
}
