package com.farm.fpms.service;

import com.farm.fpms.domain.BatchStatus;
import com.farm.fpms.domain.BusinessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
public class HarvestService {

    private final JdbcTemplate jdbcTemplate;
    private final SafetyIntervalChecker checker;
    private final BatchService batchService;

    public HarvestService(JdbcTemplate jdbcTemplate, SafetyIntervalChecker checker, BatchService batchService) {
        this.jdbcTemplate = jdbcTemplate;
        this.checker = checker;
        this.batchService = batchService;
    }

    public SafetyCheckResult precheck(long batchId, LocalDate harvestDate) {
        return checker.checkBatch(jdbcTemplate, batchId, harvestDate);
    }

    @Transactional
    public String harvest(long batchId, LocalDate harvestDate, double quantityKg, String grade, String operator) {
        SafetyCheckResult result = precheck(batchId, harvestDate);
        if (!result.isAllowed()) {
            throw new BusinessException(result.getMessage());
        }
        String current = jdbcTemplate.queryForObject("select status from plant_batch where id = ?", String.class, batchId);
        if (BatchStatus.READY_HARVEST.name().equals(current)) {
            batchService.transit(batchId, BatchStatus.HARVESTING, operator, "开始采收");
            batchService.transit(batchId, BatchStatus.COMPLETED, operator, "采收完成");
        } else if (!BatchStatus.COMPLETED.name().equals(current)) {
            throw new BusinessException("当前批次状态为 " + current + "，请先推进到 READY_HARVEST");
        }
        String traceCode = generateTraceCode(batchId, harvestDate);
        jdbcTemplate.update("insert into harvest_record(batch_id, harvest_date, quantity_kg, quality_grade, trace_code) values(?, ?, ?, ?, ?)",
                batchId, java.sql.Date.valueOf(harvestDate), quantityKg, grade, traceCode);
        jdbcTemplate.update("update plant_batch set actual_harvest_date = ?, trace_code = ? where id = ?",
                java.sql.Date.valueOf(harvestDate), traceCode, batchId);
        return traceCode;
    }

    private String generateTraceCode(long batchId, LocalDate harvestDate) {
        return "F000001-" + harvestDate.toString().replace("-", "") + "-" + Long.toString(batchId, 36).toUpperCase();
    }
}
