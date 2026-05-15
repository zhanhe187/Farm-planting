package com.farm.fpms.service;

import com.farm.fpms.domain.BusinessException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BindTokenServiceTest {

    @Test
    void tokenCanOnlyBeUsedOnce() {
        BindTokenService service = new BindTokenService(Clock.fixed(Instant.parse("2026-05-14T10:00:00Z"), ZoneId.of("UTC")));
        String token = service.issueToken("pc-session-1", 300);

        assertThat(service.consume(token).getPcSessionId()).isEqualTo("pc-session-1");

        assertThatThrownBy(() -> service.consume(token))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已使用");
    }

    @Test
    void consumedTokenCanCreateMobileSessionBinding() {
        BindTokenService service = new BindTokenService(Clock.fixed(Instant.parse("2026-05-14T10:00:00Z"), ZoneId.of("UTC")));
        String token = service.issueToken("pc-session-1", 300);

        BindToken bindToken = service.consume(token, "192.168.1.10", "Mobile Safari");

        assertThat(bindToken.getPcSessionId()).isEqualTo("pc-session-1");
        assertThat(bindToken.getUsedIp()).isEqualTo("192.168.1.10");
        assertThat(bindToken.getUsedUa()).isEqualTo("Mobile Safari");
    }
}
