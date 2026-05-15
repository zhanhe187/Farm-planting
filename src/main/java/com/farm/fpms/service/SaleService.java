package com.farm.fpms.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
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

    public void create(Long batchId, String customerName, String productName,
                       BigDecimal quantityKg, BigDecimal unitPrice, LocalDate saleDate, String note) {
        BigDecimal totalAmount = quantityKg.multiply(unitPrice);
        jdbcTemplate.update(
                "insert into sale_order(batch_id, customer_name, product_name, quantity_kg, unit_price, total_amount, sale_date, note) " +
                        "values(?,?,?,?,?,?,?,?)",
                batchId, customerName, productName, quantityKg, unitPrice, totalAmount,
                java.sql.Date.valueOf(saleDate), note);
    }
}
