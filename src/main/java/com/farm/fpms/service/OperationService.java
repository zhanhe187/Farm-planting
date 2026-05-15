package com.farm.fpms.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDate;

@Service
public class OperationService {

    private final JdbcTemplate jdbcTemplate;
    private final StockService stockService;

    public OperationService(JdbcTemplate jdbcTemplate, StockService stockService) {
        this.jdbcTemplate = jdbcTemplate;
        this.stockService = stockService;
    }

    @Transactional
    public long createOperation(long batchId, String type, LocalDate operationDate, String workerName,
                                String note, Long materialId, Double quantity) {
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "insert into plant_operation(batch_id, type, operation_date, worker_name, note) values(?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, batchId);
            ps.setString(2, type);
            ps.setDate(3, java.sql.Date.valueOf(operationDate));
            ps.setString(4, workerName);
            ps.setString(5, note);
            return ps;
        }, keyHolder);
        Long operationId = keyHolder.getKey() == null ? 0L : keyHolder.getKey().longValue();
        if (materialId != null && quantity != null && quantity > 0) {
            jdbcTemplate.update("insert into plant_operation_material(operation_id, material_id, quantity) values(?, ?, ?)",
                    operationId, materialId, quantity);
            stockService.consumeMaterial(materialId, quantity, operationId);
        }
        return operationId;
    }
}
