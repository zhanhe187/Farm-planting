package com.farm.fpms.service;

import com.farm.fpms.entity.AiProvider;
import com.farm.fpms.entity.VisionRecognitionResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Service
public class OpenAiCompatibleClient implements AiGateway {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OpenAiCompatibleClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public String chat(AiProvider provider, String systemPrompt, String userQuestion) {
        if (usesResponsesApi(provider)) {
            JsonNode response = postResponsesText(provider, systemPrompt, userQuestion, 0.2);
            return extractContent(response);
        }
        List<Map<String, Object>> messages = new ArrayList<Map<String, Object>>();
        messages.add(message("system", systemPrompt));
        messages.add(message("user", userQuestion));
        JsonNode response = postChatCompletions(provider, messages, 0.2);
        return extractContent(response);
    }

    @Override
    public void chatStream(AiProvider provider, String systemPrompt, String userQuestion, Consumer<String> tokenConsumer) {
        if (usesResponsesApi(provider)) {
            tokenConsumer.accept(chat(provider, systemPrompt, userQuestion));
            return;
        }
        List<Map<String, Object>> messages = new ArrayList<Map<String, Object>>();
        messages.add(message("system", systemPrompt));
        messages.add(message("user", userQuestion));

        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("model", provider.getDefaultModel());
        payload.put("messages", messages);
        payload.put("temperature", 0.2);
        payload.put("stream", true);

        String url = chatCompletionsUrl(provider);
        WebClient client = WebClient.builder().build();
        try {
            String body = objectMapper.writeValueAsString(payload);
            client.post().uri(url)
                    .header("Authorization", "Bearer " + provider.getApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToFlux(String.class)
                    .doOnNext(line -> {
                        String token = parseSseLine(line);
                        if (token != null) tokenConsumer.accept(token);
                    })
                    .blockLast(java.time.Duration.ofSeconds(120));
        } catch (Exception ex) {
            throw new RuntimeException("AI 流式调用失败: " + ex.getMessage(), ex);
        }
    }

    private String parseSseLine(String line) {
        if (line == null || line.isEmpty() || "[DONE]".equals(line.trim())) return null;
        String data = line.startsWith("data: ") ? line.substring(6).trim() : line.trim();
        if (data.isEmpty() || "[DONE]".equals(data)) return null;
        try {
            JsonNode node = objectMapper.readTree(data);
            JsonNode delta = node.path("choices").path(0).path("delta").path("content");
            if (!delta.isMissingNode() && !delta.isNull()) return delta.asText();
        } catch (Exception ignored) {}
        return null;
    }

    @Override
    public VisionRecognitionResult recognize(AiProvider provider, String imageDataUrl) {
        String systemPrompt = "你是农业作物图片识别助手。请只返回 JSON，字段包括 name, latin_name, usage, cultivation, soil, common_pest, confidence。";
        String userPrompt = "识别图片中的农作物，给出作物名字、用途和培育方法。";
        if (usesResponsesApi(provider)) {
            String contentText = extractContent(postResponsesVision(provider, systemPrompt, userPrompt, imageDataUrl, 0.1));
            return parseVisionResult(contentText);
        }
        List<Map<String, Object>> messages = new ArrayList<Map<String, Object>>();
        messages.add(message("system", systemPrompt));

        List<Map<String, Object>> content = new ArrayList<Map<String, Object>>();
        Map<String, Object> text = new LinkedHashMap<String, Object>();
        text.put("type", "text");
        text.put("text", userPrompt);
        content.add(text);
        Map<String, Object> image = new LinkedHashMap<String, Object>();
        image.put("type", "image_url");
        Map<String, Object> imageUrl = new LinkedHashMap<String, Object>();
        imageUrl.put("url", imageDataUrl);
        image.put("image_url", imageUrl);
        content.add(image);

        Map<String, Object> user = new LinkedHashMap<String, Object>();
        user.put("role", "user");
        user.put("content", content);
        messages.add(user);

        String contentText = extractContent(postChatCompletions(provider, messages, 0.1));
        return parseVisionResult(contentText);
    }

    private JsonNode postChatCompletions(AiProvider provider, List<Map<String, Object>> messages, double temperature) {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("model", provider.getDefaultModel());
        payload.put("messages", messages);
        payload.put("temperature", temperature);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(provider.getApiKey());
        String url = chatCompletionsUrl(provider);
        JsonNode response = restTemplate.postForObject(url, new HttpEntity<Map<String, Object>>(payload, headers), JsonNode.class);
        if (response == null) {
            throw new IllegalStateException("AI 服务返回为空");
        }
        return response;
    }

    private JsonNode postResponsesText(AiProvider provider, String systemPrompt, String userQuestion, double temperature) {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("model", provider.getDefaultModel());
        payload.put("instructions", systemPrompt);
        payload.put("input", userQuestion);
        payload.put("temperature", temperature);
        return postResponses(provider, payload);
    }

    private JsonNode postResponsesVision(AiProvider provider, String systemPrompt, String userPrompt,
                                         String imageDataUrl, double temperature) {
        List<Map<String, Object>> content = new ArrayList<Map<String, Object>>();
        Map<String, Object> text = new LinkedHashMap<String, Object>();
        text.put("type", "input_text");
        text.put("text", userPrompt);
        content.add(text);

        Map<String, Object> image = new LinkedHashMap<String, Object>();
        image.put("type", "input_image");
        image.put("image_url", imageDataUrl);
        content.add(image);

        Map<String, Object> user = new LinkedHashMap<String, Object>();
        user.put("role", "user");
        user.put("content", content);

        List<Map<String, Object>> input = new ArrayList<Map<String, Object>>();
        input.add(user);

        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("model", provider.getDefaultModel());
        payload.put("instructions", systemPrompt);
        payload.put("input", input);
        payload.put("temperature", temperature);
        return postResponses(provider, payload);
    }

    private JsonNode postResponses(AiProvider provider, Map<String, Object> payload) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(provider.getApiKey());
        JsonNode response = restTemplate.postForObject(responsesUrl(provider),
                new HttpEntity<Map<String, Object>>(payload, headers), JsonNode.class);
        if (response == null) {
            throw new IllegalStateException("AI 服务返回为空");
        }
        return response;
    }

    private String chatCompletionsUrl(AiProvider provider) {
        String baseUrl = normalizedBaseUrl(provider);
        if (baseUrl.endsWith("/chat/completions")) {
            return baseUrl;
        }
        return baseUrl + "/chat/completions";
    }

    private String responsesUrl(AiProvider provider) {
        return normalizedBaseUrl(provider);
    }

    private boolean usesResponsesApi(AiProvider provider) {
        return normalizedBaseUrl(provider).endsWith("/responses");
    }

    private String normalizedBaseUrl(AiProvider provider) {
        String baseUrl = provider.getBaseUrl() == null ? "" : provider.getBaseUrl().trim();
        while (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        return baseUrl;
    }

    private Map<String, Object> message(String role, String content) {
        Map<String, Object> message = new LinkedHashMap<String, Object>();
        message.put("role", role);
        message.put("content", content);
        return message;
    }

    private String extractContent(JsonNode response) {
        JsonNode content = response.path("choices").path(0).path("message").path("content");
        if (!content.isMissingNode() && !content.isNull()) {
            return content.asText();
        }
        JsonNode outputText = response.path("output_text");
        if (!outputText.isMissingNode() && !outputText.isNull()) {
            return outputText.asText();
        }
        StringBuilder text = new StringBuilder();
        JsonNode output = response.path("output");
        if (output.isArray()) {
            for (JsonNode item : output) {
                JsonNode itemContent = item.path("content");
                if (itemContent.isArray()) {
                    for (JsonNode contentItem : itemContent) {
                        JsonNode partText = contentItem.path("text");
                        if (!partText.isMissingNode() && !partText.isNull()) {
                            text.append(partText.asText());
                        }
                    }
                }
            }
        }
        if (text.length() > 0) {
            return text.toString();
        }
        throw new IllegalStateException("AI 服务响应缺少 choices[0].message.content 或 output_text");
    }

    private VisionRecognitionResult parseVisionResult(String contentText) {
        String json = unwrapJson(contentText);
        try {
            JsonNode node = objectMapper.readTree(json);
            VisionRecognitionResult result = new VisionRecognitionResult();
            result.setName(text(node, "name"));
            result.setLatinName(text(node, "latin_name"));
            result.setUsage(text(node, "usage"));
            result.setCultivation(text(node, "cultivation"));
            result.setSoil(text(node, "soil"));
            result.setCommonPest(text(node, "common_pest"));
            result.setConfidence(node.path("confidence").asDouble(0));
            result.setRawText(contentText);
            return result;
        } catch (Exception ex) {
            return VisionRecognitionResult.fromRawText(contentText);
        }
    }

    private String unwrapJson(String text) {
        String value = text == null ? "" : text.trim();
        if (value.startsWith("```")) {
            int firstLineEnd = value.indexOf('\n');
            int lastFence = value.lastIndexOf("```");
            if (firstLineEnd >= 0 && lastFence > firstLineEnd) {
                return value.substring(firstLineEnd + 1, lastFence).trim();
            }
        }
        return value;
    }

    private String text(JsonNode node, String field) {
        return node.path(field).asText("");
    }
}
