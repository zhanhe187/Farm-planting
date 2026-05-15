package com.farm.fpms.service;

import com.farm.fpms.domain.BatchStatus;
import com.farm.fpms.domain.BatchStatusMachine;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        BatchStatus from = BatchStatus.valueOf(current);
        statusMachine.requireTransit(from, target);
        jdbcTemplate.update("update plant_batch set status = ? where id = ?", target.name(), batchId);
        jdbcTemplate.update("insert into batch_status_log(batch_id, from_status, to_status, operator_name, reason) values(?, ?, ?, ?, ?)",
                batchId, from.name(), target.name(), operator, reason);
    }
}
