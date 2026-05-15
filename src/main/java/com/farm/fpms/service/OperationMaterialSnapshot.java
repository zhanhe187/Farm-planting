package com.farm.fpms.service;

import com.farm.fpms.domain.MaterialCategory;

public class OperationMaterialSnapshot {

    private final Long materialId;
    private final MaterialCategory category;
    private final int safeIntervalDays;

    public OperationMaterialSnapshot(Long materialId, MaterialCategory category, int safeIntervalDays) {
        this.materialId = materialId;
        this.category = category;
        this.safeIntervalDays = safeIntervalDays;
    }

    public Long getMaterialId() {
        return materialId;
    }

    public MaterialCategory getCategory() {
        return category;
    }

    public int getSafeIntervalDays() {
        return safeIntervalDays;
    }
}
