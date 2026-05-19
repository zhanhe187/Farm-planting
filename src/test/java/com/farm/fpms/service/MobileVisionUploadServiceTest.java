package com.farm.fpms.service;

import com.farm.fpms.common.BusinessException;
import com.farm.fpms.entity.AiProvider;
import com.farm.fpms.entity.VisionRecognitionResult;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Collections;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

    @Test
    void rejectsMockVisionProviderForRealMobileRecognition() {
        AiProviderService providerService = new AiProviderService(null, new AiKeyCodec(""));
        providerService.setInMemoryProviders(Collections.singletonList(new AiProvider(2L, "本地-Vision",
                "LOCAL_FALLBACK", "http://localhost:8080/mock-vision", "local-key", "local-****key",
                "fpms-local-vision", "VISION", 1, 30000, true, Collections.emptyMap())));
        AiGateway gateway = new NoopGateway();
        MobileVisionService service = new MobileVisionService(providerService, gateway, null);
        MockMultipartFile file = new MockMultipartFile("image", "crop.png", "image/png",
                new byte[]{1, 2, 3, 4});

        assertThatThrownBy(() -> service.recognize(file))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("请先配置真实图片识别 AI 端点");
    }

    @Test
    void rejectsDeepSeekTextModelForVisionRecognition() {
        AiProviderService providerService = new AiProviderService(null, new AiKeyCodec(""));
        providerService.setInMemoryProviders(Collections.singletonList(new AiProvider(3L, "DeepSeek Pro",
                "OPENAI_COMPATIBLE", "https://api.deepseek.com", "sk-real", "sk-****real",
                "deepseek-v4-pro", "VISION", 1, 30000, true, Collections.emptyMap())));
        MobileVisionService service = new MobileVisionService(providerService, new NoopGateway(), null);
        MockMultipartFile file = new MockMultipartFile("image", "crop.png", "image/png",
                new byte[]{1, 2, 3, 4});

        assertThatThrownBy(() -> service.recognize(file))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("DeepSeek 不支持图片输入");
    }

    private static class NoopGateway implements AiGateway {
        @Override
        public String chat(AiProvider provider, String systemPrompt, String userQuestion) {
            return "";
        }

        @Override
        public void chatStream(AiProvider provider, String systemPrompt, String userQuestion, Consumer<String> tokenConsumer) {
        }

        @Override
        public VisionRecognitionResult recognize(AiProvider provider, String imageDataUrl) {
            return VisionRecognitionResult.fromRawText("");
        }
    }
}
