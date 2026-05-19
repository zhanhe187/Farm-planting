package com.farm.fpms.service;

import com.farm.fpms.entity.BatchStatus;
import com.farm.fpms.entity.SafetyCheckResult;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HarvestServiceTest {

    @Test
    void harvestCompletesBatchAlreadyInHarvestingStatus() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        SafetyIntervalChecker checker = mock(SafetyIntervalChecker.class);
        BatchService batchService = mock(BatchService.class);
        LocalDate harvestDate = LocalDate.of(2026, 5, 18);

        when(checker.checkBatch(jdbcTemplate, 2L, harvestDate))
                .thenReturn(new SafetyCheckResult(true, null, "安全间隔期已满足，可以采收"));
        when(jdbcTemplate.queryForObject("select status from plant_batch where id = ?", String.class, 2L))
                .thenReturn("采收中");

        HarvestService service = new HarvestService(jdbcTemplate, checker, batchService);

        service.harvest(2L, harvestDate, 1280, "一级", "系统管理员");

        verify(batchService).transit(2L, BatchStatus.COMPLETED, "系统管理员", "采收完成");
        verify(jdbcTemplate).update(eq("insert into harvest_record(batch_id, harvest_date, quantity_kg, quality_grade, trace_code) values(?, ?, ?, ?, ?)"),
                eq(2L), any(java.sql.Date.class), eq(1280.0), eq("一级"), eq("F000001-20260518-2"));
    }

    @Test
    void harvestRejectsCompletedBatchToAvoidDuplicateTraceRecords() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        SafetyIntervalChecker checker = mock(SafetyIntervalChecker.class);
        BatchService batchService = mock(BatchService.class);
        LocalDate harvestDate = LocalDate.of(2026, 5, 18);

        when(checker.checkBatch(jdbcTemplate, 3L, harvestDate))
                .thenReturn(new SafetyCheckResult(true, null, "安全间隔期已满足，可以采收"));
        when(jdbcTemplate.queryForObject("select status from plant_batch where id = ?", String.class, 3L))
                .thenReturn("已完结");

        HarvestService service = new HarvestService(jdbcTemplate, checker, batchService);

        assertThatThrownBy(() -> service.harvest(3L, harvestDate, 1280, "一级", "系统管理员"))
                .isInstanceOf(com.farm.fpms.common.BusinessException.class)
                .hasMessageContaining("已完结");
        verify(batchService, never()).transit(eq(3L), any(BatchStatus.class), any(String.class), any(String.class));
        verify(jdbcTemplate, never()).update(eq("insert into harvest_record(batch_id, harvest_date, quantity_kg, quality_grade, trace_code) values(?, ?, ?, ?, ?)"),
                any(), any(), any(), any(), any());
    }

    @Test
    void harvestRejectsNonPositiveQuantityBeforeSafetyCheck() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        SafetyIntervalChecker checker = mock(SafetyIntervalChecker.class);
        BatchService batchService = mock(BatchService.class);
        HarvestService service = new HarvestService(jdbcTemplate, checker, batchService);

        assertThatThrownBy(() -> service.harvest(2L, LocalDate.of(2026, 5, 18), 0, "一级", "系统管理员"))
                .isInstanceOf(com.farm.fpms.common.BusinessException.class)
                .hasMessageContaining("采收数量必须大于 0");

        verify(checker, never()).checkBatch(any(JdbcTemplate.class), any(Long.class), any(LocalDate.class));
    }
}
