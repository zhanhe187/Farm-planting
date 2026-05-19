package com.farm.fpms.service;

import com.farm.fpms.entity.MaterialCategory;
import com.farm.fpms.entity.OperationMaterialSnapshot;
import com.farm.fpms.entity.OperationSnapshot;
import com.farm.fpms.entity.OperationType;
import com.farm.fpms.entity.SafetyCheckResult;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;

import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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

    @Test
    void loadBatchOperationsAcceptsChineseOperationAndMaterialLabels() throws Exception {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        doAnswer(invocation -> {
            RowCallbackHandler handler = invocation.getArgument(1);
            ResultSet rs = mock(ResultSet.class);
            when(rs.getLong("operation_id")).thenReturn(7L);
            when(rs.getString("type")).thenReturn("施药");
            when(rs.getDate("operation_date")).thenReturn(java.sql.Date.valueOf(LocalDate.of(2026, 5, 10)));
            when(rs.getLong("material_id")).thenReturn(3L);
            when(rs.wasNull()).thenReturn(false);
            when(rs.getString("category")).thenReturn("农药");
            when(rs.getInt("safe_interval_days")).thenReturn(7);
            handler.processRow(rs);
            return null;
        }).when(jdbcTemplate).query(anyString(), any(RowCallbackHandler.class), eq(1L));

        List<OperationSnapshot> operations = checker.loadBatchOperations(jdbcTemplate, 1L);

        assertThat(operations).hasSize(1);
        assertThat(operations.get(0).getType()).isEqualTo(OperationType.PESTICIDE);
        assertThat(operations.get(0).getMaterials()).hasSize(1);
        assertThat(operations.get(0).getMaterials().get(0).getCategory()).isEqualTo(MaterialCategory.PESTICIDE);
    }
}
