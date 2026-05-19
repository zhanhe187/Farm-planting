package com.farm.fpms.entity;

public class VisionRecognitionResult {

    private String name;
    private String latinName;
    private String usage;
    private String cultivation;
    private String soil;
    private String commonPest;
    private double confidence;
    private String rawText;

    public VisionRecognitionResult() {
    }

    public VisionRecognitionResult(String name, String latinName, String usage, String cultivation,
                                   String soil, String commonPest, double confidence) {
        this.name = name;
        this.latinName = latinName;
        this.usage = usage;
        this.cultivation = cultivation;
        this.soil = soil;
        this.commonPest = commonPest;
        this.confidence = confidence;
    }

    public static VisionRecognitionResult fromRawText(String rawText) {
        VisionRecognitionResult result = new VisionRecognitionResult();
        result.setName("未识别");
        result.setUsage("请根据下方 AI 原文人工确认。");
        result.setCultivation("AI 未返回结构化 JSON，系统保留原文供参考。");
        result.setRawText(rawText);
        return result;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLatinName() {
        return latinName;
    }

    public void setLatinName(String latinName) {
        this.latinName = latinName;
    }

    public String getUsage() {
        return usage;
    }

    public void setUsage(String usage) {
        this.usage = usage;
    }

    public String getCultivation() {
        return cultivation;
    }

    public void setCultivation(String cultivation) {
        this.cultivation = cultivation;
    }

    public String getSoil() {
        return soil;
    }

    public void setSoil(String soil) {
        this.soil = soil;
    }

    public String getCommonPest() {
        return commonPest;
    }

    public void setCommonPest(String commonPest) {
        this.commonPest = commonPest;
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }

    public String getRawText() {
        return rawText;
    }

    public void setRawText(String rawText) {
        this.rawText = rawText;
    }
}
