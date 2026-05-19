package com.farm.fpms.service;

import com.farm.fpms.common.BusinessException;
import com.farm.fpms.entity.BindToken;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings({"rawtypes", "unchecked"})
class BindTokenServiceTest {

    @Test
    void issuingTokenPersistsItToSqlServer() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        Clock clock = Clock.fixed(Instant.parse("2026-05-14T10:00:00Z"), ZoneId.of("UTC"));
        BindTokenService service = new BindTokenService(jdbcTemplate, clock);

        String token = service.issueToken("pc-session-1", 300);

        assertThat(token).hasSize(32);
        verify(jdbcTemplate).update(
                "insert into ai_bind_token(token, pc_session_id, expires_at, used) values (?, ?, ?, 0)",
                token, "pc-session-1", Timestamp.from(Instant.parse("2026-05-14T10:05:00Z")));
    }

    @Test
    void consumedTokenCanCreateMobileSessionBinding() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        Clock clock = Clock.fixed(Instant.parse("2026-05-14T10:00:00Z"), ZoneId.of("UTC"));
        BindTokenService service = new BindTokenService(jdbcTemplate, clock);
        when(jdbcTemplate.query(eq(BindTokenService.SELECT_BY_TOKEN), any(RowMapper.class), eq("token-1")))
                .thenReturn(Collections.singletonList(new BindToken("token-1", "pc-session-1",
                        Instant.parse("2026-05-14T10:05:00Z"))));
        when(jdbcTemplate.update(BindTokenService.CONSUME_SQL, "192.168.1.10", "Mobile Safari", "token-1",
                Timestamp.from(Instant.parse("2026-05-14T10:00:00Z")))).thenReturn(1);

        BindToken bindToken = service.consume("token-1", "192.168.1.10", "Mobile Safari");

        assertThat(bindToken.getPcSessionId()).isEqualTo("pc-session-1");
        assertThat(bindToken.getUsedIp()).isEqualTo("192.168.1.10");
        assertThat(bindToken.getUsedUa()).isEqualTo("Mobile Safari");
    }

    @Test
    void longMobileUserAgentIsTrimmedBeforeSqlServerUpdate() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        Clock clock = Clock.fixed(Instant.parse("2026-05-14T10:00:00Z"), ZoneId.of("UTC"));
        BindTokenService service = new BindTokenService(jdbcTemplate, clock);
        when(jdbcTemplate.query(eq(BindTokenService.SELECT_BY_TOKEN), any(RowMapper.class), eq("token-1")))
                .thenReturn(Collections.singletonList(new BindToken("token-1", "pc-session-1",
                        Instant.parse("2026-05-14T10:05:00Z"))));
        when(jdbcTemplate.update(eq(BindTokenService.CONSUME_SQL), eq("192.168.1.10"), any(String.class), eq("token-1"),
                eq(Timestamp.from(Instant.parse("2026-05-14T10:00:00Z"))))).thenReturn(1);
        String longUserAgent = repeat("Mozilla/5.0 (Linux; Android 16; V2339A Build/BP2A.250605.031.A3; wv) ", 30);

        BindToken bindToken = service.consume("token-1", "192.168.1.10", longUserAgent);

        assertThat(bindToken.getUsedUa()).hasSize(1000);
        verify(jdbcTemplate).update(eq(BindTokenService.CONSUME_SQL), eq("192.168.1.10"),
                org.mockito.ArgumentMatchers.<String>argThat(value -> value != null && value.length() == 1000),
                eq("token-1"), eq(Timestamp.from(Instant.parse("2026-05-14T10:00:00Z"))));
    }

    @Test
    void tokenCanOnlyBeUsedOnce() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        BindTokenService service = new BindTokenService(jdbcTemplate,
                Clock.fixed(Instant.parse("2026-05-14T10:00:00Z"), ZoneId.of("UTC")));
        BindToken used = new BindToken("token-1", "pc-session-1", Instant.parse("2026-05-14T10:05:00Z"));
        used.markUsed("192.168.1.10", "Mobile Safari");
        when(jdbcTemplate.query(eq(BindTokenService.SELECT_BY_TOKEN), any(RowMapper.class), eq("token-1")))
                .thenReturn(Collections.singletonList(used));

        assertThatThrownBy(() -> service.consume("token-1"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已使用");
    }

    @Test
    void missingTokenIsRejected() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        BindTokenService service = new BindTokenService(jdbcTemplate,
                Clock.fixed(Instant.parse("2026-05-14T10:00:00Z"), ZoneId.of("UTC")));
        when(jdbcTemplate.query(eq(BindTokenService.SELECT_BY_TOKEN), any(RowMapper.class), eq("missing")))
                .thenReturn(Collections.<BindToken>emptyList());

        assertThatThrownBy(() -> service.consume("missing"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不存在");
    }

    private String repeat(String value, int times) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < times; i++) {
            builder.append(value);
        }
        return builder.toString();
    }
}
