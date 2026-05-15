package com.farm.fpms.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BatchStatusMachineTest {

    private final BatchStatusMachine machine = new BatchStatusMachine();

    @Test
    void plannedCanMoveToSowed() {
        assertThat(machine.canTransit(BatchStatus.PLANNED, BatchStatus.SOWED)).isTrue();
    }

    @Test
    void plannedCannotJumpToHarvesting() {
        assertThat(machine.canTransit(BatchStatus.PLANNED, BatchStatus.HARVESTING)).isFalse();

        assertThatThrownBy(() -> machine.requireTransit(BatchStatus.PLANNED, BatchStatus.HARVESTING))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("非法批次状态迁移");
    }

    @Test
    void growingCanBeAbandoned() {
        assertThat(machine.canTransit(BatchStatus.GROWING, BatchStatus.ABANDONED)).isTrue();
    }
}
