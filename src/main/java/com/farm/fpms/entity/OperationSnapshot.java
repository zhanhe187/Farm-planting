package com.farm.fpms.entity;

import com.farm.fpms.entity.OperationType;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

public class OperationSnapshot {

    private final Long operationId;
    private final OperationType type;
    private final LocalDate operationDate;
    private final List<OperationMaterialSnapshot> materials;

    public OperationSnapshot(Long operationId, OperationType type, LocalDate operationDate,
                             List<OperationMaterialSnapshot> materials) {
        this.operationId = operationId;
        this.type = type;
        this.operationDate = operationDate;
        this.materials = materials == null ? Collections.<OperationMaterialSnapshot>emptyList() : materials;
    }

    public Long getOperationId() {
        return operationId;
    }

    public OperationType getType() {
        return type;
    }

    public LocalDate getOperationDate() {
        return operationDate;
    }

    public List<OperationMaterialSnapshot> getMaterials() {
        return materials;
    }
}
