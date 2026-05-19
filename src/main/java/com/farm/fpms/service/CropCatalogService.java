package com.farm.fpms.service;

import com.farm.fpms.common.BusinessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Types;
import java.util.List;
import java.util.Map;

@Service
public class CropCatalogService {

    private final JdbcTemplate jdbcTemplate;

    public CropCatalogService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public long createCrop(String name, String variety, int minGrowthDays, BigDecimal salePricePerKg, String imageUrl) {
        String normalizedName = normalizeRequired(name, "农作物名称不能为空");
        String normalizedVariety = normalizeOptional(variety);
        String normalizedImageUrl = normalizeOptional(imageUrl);
        BigDecimal normalizedPrice = normalizeMoney(salePricePerKg, "销售参考价不能小于 0");
        validateGrowthDays(minGrowthDays);

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "insert into farm_crop(name, variety, min_growth_days, sale_price_per_kg, image_url) values(?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, normalizedName);
            if (normalizedVariety == null) {
                ps.setNull(2, Types.NVARCHAR);
            } else {
                ps.setString(2, normalizedVariety);
            }
            ps.setInt(3, minGrowthDays);
            ps.setBigDecimal(4, normalizedPrice);
            if (normalizedImageUrl == null) {
                ps.setNull(5, Types.NVARCHAR);
            } else {
                ps.setString(5, normalizedImageUrl);
            }
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new BusinessException("农作物创建失败，未返回新作物编号");
        }
        return key.longValue();
    }

    @Transactional
    public void updateCrop(long id, String name, String variety, int minGrowthDays,
                           BigDecimal salePricePerKg, String imageUrl) {
        String normalizedName = normalizeRequired(name, "农作物名称不能为空");
        String normalizedVariety = normalizeOptional(variety);
        String normalizedImageUrl = normalizeOptional(imageUrl);
        BigDecimal normalizedPrice = normalizeMoney(salePricePerKg, "销售参考价不能小于 0");
        validateGrowthDays(minGrowthDays);

        jdbcTemplate.update(
                "update farm_crop set name = ?, variety = ?, min_growth_days = ?, sale_price_per_kg = ?, image_url = ?, enabled = 1 where id = ?",
                normalizedName, normalizedVariety, minGrowthDays, normalizedPrice, normalizedImageUrl, id);
    }

    @Transactional
    public void deleteCrop(long id) {
        jdbcTemplate.update("update farm_crop set enabled = 0 where id = ?", id);
    }

    public List<Map<String, Object>> listActiveCrops() {
        return jdbcTemplate.queryForList(
                "select id, name, variety, min_growth_days, sale_price_per_kg, image_url, enabled from farm_crop where enabled = 1 order by id");
    }

    public List<Map<String, Object>> listAllCrops() {
        return jdbcTemplate.queryForList(
                "select id, name, variety, min_growth_days, sale_price_per_kg, image_url, enabled from farm_crop order by enabled desc, id");
    }

    private String normalizeRequired(String value, String message) {
        String normalized = normalizeOptional(value);
        if (normalized == null) {
            throw new BusinessException(message);
        }
        return normalized;
    }

    private String normalizeOptional(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }

    private BigDecimal normalizeMoney(BigDecimal value, String negativeMessage) {
        BigDecimal normalized = value == null ? BigDecimal.ZERO : value;
        if (normalized.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(negativeMessage);
        }
        return normalized;
    }

    private void validateGrowthDays(int minGrowthDays) {
        if (minGrowthDays <= 0) {
            throw new BusinessException("生长周期必须大于 0 天");
        }
    }
}
