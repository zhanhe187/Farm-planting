package com.farm.fpms.entity;

import com.farm.fpms.common.BusinessException;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

@Component
public class BatchStatusMachine {

    private final Map<BatchStatus, Set<BatchStatus>> allowedTransitions;

    public BatchStatusMachine() {
        Map<BatchStatus, Set<BatchStatus>> rules = new EnumMap<BatchStatus, Set<BatchStatus>>(BatchStatus.class);
        rules.put(BatchStatus.PLANNED, EnumSet.of(BatchStatus.SOWED, BatchStatus.ABANDONED));
        rules.put(BatchStatus.SOWED, EnumSet.of(BatchStatus.GROWING, BatchStatus.ABANDONED));
        rules.put(BatchStatus.GROWING, EnumSet.of(BatchStatus.READY_HARVEST, BatchStatus.ABANDONED));
        rules.put(BatchStatus.READY_HARVEST, EnumSet.of(BatchStatus.HARVESTING, BatchStatus.ABANDONED));
        rules.put(BatchStatus.HARVESTING, EnumSet.of(BatchStatus.COMPLETED));
        rules.put(BatchStatus.COMPLETED, EnumSet.noneOf(BatchStatus.class));
        rules.put(BatchStatus.ABANDONED, EnumSet.noneOf(BatchStatus.class));
        this.allowedTransitions = Collections.unmodifiableMap(rules);
    }

    public boolean canTransit(BatchStatus from, BatchStatus to) {
        if (from == null || to == null) {
            return false;
        }
        Set<BatchStatus> targets = allowedTransitions.get(from);
        return targets != null && targets.contains(to);
    }

    public void requireTransit(BatchStatus from, BatchStatus to) {
        if (!canTransit(from, to)) {
            throw new BusinessException("非法批次状态迁移：" + from + " -> " + to);
        }
    }
}
