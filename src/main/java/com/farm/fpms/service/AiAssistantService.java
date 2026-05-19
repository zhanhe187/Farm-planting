package com.farm.fpms.service;

import com.farm.fpms.entity.AiProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.function.Consumer;

@Service
public class AiAssistantService {

    private static final String ACTIVE_BATCH_COUNT_SQL =
            "select count(*) from plant_batch where status not in (N'已完结',N'已废弃')";

    private final JdbcTemplate jdbcTemplate;
    private final PromptBuilder promptBuilder;
    private final AiProviderService aiProviderService;
    private final AiGateway aiGateway;

    public AiAssistantService(JdbcTemplate jdbcTemplate, PromptBuilder promptBuilder,
                              AiProviderService aiProviderService, AiGateway aiGateway) {
        this.jdbcTemplate = jdbcTemplate;
        this.promptBuilder = promptBuilder;
        this.aiProviderService = aiProviderService;
        this.aiGateway = aiGateway;
    }

    public String answer(String question, Long batchId) {
        String context = "全场概况：在种批次 " + jdbcTemplate.queryForObject(
                ACTIVE_BATCH_COUNT_SQL, Integer.class) + " 个。";
        if (batchId != null) {
            Map<String, Object> batch = jdbcTemplate.queryForMap(
                    "select b.batch_no, b.status, c.name crop_name, p.name plot_name, b.sow_date " +
                            "from plant_batch b join farm_crop c on c.id = b.crop_id join farm_plot p on p.id = b.plot_id where b.id = ?",
                    batchId);
            context = context + " 当前批次：" + batch;
        }
        String prompt = promptBuilder.buildContextPrompt(context);
        AiProvider provider = aiProviderService.requireProvider("CHAT");
        long start = System.nanoTime();
        try {
            String answer = aiGateway.chat(provider, prompt, question);
            aiProviderService.logCall(provider.getId(), "CHAT", question, true,
                    java.time.Duration.ofNanos(System.nanoTime() - start), null);
            return answer;
        } catch (RuntimeException ex) {
            aiProviderService.logCall(provider.getId(), "CHAT", question, false,
                    java.time.Duration.ofNanos(System.nanoTime() - start), ex.getClass().getSimpleName());
            throw ex;
        }
    }

    public void streamAnswer(String question, Long batchId, Consumer<String> tokenConsumer) {
        String context = "全场概况：在种批次 " + jdbcTemplate.queryForObject(
                ACTIVE_BATCH_COUNT_SQL, Integer.class) + " 个。";
        if (batchId != null) {
            Map<String, Object> batch = jdbcTemplate.queryForMap(
                    "select b.batch_no, b.status, c.name crop_name, p.name plot_name, b.sow_date " +
                            "from plant_batch b join farm_crop c on c.id = b.crop_id join farm_plot p on p.id = b.plot_id where b.id = ?",
                    batchId);
            context = context + " 当前批次：" + batch;
        }
        String prompt = promptBuilder.buildContextPrompt(context);
        AiProvider provider = aiProviderService.requireProvider("CHAT");
        long start = System.nanoTime();
        try {
            aiGateway.chatStream(provider, prompt, question, tokenConsumer);
            aiProviderService.logCall(provider.getId(), "CHAT", question, true,
                    java.time.Duration.ofNanos(System.nanoTime() - start), null);
        } catch (RuntimeException ex) {
            aiProviderService.logCall(provider.getId(), "CHAT", question, false,
                    java.time.Duration.ofNanos(System.nanoTime() - start), ex.getClass().getSimpleName());
            throw ex;
        }
    }
}
