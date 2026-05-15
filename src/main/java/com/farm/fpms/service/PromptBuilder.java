package com.farm.fpms.service;

import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class PromptBuilder {

    private static final Pattern PHONE = Pattern.compile("(?<!\\d)(1[3-9]\\d)(\\d{4})(\\d{4})(?!\\d)");
    private static final Pattern CITIZEN_ID = Pattern.compile("(?<!\\d)([1-9]\\d{5})(\\d{8})(\\d{3}[0-9Xx])(?!\\d)");

    public String buildContextPrompt(String rawContext) {
        String safe = rawContext == null ? "" : rawContext;
        safe = maskPhone(safe);
        safe = maskCitizenId(safe);
        return "你是智慧农场农事助手。请基于以下脱敏业务上下文给出可执行建议：\n" + safe;
    }

    private String maskPhone(String text) {
        Matcher matcher = PHONE.matcher(text);
        return matcher.replaceAll("$1****$3");
    }

    private String maskCitizenId(String text) {
        Matcher matcher = CITIZEN_ID.matcher(text);
        return matcher.replaceAll("$1********$3");
    }
}
