package com.farm.fpms.service;

import com.farm.fpms.domain.BusinessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class AiProviderService {

    private final JdbcTemplate jdbcTemplate;
    private final AiKeyCodec keyCodec;
    private final AtomicReference<List<AiProvider>> inMemoryProviders =
            new AtomicReference<List<AiProvider>>(Collections.<AiProvider>emptyList());

    public AiProviderService(JdbcTemplate jdbcTemplate, AiKeyCodec keyCodec) {
        this.jdbcTemplate = jdbcTemplate;
        this.keyCodec = keyCodec;
    }

    public void createProvider(AiProviderForm form) {
        validate(form, true);
        jdbcTemplate.update("insert into ai_provider(name, provider_type, base_url, encrypted_api_key, api_key_mask, " +
                        "default_model, scene, priority, timeout_ms, enabled) values(?,?,?,?,?,?,?,?,?,?)",
                trim(form.getName()), trim(form.getProviderType()), trimBaseUrl(form.getBaseUrl()),
                keyCodec.encode(form.getApiKey()), AiKeyCodec.mask(form.getApiKey()), trim(form.getDefaultModel()),
                trim(form.getScene()).toUpperCase(), form.getPriority(), normalizedTimeout(form.getTimeoutMs()),
                form.isEnabled());
    }

    public void updateProvider(long id, AiProviderForm form) {
        validate(form, false);
        if (form.getApiKey() == null || form.getApiKey().trim().isEmpty()) {
            jdbcTemplate.update("update ai_provider set name=?, provider_type=?, base_url=?, default_model=?, scene=?, " +
                            "priority=?, timeout_ms=?, enabled=? where id=?",
                    trim(form.getName()), trim(form.getProviderType()), trimBaseUrl(form.getBaseUrl()),
                    trim(form.getDefaultModel()), trim(form.getScene()).toUpperCase(), form.getPriority(),
                    normalizedTimeout(form.getTimeoutMs()), form.isEnabled(), id);
            return;
        }
        jdbcTemplate.update("update ai_provider set name=?, provider_type=?, base_url=?, encrypted_api_key=?, " +
                        "api_key_mask=?, default_model=?, scene=?, priority=?, timeout_ms=?, enabled=? where id=?",
                trim(form.getName()), trim(form.getProviderType()), trimBaseUrl(form.getBaseUrl()),
                keyCodec.encode(form.getApiKey()), AiKeyCodec.mask(form.getApiKey()), trim(form.getDefaultModel()),
                trim(form.getScene()).toUpperCase(), form.getPriority(), normalizedTimeout(form.getTimeoutMs()),
                form.isEnabled(), id);
    }

    public List<Map<String, Object>> listProviderRows() {
        return jdbcTemplate.queryForList("select * from ai_provider order by scene, priority, id");
    }

    public List<Map<String, Object>> listLogRows() {
        return jdbcTemplate.queryForList("select top 80 * from ai_call_log order by id desc");
    }

    public AiProvider requireProvider(String scene) {
        List<AiProvider> memory = inMemoryProviders.get();
        if (!memory.isEmpty()) {
            for (AiProvider provider : memory) {
                if (provider.isEnabled() && provider.getScene().equalsIgnoreCase(scene)) {
                    return provider;
                }
            }
        }
        List<AiProvider> providers = jdbcTemplate.query(
                "select top 1 * from ai_provider where scene = ? and enabled = 1 order by priority, id",
                this::mapProvider, scene.toUpperCase());
        if (providers.isEmpty()) {
            throw new BusinessException("未配置可用的 AI " + scene + " 端点，请先在 AI 端点页面配置 Base URL 和 API Key");
        }
        return providers.get(0);
    }

    public AiProvider requireProviderById(long id) {
        List<AiProvider> providers = jdbcTemplate.query("select top 1 * from ai_provider where id = ?",
                this::mapProvider, id);
        if (providers.isEmpty()) {
            throw new BusinessException("AI 端点不存在");
        }
        return providers.get(0);
    }

    public void logCall(Long providerId, String scene, String promptSummary, boolean success,
                        Duration elapsed, String errorCode) {
        if (jdbcTemplate == null) {
            return;
        }
        jdbcTemplate.update("insert into ai_call_log(provider_id, scene, prompt_summary, success, elapsed_ms, error_code) " +
                        "values(?,?,?,?,?,?)",
                providerId, scene, abbreviate(promptSummary, 500), success, (int) elapsed.toMillis(), errorCode);
    }

    public void setInMemoryProviders(List<AiProvider> providers) {
        this.inMemoryProviders.set(new ArrayList<AiProvider>(providers));
    }

    private AiProvider mapProvider(ResultSet rs, int rowNum) throws SQLException {
        String apiKey = keyCodec.decode(rs.getBytes("encrypted_api_key"));
        return new AiProvider(rs.getLong("id"), rs.getString("name"), rs.getString("provider_type"),
                rs.getString("base_url"), apiKey, rs.getString("api_key_mask"), rs.getString("default_model"),
                rs.getString("scene"), rs.getInt("priority"), rs.getInt("timeout_ms"),
                rs.getBoolean("enabled"), Collections.<String, Object>emptyMap());
    }

    private void validate(AiProviderForm form, boolean requireKey) {
        if (isBlank(form.getName()) || isBlank(form.getProviderType()) || isBlank(form.getBaseUrl())
                || isBlank(form.getDefaultModel()) || isBlank(form.getScene())) {
            throw new BusinessException("AI 端点名称、类型、地址、模型和场景不能为空");
        }
        if (requireKey && isBlank(form.getApiKey())) {
            throw new BusinessException("新建 AI 端点必须填写 API Key");
        }
        String scene = form.getScene().trim().toUpperCase();
        if (!"CHAT".equals(scene) && !"VISION".equals(scene)) {
            throw new BusinessException("AI 场景只支持 CHAT 或 VISION");
        }
    }

    private int normalizedTimeout(int timeoutMs) {
        return timeoutMs <= 0 ? 60000 : timeoutMs;
    }

    private String trimBaseUrl(String value) {
        String trimmed = trim(value);
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String abbreviate(String value, int max) {
        String text = value == null ? "" : value;
        return text.length() <= max ? text : text.substring(0, max);
    }
}
