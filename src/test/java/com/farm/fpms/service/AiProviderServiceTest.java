package com.farm.fpms.service;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AiProviderServiceTest {

    @Test
    void savesProviderWithMaskedKeyAndCanDecodeLocalDevKey() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        AiKeyCodec keyCodec = new AiKeyCodec("");
        AiProviderService service = new AiProviderService(jdbcTemplate, keyCodec);

        AiProviderForm form = new AiProviderForm();
        form.setName("OpenAI");
        form.setProviderType("OPENAI_COMPATIBLE");
        form.setBaseUrl("https://api.example.com/v1");
        form.setApiKey("sk-test-1234567890");
        form.setDefaultModel("gpt-4o-mini");
        form.setScene("CHAT");
        form.setPriority(1);
        form.setTimeoutMs(30000);
        form.setEnabled(true);

        service.createProvider(form);

        verify(jdbcTemplate).update(contains("insert into ai_provider"), eq("OpenAI"),
                eq("OPENAI_COMPATIBLE"), eq("https://api.example.com/v1"), any(byte[].class),
                eq("sk-********7890"), eq("gpt-4o-mini"), eq("CHAT"), eq(1), eq(30000), eq(true));
        assertThat(keyCodec.decode(keyCodec.encode("sk-test-1234567890"))).isEqualTo("sk-test-1234567890");
    }
}
