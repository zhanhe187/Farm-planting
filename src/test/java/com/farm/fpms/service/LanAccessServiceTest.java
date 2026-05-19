package com.farm.fpms.service;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.net.InetAddress;

import static org.assertj.core.api.Assertions.assertThat;

class LanAccessServiceTest {

    @Test
    void usesConfiguredNonLocalhostBindUrlFirst() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setScheme("http");
        request.setServerName("localhost");
        request.setServerPort(18080);
        LanAccessService service = new LanAccessService(() -> address("192.168.5.93"));

        String url = service.bindUrl(request, "http://192.168.1.8:18080/m/bind");

        assertThat(url).isEqualTo("http://192.168.1.8:18080/m/bind");
    }

    @Test
    void replacesLocalhostRequestWithLanAddress() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setScheme("http");
        request.setServerName("localhost");
        request.setServerPort(18080);
        LanAccessService service = new LanAccessService(() -> address("192.168.5.93"));

        String url = service.bindUrl(request, "http://localhost:8080/m/bind");

        assertThat(url).isEqualTo("http://192.168.5.93:18080/m/bind");
    }

    @Test
    void keepsRequestHostWhenItIsAlreadyReachable() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setScheme("http");
        request.setServerName("192.168.5.93");
        request.setServerPort(18080);
        LanAccessService service = new LanAccessService(() -> address("192.168.1.88"));

        String url = service.bindUrl(request, "");

        assertThat(url).isEqualTo("http://192.168.5.93:18080/m/bind");
    }

    @Test
    void buildsTraceUrlWithLanAddressWhenRequestUsesLocalhost() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setScheme("http");
        request.setServerName("localhost");
        request.setServerPort(8080);
        LanAccessService service = new LanAccessService(() -> address("192.168.5.93"));

        String url = service.traceUrl(request, "http://localhost:8080/trace/", "F000001-20260518-2");

        assertThat(url).isEqualTo("http://192.168.5.93:8080/trace/F000001-20260518-2");
    }

    @Test
    void usesConfiguredReachableTraceBaseUrlFirst() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setScheme("http");
        request.setServerName("localhost");
        request.setServerPort(8080);
        LanAccessService service = new LanAccessService(() -> address("192.168.5.93"));

        String url = service.traceUrl(request, "https://farm.example.com/trace", "TRACE-001");

        assertThat(url).isEqualTo("https://farm.example.com/trace/TRACE-001");
    }

    private InetAddress address(String value) {
        try {
            return InetAddress.getByName(value);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
}
