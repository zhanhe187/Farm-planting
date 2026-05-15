package com.farm.fpms.service;

import java.util.function.Consumer;

public interface AiGateway {

    String chat(AiProvider provider, String systemPrompt, String userQuestion);

    void chatStream(AiProvider provider, String systemPrompt, String userQuestion, Consumer<String> tokenConsumer);

    VisionRecognitionResult recognize(AiProvider provider, String imageDataUrl);
}
