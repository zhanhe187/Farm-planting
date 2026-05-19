package com.farm.fpms.service;

import com.farm.fpms.common.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.support.GeneratedKeyHolder;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StockServiceTest {

    @Test
    void consumeReducesAvailableStock() {
        StockService service = new StockService();
        double left = service.consumeAvailable(100.0, 25.5);

        assertThat(left).isEqualTo(74.5);
    }

    @Test
    void consumeRejectsInsufficientStock() {
        StockService service = new StockService();

        assertThatThrownBy(() -> service.consumeAvailable(10.0, 11.0))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("库存不足");
    }

    @Test
    void replenishAddsToAvailableStock() {
        StockService service = new StockService();

        double updated = service.replenishAvailable(100.0, 25.5);

        assertThat(updated).isEqualTo(125.5);
    }

    @Test
    void replenishRejectsNonPositiveQuantity() {
        StockService service = new StockService();

        assertThatThrownBy(() -> service.replenishAvailable(100.0, 0))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("入库数量必须大于 0");
    }

    @Test
    void createSeedMaterialRequiresLinkedCrop() {
        StockService service = new StockService(mock(JdbcTemplate.class));

        assertThatThrownBy(() -> service.createMaterial("番茄种子", "种子", "袋", 0,
                        new BigDecimal("18.50"), null, 20.0, 5.0))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("种子农资必须关联可种植作物");
    }

    @Test
    void createMaterialStoresPriceAndCreatesInventory() throws Exception {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForObject("select count(*) from farm_crop where id = ? and enabled = 1",
                Integer.class, 1L)).thenReturn(1);
        when(jdbcTemplate.update(any(PreparedStatementCreator.class), any(GeneratedKeyHolder.class)))
                .thenAnswer(invocation -> {
                    GeneratedKeyHolder keyHolder = invocation.getArgument(1);
                    keyHolder.getKeyList().add(Collections.singletonMap("id", 31L));
                    return 1;
                });
        StockService service = new StockService(jdbcTemplate);

        long materialId = service.createMaterial("番茄种子", "种子", "袋", 0,
                new BigDecimal("18.50"), 1L, 20.0, 5.0);

        assertThat(materialId).isEqualTo(31L);
        org.mockito.ArgumentCaptor<PreparedStatementCreator> creatorCaptor =
                org.mockito.ArgumentCaptor.forClass(PreparedStatementCreator.class);
        verify(jdbcTemplate).update(creatorCaptor.capture(), any(GeneratedKeyHolder.class));

        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        when(connection.prepareStatement(anyString(), eq(Statement.RETURN_GENERATED_KEYS))).thenReturn(statement);
        creatorCaptor.getValue().createPreparedStatement(connection);

        verify(statement).setString(1, "番茄种子");
        verify(statement).setString(2, "种子");
        verify(statement).setString(3, "袋");
        verify(statement).setInt(4, 0);
        verify(statement).setBigDecimal(5, new BigDecimal("18.50"));
        verify(statement).setLong(6, 1L);
        verify(jdbcTemplate).update(
                "insert into stock_inventory(material_id, quantity, safety_stock, version) values(?, ?, ?, 0)",
                31L, 20.0, 5.0);
    }

    @Test
    void createSeedMaterialRejectsDisabledOrMissingCrop() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForObject("select count(*) from farm_crop where id = ? and enabled = 1",
                Integer.class, 99L)).thenReturn(0);
        StockService service = new StockService(jdbcTemplate);

        assertThatThrownBy(() -> service.createMaterial("停用作物种子", "种子", "袋", 0,
                        new BigDecimal("10.00"), 99L, 1.0, 1.0))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("种子关联的作物不存在或已停用");
    }

    @Test
    void replenishMaterialRecordsPurchaseCost() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForObject("select quantity from stock_inventory where material_id = ?",
                Double.class, 2L)).thenReturn(10.0);
        StockService service = new StockService(jdbcTemplate);

        service.replenishMaterial(2L, 15.0, new BigDecimal("3.60"));

        verify(jdbcTemplate).update("update stock_inventory set quantity = ?, version = version + 1 where material_id = ?",
                25.0, 2L);
        verify(jdbcTemplate).update(
                "insert into stock_in_order(material_id, quantity, unit_price, total_amount, created_at) values(?, ?, ?, ?, current_timestamp)",
                2L, 15.0, new BigDecimal("3.60"), new BigDecimal("54.00"));
        verify(jdbcTemplate).update("update farm_material set unit_price = ? where id = ?",
                new BigDecimal("3.60"), 2L);
    }
}
