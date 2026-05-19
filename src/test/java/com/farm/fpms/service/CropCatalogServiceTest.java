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

class CropCatalogServiceTest {

    @Test
    void createCropStoresChineseCropWithSalePrice() throws Exception {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.update(any(PreparedStatementCreator.class), any(GeneratedKeyHolder.class)))
                .thenAnswer(invocation -> {
                    GeneratedKeyHolder keyHolder = invocation.getArgument(1);
                    keyHolder.getKeyList().add(Collections.singletonMap("id", 21L));
                    return 1;
                });

        CropCatalogService service = new CropCatalogService(jdbcTemplate);

        long id = service.createCrop("西兰花", "耐寒青梗", 65, new BigDecimal("6.80"), null);

        assertThat(id).isEqualTo(21L);
        org.mockito.ArgumentCaptor<PreparedStatementCreator> creatorCaptor =
                org.mockito.ArgumentCaptor.forClass(PreparedStatementCreator.class);
        verify(jdbcTemplate).update(creatorCaptor.capture(), any(GeneratedKeyHolder.class));

        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        when(connection.prepareStatement(anyString(), eq(Statement.RETURN_GENERATED_KEYS))).thenReturn(statement);
        creatorCaptor.getValue().createPreparedStatement(connection);

        verify(statement).setString(1, "西兰花");
        verify(statement).setString(2, "耐寒青梗");
        verify(statement).setInt(3, 65);
        verify(statement).setBigDecimal(4, new BigDecimal("6.80"));
    }

    @Test
    void createCropRejectsNegativeSalePrice() {
        CropCatalogService service = new CropCatalogService(mock(JdbcTemplate.class));

        assertThatThrownBy(() -> service.createCrop("番茄", "粉果", 80, new BigDecimal("-1.00"), null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("销售参考价不能小于 0");
    }

    @Test
    void updateCropChangesSalePriceAndBasicInfo() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        CropCatalogService service = new CropCatalogService(jdbcTemplate);

        service.updateCrop(7L, "甜玉米", "水果玉米", 72, new BigDecimal("6.50"), "");

        verify(jdbcTemplate).update(
                "update farm_crop set name = ?, variety = ?, min_growth_days = ?, sale_price_per_kg = ?, image_url = ?, enabled = 1 where id = ?",
                "甜玉米", "水果玉米", 72, new BigDecimal("6.50"), null, 7L);
    }

    @Test
    void updateCropRejectsNonPositiveGrowthDays() {
        CropCatalogService service = new CropCatalogService(mock(JdbcTemplate.class));

        assertThatThrownBy(() -> service.updateCrop(7L, "甜玉米", "水果玉米", 0, new BigDecimal("6.50"), null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("生长周期必须大于 0 天");
    }

    @Test
    void deleteCropSoftDisablesCropInsteadOfBreakingHistory() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        CropCatalogService service = new CropCatalogService(jdbcTemplate);

        service.deleteCrop(7L);

        verify(jdbcTemplate).update("update farm_crop set enabled = 0 where id = ?", 7L);
    }
}
