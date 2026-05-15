package com.farm.fpms.web;

import com.farm.fpms.service.AiAssistantService;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

@Controller
public class AiController {

    private final AiAssistantService aiAssistantService;

    public AiController(AiAssistantService aiAssistantService) {
        this.aiAssistantService = aiAssistantService;
    }

    @GetMapping("/ai/chat")
    public String chat(Model model) {
        model.addAttribute("answer", null);
        return "ai-chat";
    }

    @PostMapping("/ai/chat")
    public String ask(@RequestParam String question, @RequestParam(required = false) Long batchId, Model model) {
        model.addAttribute("question", question);
        model.addAttribute("batchId", batchId);
        try {
            model.addAttribute("answer", aiAssistantService.answer(question, batchId));
        } catch (RuntimeException ex) {
            model.addAttribute("error", ex.getMessage());
        }
        return "ai-chat";
    }

    @GetMapping(path = "/ai/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @ResponseBody
    public SseEmitter stream(@RequestParam String question, @RequestParam(required = false) Long batchId) {
        SseEmitter emitter = new SseEmitter(120000L);
        new Thread(() -> {
            try {
                aiAssistantService.streamAnswer(question, batchId, token -> {
                    try {
                        emitter.send(SseEmitter.event().name("token").data(token));
                    } catch (IOException ignored) {}
                });
                emitter.send(SseEmitter.event().name("done").data(""));
                emitter.complete();
            } catch (Exception ex) {
                try {
                    emitter.send(SseEmitter.event().name("error").data(ex.getMessage() != null ? ex.getMessage() : "AI 服务暂时不可用"));
                } catch (IOException ignored) {}
                emitter.complete();
            }
        }).start();
        return emitter;
    }
}
