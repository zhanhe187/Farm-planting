package com.farm.fpms.domain;

public enum OperationType {
    SOWING("播种"),
    FERTILIZE("施肥"),
    PESTICIDE("施药"),
    IRRIGATION("灌溉"),
    WEEDING("除草"),
    PEST_CHECK("病虫害巡检"),
    HARVEST("采收"),
    CORRECTION("红冲修正");

    private final String label;

    OperationType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
