package com.farm.fpms.service;

import com.farm.fpms.entity.BatchStatusMachine;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.support.GeneratedKeyHolder;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BatchServiceTest {

    @Test
    void createBatchStartsWithChinesePlannedStatusAndReturnsGeneratedId() throws Exception {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        AtomicBoolean usedKeyHolder = new AtomicBoolean(false);
        when(jdbcTemplate.update(any(), any(GeneratedKeyHolder.class))).thenAnswer(invocation -> {
            GeneratedKeyHolder keyHolder = invocation.getArgument(1);
            keyHolder.getKeyList().add(Collections.singletonMap("id", 88L));
            usedKeyHolder.set(true);
            return 1;
        });

        BatchService service = new BatchService(jdbcTemplate, new BatchStatusMachine());

        long id = service.createBatch("2026冬-生菜-1号", 1L, 5L, 6.5,
                null, LocalDate.of(2026, 12, 20), 2L);

        assertThat(id).isEqualTo(88L);
        assertThat(usedKeyHolder).isTrue();

        org.mockito.ArgumentCaptor<PreparedStatementCreator> creatorCaptor =
                org.mockito.ArgumentCaptor.forClass(PreparedStatementCreator.class);
        verify(jdbcTemplate).update(creatorCaptor.capture(), any(GeneratedKeyHolder.class));
        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        when(connection.prepareStatement(anyString(), eq(Statement.RETURN_GENERATED_KEYS))).thenReturn(statement);
        creatorCaptor.getValue().createPreparedStatement(connection);
        verify(statement).setString(4, "已计划");
    }
}
