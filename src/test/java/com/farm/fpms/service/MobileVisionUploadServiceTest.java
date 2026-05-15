package com.farm.fpms.service;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Collections;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

class MobileVisionUploadServiceTest {

    @Test
    void recognizesUploadedCropImageThroughVisionProvider() {
        AiGateway gateway = new AiGateway() {
            @Override
            public String chat(AiProvider provider, String systemPrompt, String userQuestion) {
                return "";
            }

            @Override
            public void chatStream(AiProvider provider, String systemPrompt, String userQuestion, Consumer<String> tokenConsumer) {
            }

            @Override
            public VisionRecognitionResult recognize(AiProvider provider, String imageDataUrl) {
                assertThat(imageDataUrl).startsWith("data:image/png;base64,");
                return new VisionRecognitionResult("黄瓜", "Cucumis sativus", "鲜食和腌制",
                        "20-28摄氏度，搭架栽培，保持通风。", "疏松肥沃土壤", "霜霉病", 0.91);
            }
        };
        AiProviderService providerService = new AiProviderService(null, new AiKeyCodec(""));
        providerService.setInMemoryProviders(Collections.singletonList(new AiProvider(1L, "Vision",
                "OPENAI_COMPATIBLE", "https://api.example.com/v1", "sk-real", "sk-****real",
                "gpt-4o-mini", "VISION", 1, 30000, true, Collections.emptyMap())));
        MobileVisionService service = new MobileVisionService(providerService, gateway, null);

        MockMultipartFile file = new MockMultipartFile("image", "crop.png", "image/png",
                new byte[]{1, 2, 3, 4});

        VisionRecognitionResult result = service.recognize(file);

        assertThat(result.getName()).isEqualTo("黄瓜");
        assertThat(result.getUsage()).contains("鲜食");
        assertThat(result.getCultivation()).contains("通风");
    }
}
