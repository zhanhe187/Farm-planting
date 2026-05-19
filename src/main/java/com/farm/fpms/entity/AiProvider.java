package com.farm.fpms.entity;

import java.util.Map;

public class AiProvider {

    private final Long id;
    private final String name;
    private final String providerType;
    private final String baseUrl;
    private final String apiKey;
    private final String apiKeyMask;
    private final String defaultModel;
    private final String scene;
    private final int priority;
    private final int timeoutMs;
    private final boolean enabled;
    private final Map<String, Object> raw;

    public AiProvider(Long id, String name, String providerType, String baseUrl, String apiKey, String apiKeyMask,
                      String defaultModel, String scene, int priority, int timeoutMs, boolean enabled,
                      Map<String, Object> raw) {
        this.id = id;
        this.name = name;
        this.providerType = providerType;
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.apiKeyMask = apiKeyMask;
        this.defaultModel = defaultModel;
        this.scene = scene;
        this.priority = priority;
        this.timeoutMs = timeoutMs;
        this.enabled = enabled;
        this.raw = raw;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getProviderType() {
        return providerType;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getApiKeyMask() {
        return apiKeyMask;
    }

    public String getDefaultModel() {
        return defaultModel;
    }

    public String getScene() {
        return scene;
    }

    public int getPriority() {
        return priority;
    }

    public int getTimeoutMs() {
        return timeoutMs;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Map<String, Object> getRaw() {
        return raw;
    }
}
