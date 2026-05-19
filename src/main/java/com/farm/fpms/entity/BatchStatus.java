package com.farm.fpms.entity;

public enum BatchStatus {
    PLANNED("已计划"),
    SOWED("已播种"),
    GROWING("生长期"),
    READY_HARVEST("待采收"),
    HARVESTING("采收中"),
    COMPLETED("已完结"),
    ABANDONED("已废弃");

    private final String label;

    BatchStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static BatchStatus fromLabel(String label) {
        for (BatchStatus s : values()) {
            if (s.label.equals(label) || s.name().equals(label)) {
                return s;
            }
        }
        throw new IllegalArgumentException("未知批次状态: " + label);
    }
}
