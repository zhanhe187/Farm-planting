package com.farm.fpms.service;

import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.function.Supplier;

@Service
public class LanAccessService {

    private final Supplier<InetAddress> lanAddressSupplier;

    public LanAccessService() {
        this(LanAccessService::detectLanAddress);
    }

    LanAccessService(Supplier<InetAddress> lanAddressSupplier) {
        this.lanAddressSupplier = lanAddressSupplier;
    }

    public String bindUrl(HttpServletRequest request, String configuredUrl) {
        if (isConfiguredReachable(configuredUrl)) {
            return trim(configuredUrl);
        }
        return requestBaseUrl(request) + "/m/bind";
    }

    public String traceUrl(HttpServletRequest request, String configuredBaseUrl, String traceCode) {
        String baseUrl = isConfiguredReachable(configuredBaseUrl)
                ? trim(configuredBaseUrl)
                : requestBaseUrl(request) + "/trace/";
        if (!baseUrl.endsWith("/")) {
            baseUrl = baseUrl + "/";
        }
        return baseUrl + trim(traceCode);
    }

    private String requestBaseUrl(HttpServletRequest request) {
        String host = request.getServerName();
        if (isLocalhost(host)) {
            InetAddress lanAddress = lanAddressSupplier.get();
            if (lanAddress != null) {
                host = lanAddress.getHostAddress();
            }
        }
        StringBuilder base = new StringBuilder();
        base.append(request.getScheme()).append("://").append(host);
        int port = request.getServerPort();
        if (port != 80 && port != 443) {
            base.append(":").append(port);
        }
        return base.toString();
    }

    private boolean isConfiguredReachable(String configuredUrl) {
        String value = trim(configuredUrl).toLowerCase();
        return !value.isEmpty() && !value.contains("localhost") && !value.contains("127.0.0.1");
    }

    private boolean isLocalhost(String host) {
        String value = trim(host).toLowerCase();
        return value.isEmpty() || "localhost".equals(value) || "127.0.0.1".equals(value) || "::1".equals(value);
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private static InetAddress detectLanAddress() {
        try {
            InetAddress best = null;
            int bestScore = -1;
            for (NetworkInterface networkInterface : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (!networkInterface.isUp() || networkInterface.isLoopback() || networkInterface.isVirtual()) {
                    continue;
                }
                String interfaceName = (networkInterface.getName() + " " + networkInterface.getDisplayName()).toLowerCase();
                if (interfaceName.contains("vmware") || interfaceName.contains("vmnet")
                        || interfaceName.contains("virtual") || interfaceName.contains("vbox")
                        || interfaceName.contains("hyper-v") || interfaceName.contains("wsl")
                        || interfaceName.contains("docker")) {
                    continue;
                }
                for (InetAddress address : Collections.list(networkInterface.getInetAddresses())) {
                    if (address instanceof Inet4Address && !address.isLoopbackAddress() && !address.isLinkLocalAddress()) {
                        int score = scoreInterface(interfaceName);
                        if (score > bestScore) {
                            best = address;
                            bestScore = score;
                        }
                    }
                }
            }
            return best;
        } catch (Exception ignored) {}
        return null;
    }

    private static int scoreInterface(String interfaceName) {
        if (interfaceName.contains("wlan") || interfaceName.contains("wi-fi") || interfaceName.contains("wifi")) {
            return 100;
        }
        if (interfaceName.contains("ethernet") || interfaceName.contains("以太网")) {
            return 80;
        }
        return 10;
    }
}
