package com.farm.fpms.entity;

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

    public static MaterialCategory fromLabel(String label) {
        for (MaterialCategory c : values()) {
            if (c.label.equals(label) || c.name().equals(label)) {
                return c;
            }
        }
        throw new IllegalArgumentException("未知物料类别: " + label);
    }
}
