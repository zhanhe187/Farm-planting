package com.farm.fpms.service;

import com.farm.fpms.domain.BusinessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StockService {

    private JdbcTemplate jdbcTemplate;

    public StockService() {
    }

    @Autowired
    public StockService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public double consumeAvailable(double available, double quantity) {
        if (quantity <= 0) {
            throw new BusinessException("消耗数量必须大于 0");
        }
        if (available < quantity) {
            throw new BusinessException("库存不足，当前库存 " + available + "，需要 " + quantity);
        }
        return available - quantity;
    }

    @Transactional
    public void consumeMaterial(long materialId, double quantity, Long operationId) {
        Double current = jdbcTemplate.queryForObject(
                "select quantity from stock_inventory where material_id = ?",
                Double.class,
                materialId);
        double left = consumeAvailable(current == null ? 0.0 : current, quantity);
        jdbcTemplate.update("update stock_inventory set quantity = ?, version = version + 1 where material_id = ?",
                left, materialId);
        jdbcTemplate.update("insert into stock_out_order(material_id, quantity, type, operation_id, created_at) " +
                        "values(?, ?, 'OPERATION_USE', ?, current_timestamp)",
                materialId, quantity, operationId);
    }
}
