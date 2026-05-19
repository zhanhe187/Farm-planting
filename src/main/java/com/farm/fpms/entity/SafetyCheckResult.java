package com.farm.fpms.entity;

import java.time.LocalDate;

public class SafetyCheckResult {

    private final boolean allowed;
    private final LocalDate forbidUntil;
    private final String message;

    public SafetyCheckResult(boolean allowed, LocalDate forbidUntil, String message) {
        this.allowed = allowed;
        this.forbidUntil = forbidUntil;
        this.message = message;
    }

    public boolean isAllowed() {
        return allowed;
    }

    public LocalDate getForbidUntil() {
        return forbidUntil;
    }

    public String getMessage() {
        return message;
    }
}
