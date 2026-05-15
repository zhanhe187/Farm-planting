package com.farm.fpms.service;

import com.farm.fpms.domain.MaterialCategory;
import com.farm.fpms.domain.OperationType;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

class SafetyIntervalCheckerTest {

    private final SafetyIntervalChecker checker = new SafetyIntervalChecker();

    @Test
    void noPesticideAllowsHarvest() {
        SafetyCheckResult result = checker.check(Collections.emptyList(), LocalDate.of(2026, 5, 14));

        assertThat(result.isAllowed()).isTrue();
        assertThat(result.getForbidUntil()).isNull();
    }

    @Test
    void pesticideWithinIntervalBlocksHarvest() {
        OperationMaterialSnapshot material = new OperationMaterialSnapshot(1L, MaterialCategory.PESTICIDE, 7);
        OperationSnapshot operation = new OperationSnapshot(1L, OperationType.PESTICIDE,
                LocalDate.of(2026, 5, 10), Collections.singletonList(material));

        SafetyCheckResult result = checker.check(Collections.singletonList(operation), LocalDate.of(2026, 5, 14));

        assertThat(result.isAllowed()).isFalse();
        assertThat(result.getForbidUntil()).isEqualTo(LocalDate.of(2026, 5, 17));
    }

    @Test
    void latestPesticideIntervalWins() {
        OperationMaterialSnapshot shortInterval = new OperationMaterialSnapshot(1L, MaterialCategory.PESTICIDE, 3);
        OperationMaterialSnapshot longInterval = new OperationMaterialSnapshot(2L, MaterialCategory.PESTICIDE, 10);
        OperationSnapshot first = new OperationSnapshot(1L, OperationType.PESTICIDE,
                LocalDate.of(2026, 5, 1), Collections.singletonList(longInterval));
        OperationSnapshot second = new OperationSnapshot(2L, OperationType.FERTILIZE,
                LocalDate.of(2026, 5, 12), Collections.singletonList(shortInterval));

        SafetyCheckResult result = checker.check(Arrays.asList(first, second), LocalDate.of(2026, 5, 15));

        assertThat(result.isAllowed()).isTrue();
        assertThat(result.getForbidUntil()).isEqualTo(LocalDate.of(2026, 5, 15));
    }
}
