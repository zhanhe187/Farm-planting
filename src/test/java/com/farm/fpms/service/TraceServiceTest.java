package com.farm.fpms.service;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TraceServiceTest {

    @Test
    void publicTraceAddsUppercaseAliasesForSqlServerMapKeys() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        Map<String, Object> row = new HashMap<String, Object>();
        row.put("batch_id", 2L);
        row.put("batch_no", "2026春-黄瓜-2号");
        row.put("crop_name", "黄瓜");
        row.put("variety", "津优 35");
        row.put("plot_name", "B2 露天地");
        row.put("soil_type", "沙壤土");
        row.put("harvest_date", Date.valueOf(LocalDate.of(2026, 5, 18)));
        row.put("quantity_kg", new BigDecimal("1280.00"));
        row.put("image_url", "https://example.com/cucumber.jpg");

        when(jdbcTemplate.queryForList(contains("from harvest_record"), eq("TRACE-001")))
                .thenReturn(Collections.singletonList(row));
        when(jdbcTemplate.queryForList(contains("from plant_operation"), eq(2L)))
                .thenReturn(Collections.<Map<String, Object>>emptyList());
        when(jdbcTemplate.queryForList(anyString()))
                .thenReturn(Collections.<Map<String, Object>>emptyList());

        TraceService service = new TraceService(jdbcTemplate, mock(QrCodeService.class), mock(LanAccessService.class));

        Map<String, Object> trace = service.publicTrace("TRACE-001");

        assertThat(trace.get("CROP_NAME")).isEqualTo("黄瓜");
        assertThat(trace.get("BATCH_NO")).isEqualTo("2026春-黄瓜-2号");
        assertThat(trace.get("PLOT_NAME")).isEqualTo("B2 露天地");
        assertThat(trace.get("QUANTITY_KG")).isEqualTo(new BigDecimal("1280.00"));
        assertThat(trace.get("IMAGE_URL")).isEqualTo("https://example.com/cucumber.jpg");
    }

    @Test
    void generateQrDataUriUsesLanReachableTraceUrl() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        QrCodeService qrCodeService = mock(QrCodeService.class);
        LanAccessService lanAccessService = mock(LanAccessService.class);
        MockHttpServletRequest request = new MockHttpServletRequest();
        TraceService service = new TraceService(jdbcTemplate, qrCodeService, lanAccessService);
        ReflectionTestUtils.setField(service, "traceBaseUrl", "http://localhost:8080/trace/");

        when(lanAccessService.traceUrl(request, "http://localhost:8080/trace/", "TRACE-001"))
                .thenReturn("http://192.168.5.93:8080/trace/TRACE-001");
        when(qrCodeService.toDataUri("http://192.168.5.93:8080/trace/TRACE-001"))
                .thenReturn("data:image/png;base64,qr");

        String dataUri = service.generateQrDataUri(request, "TRACE-001");

        assertThat(dataUri).isEqualTo("data:image/png;base64,qr");
        verify(qrCodeService).toDataUri("http://192.168.5.93:8080/trace/TRACE-001");
    }
}
