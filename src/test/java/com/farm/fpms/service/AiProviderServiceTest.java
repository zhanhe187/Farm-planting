package com.farm.fpms.service;

import com.farm.fpms.entity.AiProvider;
import com.farm.fpms.entity.AiProviderForm;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

    @Test
    void updatesProviderWithoutReplacingExistingKeyWhenKeyIsBlank() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        AiProviderService service = new AiProviderService(jdbcTemplate, new AiKeyCodec(""));
        AiProviderForm form = new AiProviderForm();
        form.setName("DeepSeek Chat");
        form.setProviderType("OPENAI_COMPATIBLE");
        form.setBaseUrl("https://api.deepseek.com/");
        form.setApiKey(" ");
        form.setDefaultModel("deepseek-chat");
        form.setScene("CHAT");
        form.setPriority(2);
        form.setTimeoutMs(45000);
        form.setEnabled(true);

        service.updateProvider(8L, form);

        verify(jdbcTemplate).update(contains("update ai_provider set name=?, provider_type=?, base_url=?, default_model=?"),
                eq("DeepSeek Chat"), eq("OPENAI_COMPATIBLE"), eq("https://api.deepseek.com"),
                eq("deepseek-chat"), eq("CHAT"), eq(2), eq(45000), eq(true), eq(8L));
    }

    @Test
    void deletesProviderById() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        AiProviderService service = new AiProviderService(jdbcTemplate, new AiKeyCodec(""));

        service.deleteProvider(8L);

        verify(jdbcTemplate).update("delete from ai_provider where id = ?", 8L);
    }

    @Test
    void rejectsDeepSeekProviderForVisionScene() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        AiProviderService service = new AiProviderService(jdbcTemplate, new AiKeyCodec(""));
        AiProviderForm form = new AiProviderForm();
        form.setName("DeepSeek Pro Vision");
        form.setProviderType("OPENAI_COMPATIBLE");
        form.setBaseUrl("https://api.deepseek.com");
        form.setApiKey("sk-test-1234567890");
        form.setDefaultModel("deepseek-v4-pro");
        form.setScene("VISION");
        form.setPriority(1);
        form.setTimeoutMs(30000);
        form.setEnabled(true);

        assertThatThrownBy(() -> service.createProvider(form))
                .isInstanceOf(com.farm.fpms.common.BusinessException.class)
                .hasMessageContaining("DeepSeek 端点只能用于 CHAT");
    }
}
