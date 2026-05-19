package com.farm.fpms.service;

import com.farm.fpms.entity.AiProvider;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiAssistantServiceTest {

    @Test
    void answerCountsActiveBatchesUsingChineseStoredStatuses() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        AiProviderService aiProviderService = mock(AiProviderService.class);
        AiGateway aiGateway = mock(AiGateway.class);
        AiProvider provider = new AiProvider(1L, "测试端点", "OPENAI_COMPATIBLE",
                "https://api.example.com/v1", "sk-test", "sk-****test", "gpt-test",
                "CHAT", 1, 30000, true, null);

        when(jdbcTemplate.queryForObject(
                "select count(*) from plant_batch where status not in (N'已完结',N'已废弃')",
                Integer.class)).thenReturn(2);
        when(aiProviderService.requireProvider("CHAT")).thenReturn(provider);
        when(aiGateway.chat(eq(provider), any(String.class), eq("给我建议"))).thenReturn("建议");

        AiAssistantService service = new AiAssistantService(jdbcTemplate, new PromptBuilder(),
                aiProviderService, aiGateway);

        String answer = service.answer("给我建议", null);

        ArgumentCaptor<String> prompt = ArgumentCaptor.forClass(String.class);
        verify(aiGateway).chat(eq(provider), prompt.capture(), eq("给我建议"));
        assertThat(answer).isEqualTo("建议");
        assertThat(prompt.getValue()).contains("在种批次 2 个");
    }
}
