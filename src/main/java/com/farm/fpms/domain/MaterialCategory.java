package com.farm.fpms.domain;

public enum MaterialCategory {
    SEED("种子"),
    FERTILIZER("化肥"),
    PESTICIDE("农药"),
    TOOL("工具"),
    OTHER("其他");

    private final String label;

    MaterialCategory(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
