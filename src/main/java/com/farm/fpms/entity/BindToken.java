package com.farm.fpms.entity;

import java.time.Instant;

public class BindToken {

    private final String token;
    private final String pcSessionId;
    private final Instant expiresAt;
    private boolean used;
    private String usedIp;
    private String usedUa;

    public BindToken(String token, String pcSessionId, Instant expiresAt) {
        this.token = token;
        this.pcSessionId = pcSessionId;
        this.expiresAt = expiresAt;
    }

    public String getToken() {
        return token;
    }

    public String getPcSessionId() {
        return pcSessionId;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public boolean isUsed() {
        return used;
    }

    public void markUsed() {
        this.used = true;
    }

    public void markUsed(String usedIp, String usedUa) {
        this.used = true;
        this.usedIp = usedIp;
        this.usedUa = usedUa;
    }

    public String getUsedIp() {
        return usedIp;
    }

    public String getUsedUa() {
        return usedUa;
    }
}
