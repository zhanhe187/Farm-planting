package com.farm.fpms.service;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class OpenAiCompatibleClientTest {

    @Test
    void sendsChatCompletionToConfiguredProvider() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        OpenAiCompatibleClient client = new OpenAiCompatibleClient(restTemplate);
        AiProvider provider = provider("CHAT", "gpt-4o-mini");

        server.expect(once(), requestTo("https://api.example.com/v1/chat/completions"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer sk-real"))
                .andRespond(withSuccess("{\"choices\":[{\"message\":{\"content\":\"需要保持通风并检查虫害。\"}}]}",
                        MediaType.APPLICATION_JSON));

        String answer = client.chat(provider, "system", "本周如何管理番茄？");

        assertThat(answer).isEqualTo("需要保持通风并检查虫害。");
        server.verify();
    }

    @Test
    void parsesVisionJsonContentFromCompatibleProvider() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        OpenAiCompatibleClient client = new OpenAiCompatibleClient(restTemplate);
        AiProvider provider = provider("VISION", "gpt-4o-mini");

        server.expect(once(), requestTo("https://api.example.com/v1/chat/completions"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"choices\":[{\"message\":{\"content\":\"{\\\"name\\\":\\\"番茄\\\",\\\"usage\\\":\\\"鲜食、加工番茄酱\\\",\\\"cultivation\\\":\\\"充足日照，少量多次浇水。\\\"}\"}}]}",
                        MediaType.APPLICATION_JSON));

        VisionRecognitionResult result = client.recognize(provider, "data:image/jpeg;base64,AAAA");

        assertThat(result.getName()).isEqualTo("番茄");
        assertThat(result.getUsage()).contains("鲜食");
        assertThat(result.getCultivation()).contains("日照");
        server.verify();
    }

    private AiProvider provider(String scene, String model) {
        return new AiProvider(7L, "OpenAI", "OPENAI_COMPATIBLE", "https://api.example.com/v1",
                "sk-real", "sk-****real", model, scene, 1, 30000, true, Collections.emptyMap());
    }
}
