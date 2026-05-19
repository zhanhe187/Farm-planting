package com.farm.fpms.service;

import com.farm.fpms.common.BusinessException;
import com.farm.fpms.common.SessionUser;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
public class SaleService {

    private final JdbcTemplate jdbcTemplate;

    public SaleService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Map<String, Object>> listAll() {
        return jdbcTemplate.queryForList(
                "select s.*, b.batch_no from sale_order s " +
                        "left join plant_batch b on b.id = s.batch_id order by s.sale_date desc, s.id desc");
    }

    public List<Map<String, Object>> listForUser(SessionUser user) {
        if (user != null && "CUSTOMER".equals(user.getRoleCode())) {
            return jdbcTemplate.queryForList(
                    "select s.*, b.batch_no from sale_order s " +
                            "left join plant_batch b on b.id = s.batch_id where s.customer_name = ? order by s.sale_date desc, s.id desc",
                    user.getDisplayName());
        }
        return listAll();
    }

    public List<Map<String, Object>> listMarketCrops() {
        return jdbcTemplate.queryForList(
                "select id, name, variety, sale_price_per_kg, image_url from farm_crop " +
                        "where enabled = 1 and sale_price_per_kg > 0 order by name, variety");
    }

    public void purchaseCrop(SessionUser user, long cropId, BigDecimal quantityKg) {
        if (user == null || !"CUSTOMER".equals(user.getRoleCode())) {
            throw new BusinessException("只有客户账号可以自助购买农作物");
        }
        BigDecimal normalizedQuantity = requirePositive(quantityKg, "购买数量必须大于 0");
        Map<String, Object> crop = requireMarketCrop(cropId);
        String cropName = String.valueOf(crop.get("name"));
        Object variety = crop.get("variety");
        String productName = variety == null || String.valueOf(variety).trim().isEmpty()
                ? cropName
                : cropName + "（" + String.valueOf(variety).trim() + "）";
        BigDecimal unitPrice = (BigDecimal) crop.get("sale_price_per_kg");
        create(null, user.getDisplayName(), productName, normalizedQuantity, unitPrice, LocalDate.now(), "客户自助购买");
    }

    public void create(Long batchId, String customerName, String productName,
                       BigDecimal quantityKg, BigDecimal unitPrice, LocalDate saleDate, String note) {
        String normalizedCustomer = requireText(customerName, "客户名称不能为空");
        String normalizedProduct = requireText(productName, "产品名称不能为空");
        BigDecimal normalizedQuantity = requirePositive(quantityKg, "销售数量必须大于 0");
        BigDecimal normalizedUnitPrice = requireNonNegative(unitPrice, "销售单价不能小于 0");
        if (saleDate == null) {
            throw new BusinessException("销售日期不能为空");
        }
        BigDecimal totalAmount = normalizedQuantity.multiply(normalizedUnitPrice).setScale(2, RoundingMode.HALF_UP);
        jdbcTemplate.update(
                "insert into sale_order(batch_id, customer_name, product_name, quantity_kg, unit_price, total_amount, sale_date, note) " +
                        "values(?,?,?,?,?,?,?,?)",
                batchId, normalizedCustomer, normalizedProduct, normalizedQuantity, normalizedUnitPrice, totalAmount,
                java.sql.Date.valueOf(saleDate), note);
    }

    private Map<String, Object> requireMarketCrop(long cropId) {
        try {
            return jdbcTemplate.queryForMap(
                    "select name, variety, sale_price_per_kg from farm_crop where id = ? and enabled = 1 and sale_price_per_kg > 0",
                    cropId);
        } catch (EmptyResultDataAccessException ex) {
            throw new BusinessException("农作物不存在或未上架");
        }
    }

    private String requireText(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new BusinessException(message);
        }
        return value.trim();
    }

    private BigDecimal requirePositive(BigDecimal value, String message) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(message);
        }
        return value;
    }

    private BigDecimal requireNonNegative(BigDecimal value, String message) {
        if (value == null || value.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(message);
        }
        return value;
    }
}
