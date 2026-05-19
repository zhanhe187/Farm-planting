package com.farm.fpms.service;

import com.farm.fpms.common.BusinessException;
import com.farm.fpms.common.SessionUser;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SaleServiceTest {

    @Test
    void createRejectsNonPositiveQuantity() {
        SaleService service = new SaleService(mock(JdbcTemplate.class));

        assertThatThrownBy(() -> service.create(1L, "社区团购", "甜玉米",
                BigDecimal.ZERO, new BigDecimal("6.50"), LocalDate.of(2026, 5, 19), null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("销售数量必须大于 0");
    }

    @Test
    void createRejectsNegativeUnitPrice() {
        SaleService service = new SaleService(mock(JdbcTemplate.class));

        assertThatThrownBy(() -> service.create(1L, "社区团购", "甜玉米",
                new BigDecimal("10.00"), new BigDecimal("-1.00"), LocalDate.of(2026, 5, 19), null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("销售单价不能小于 0");
    }

    @Test
    void createStoresRoundedTotalAmount() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        SaleService service = new SaleService(jdbcTemplate);

        service.create(4L, "社区团购", "甜玉米",
                new BigDecimal("12.50"), new BigDecimal("6.50"), LocalDate.of(2026, 5, 19), "新客");

        verify(jdbcTemplate).update(
                "insert into sale_order(batch_id, customer_name, product_name, quantity_kg, unit_price, total_amount, sale_date, note) " +
                        "values(?,?,?,?,?,?,?,?)",
                4L, "社区团购", "甜玉米", new BigDecimal("12.50"), new BigDecimal("6.50"),
                new BigDecimal("81.25"), java.sql.Date.valueOf(LocalDate.of(2026, 5, 19)), "新客");
    }

    @Test
    void listForCustomerOnlyReturnsRowsMatchingDisplayName() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        SaleService service = new SaleService(jdbcTemplate);
        SessionUser customer = new SessionUser(6L, "customer", "经销客户", "CUSTOMER", "OWN_CUSTOMER");

        service.listForUser(customer);

        verify(jdbcTemplate).queryForList(
                "select s.*, b.batch_no from sale_order s " +
                        "left join plant_batch b on b.id = s.batch_id where s.customer_name = ? order by s.sale_date desc, s.id desc",
                "经销客户");
    }

    @Test
    void listForManagerReturnsAllRows() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        SaleService service = new SaleService(jdbcTemplate);
        SessionUser owner = new SessionUser(2L, "owner", "青禾农场主", "FARM_OWNER", "ALL");

        service.listForUser(owner);

        verify(jdbcTemplate).queryForList(
                "select s.*, b.batch_no from sale_order s " +
                        "left join plant_batch b on b.id = s.batch_id order by s.sale_date desc, s.id desc");
    }

    @Test
    void purchaseCropCreatesCustomerSaleFlowWithCurrentCropPrice() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        Map<String, Object> crop = new LinkedHashMap<String, Object>();
        crop.put("name", "甜玉米");
        crop.put("variety", "水果玉米");
        crop.put("sale_price_per_kg", new BigDecimal("6.50"));
        when(jdbcTemplate.queryForMap(
                "select name, variety, sale_price_per_kg from farm_crop where id = ? and enabled = 1 and sale_price_per_kg > 0",
                9L)).thenReturn(crop);
        SaleService service = new SaleService(jdbcTemplate);
        SessionUser customer = new SessionUser(6L, "customer", "经销客户", "CUSTOMER", "OWN_CUSTOMER");
        LocalDate today = LocalDate.now();

        service.purchaseCrop(customer, 9L, new BigDecimal("10.00"));

        verify(jdbcTemplate).update(
                "insert into sale_order(batch_id, customer_name, product_name, quantity_kg, unit_price, total_amount, sale_date, note) " +
                        "values(?,?,?,?,?,?,?,?)",
                null, "经销客户", "甜玉米（水果玉米）", new BigDecimal("10.00"), new BigDecimal("6.50"),
                new BigDecimal("65.00"), java.sql.Date.valueOf(today), "客户自助购买");
    }

    @Test
    void purchaseCropRejectsNonCustomerUser() {
        SaleService service = new SaleService(mock(JdbcTemplate.class));
        SessionUser owner = new SessionUser(2L, "owner", "青禾农场主", "FARM_OWNER", "ALL");

        assertThatThrownBy(() -> service.purchaseCrop(owner, 1L, new BigDecimal("5.00")))
                .isInstanceOf(BusinessException.class)
                .hasMessage("只有客户账号可以自助购买农作物");
    }
}
