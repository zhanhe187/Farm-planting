package com.farm.fpms.service;

import com.farm.fpms.common.BusinessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Types;

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

    public double replenishAvailable(double available, double quantity) {
        if (quantity <= 0) {
            throw new BusinessException("入库数量必须大于 0");
        }
        return available + quantity;
    }

    @Transactional
    public long createMaterial(String name, String category, String unit, int safeIntervalDays,
                               BigDecimal unitPrice, Long cropId, double initialQuantity, double safetyStock) {
        String normalizedName = normalizeRequired(name, "农资名称不能为空");
        String normalizedCategory = normalizeRequired(category, "农资类别不能为空");
        String normalizedUnit = normalizeRequired(unit, "计量单位不能为空");
        BigDecimal normalizedPrice = normalizeMoney(unitPrice, "农资单价不能小于 0");
        if (safeIntervalDays < 0) {
            throw new BusinessException("安全间隔期不能小于 0 天");
        }
        if (initialQuantity < 0) {
            throw new BusinessException("初始库存不能小于 0");
        }
        if (safetyStock < 0) {
            throw new BusinessException("安全库存不能小于 0");
        }
        if ("种子".equals(normalizedCategory) && cropId == null) {
            throw new BusinessException("种子农资必须关联可种植作物");
        }
        if ("种子".equals(normalizedCategory) && !activeCropExists(cropId)) {
            throw new BusinessException("种子关联的作物不存在或已停用");
        }

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "insert into farm_material(name, category, unit, safe_interval_days, unit_price, crop_id) values(?, ?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, normalizedName);
            ps.setString(2, normalizedCategory);
            ps.setString(3, normalizedUnit);
            ps.setInt(4, safeIntervalDays);
            ps.setBigDecimal(5, normalizedPrice);
            if (cropId == null) {
                ps.setNull(6, Types.BIGINT);
            } else {
                ps.setLong(6, cropId);
            }
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new BusinessException("农资创建失败，未返回新农资编号");
        }
        long materialId = key.longValue();
        jdbcTemplate.update("insert into stock_inventory(material_id, quantity, safety_stock, version) values(?, ?, ?, 0)",
                materialId, initialQuantity, safetyStock);
        if (initialQuantity > 0) {
            jdbcTemplate.update(
                    "insert into stock_in_order(material_id, quantity, unit_price, total_amount, created_at) values(?, ?, ?, ?, current_timestamp)",
                    materialId, initialQuantity, normalizedPrice, totalAmount(initialQuantity, normalizedPrice));
        }
        return materialId;
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

    @Transactional
    public void replenishMaterial(long materialId, double quantity) {
        replenishMaterial(materialId, quantity, BigDecimal.ZERO);
    }

    @Transactional
    public void replenishMaterial(long materialId, double quantity, BigDecimal unitPrice) {
        BigDecimal normalizedPrice = normalizeMoney(unitPrice, "入库成本价不能小于 0");
        Double current = jdbcTemplate.queryForObject(
                "select quantity from stock_inventory where material_id = ?",
                Double.class,
                materialId);
        double updated = replenishAvailable(current == null ? 0.0 : current, quantity);
        jdbcTemplate.update("update stock_inventory set quantity = ?, version = version + 1 where material_id = ?",
                updated, materialId);
        jdbcTemplate.update(
                "insert into stock_in_order(material_id, quantity, unit_price, total_amount, created_at) values(?, ?, ?, ?, current_timestamp)",
                materialId, quantity, normalizedPrice, totalAmount(quantity, normalizedPrice));
        jdbcTemplate.update("update farm_material set unit_price = ? where id = ?",
                normalizedPrice, materialId);
    }

    private String normalizeRequired(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new BusinessException(message);
        }
        return value.trim();
    }

    private BigDecimal normalizeMoney(BigDecimal value, String negativeMessage) {
        BigDecimal normalized = value == null ? BigDecimal.ZERO : value;
        if (normalized.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(negativeMessage);
        }
        return normalized.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal totalAmount(double quantity, BigDecimal unitPrice) {
        return unitPrice.multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP);
    }

    private boolean activeCropExists(Long cropId) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from farm_crop where id = ? and enabled = 1",
                Integer.class,
                cropId);
        return count != null && count > 0;
    }
}
