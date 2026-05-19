package com.farm.fpms.service;

import com.farm.fpms.entity.BatchStatus;
import com.farm.fpms.entity.BatchStatusMachine;
import com.farm.fpms.common.BusinessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;

@Service
public class BatchService {

    private final JdbcTemplate jdbcTemplate;
    private final BatchStatusMachine statusMachine;

    public BatchService(JdbcTemplate jdbcTemplate, BatchStatusMachine statusMachine) {
        this.jdbcTemplate = jdbcTemplate;
        this.statusMachine = statusMachine;
    }

    @Transactional
    public void transit(long batchId, BatchStatus target, String operator, String reason) {
        String current = jdbcTemplate.queryForObject("select status from plant_batch where id = ?", String.class, batchId);
        BatchStatus from = BatchStatus.fromLabel(current);
        statusMachine.requireTransit(from, target);
        jdbcTemplate.update("update plant_batch set status = ? where id = ?", target.getLabel(), batchId);
        jdbcTemplate.update("insert into batch_status_log(batch_id, from_status, to_status, operator_name, reason) values(?, ?, ?, ?, ?)",
                batchId, from.getLabel(), target.getLabel(), operator, reason);
    }

    @Transactional
    public long createBatch(String batchNo, long plotId, long cropId, double plannedAreaMu,
                            LocalDate sowDate, LocalDate expectedHarvestDate, Long ownerId) {
        if (batchNo == null || batchNo.trim().isEmpty()) {
            throw new BusinessException("批次号不能为空");
        }
        if (plannedAreaMu <= 0) {
            throw new BusinessException("计划面积必须大于 0");
        }
        if (expectedHarvestDate == null) {
            throw new BusinessException("预计采收日期不能为空");
        }
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "insert into plant_batch(batch_no, plot_id, crop_id, status, planned_area_mu, sow_date, expected_harvest_date, owner_id) values(?, ?, ?, ?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, batchNo.trim());
            ps.setLong(2, plotId);
            ps.setLong(3, cropId);
            ps.setString(4, BatchStatus.PLANNED.getLabel());
            ps.setDouble(5, plannedAreaMu);
            if (sowDate == null) {
                ps.setNull(6, Types.DATE);
            } else {
                ps.setDate(6, java.sql.Date.valueOf(sowDate));
            }
            ps.setDate(7, java.sql.Date.valueOf(expectedHarvestDate));
            if (ownerId == null) {
                ps.setNull(8, Types.BIGINT);
            } else {
                ps.setLong(8, ownerId);
            }
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new BusinessException("批次创建失败，未返回新批次编号");
        }
        return key.longValue();
    }
}
