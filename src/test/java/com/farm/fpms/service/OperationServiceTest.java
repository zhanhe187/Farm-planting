package com.farm.fpms.service;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;

import java.time.LocalDate;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OperationServiceTest {

    @Test
    void createOperationReturnsGeneratedKeyWithoutMaxIdQuery() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        StockService stockService = mock(StockService.class);
        AtomicBoolean usedKeyHolder = new AtomicBoolean(false);

        when(jdbcTemplate.update(any(), any(GeneratedKeyHolder.class))).thenAnswer(invocation -> {
            GeneratedKeyHolder keyHolder = invocation.getArgument(1);
            keyHolder.getKeyList().add(Collections.singletonMap("id", 42L));
            usedKeyHolder.set(true);
            return 1;
        });

        OperationService service = new OperationService(jdbcTemplate, stockService);

        long id = service.createOperation(1L, "FERTILIZE", LocalDate.of(2026, 5, 14),
                "worker", "note", null, null);

        assertThat(id).isEqualTo(42L);
        assertThat(usedKeyHolder).isTrue();
        verify(jdbcTemplate, never()).queryForObject(eq("select max(id) from plant_operation"), eq(Long.class));
    }
}
