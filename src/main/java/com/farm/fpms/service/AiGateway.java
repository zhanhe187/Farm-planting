package com.farm.fpms.service;

import com.farm.fpms.entity.AiProvider;
import com.farm.fpms.entity.VisionRecognitionResult;

import java.util.function.Consumer;

public interface AiGateway {

    String chat(AiProvider provider, String systemPrompt, String userQuestion);

    void chatStream(AiProvider provider, String systemPrompt, String userQuestion, Consumer<String> tokenConsumer);

    VisionRecognitionResult recognize(AiProvider provider, String imageDataUrl);
}
