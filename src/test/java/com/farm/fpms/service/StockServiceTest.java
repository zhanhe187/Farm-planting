package com.farm.fpms.service;

import com.farm.fpms.domain.BusinessException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
}
