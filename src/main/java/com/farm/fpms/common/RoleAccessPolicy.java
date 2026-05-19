package com.farm.fpms.common;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class RoleAccessPolicy {

    private static final Set<String> MANAGERS = roles("SUPER_ADMIN", "FARM_OWNER");
    private static final Set<String> PRODUCTION = roles("SUPER_ADMIN", "FARM_OWNER", "AGRI_TECH");
    private static final Set<String> OPERATION_USERS = roles("SUPER_ADMIN", "FARM_OWNER", "AGRI_TECH", "FIELD_WORKER");
    private static final Set<String> STOCK_USERS = roles("SUPER_ADMIN", "FARM_OWNER", "WAREHOUSE");
    private static final Set<String> MOBILE_USERS = roles("SUPER_ADMIN", "FARM_OWNER", "AGRI_TECH", "FIELD_WORKER");
    private static final Set<String> AI_USERS = roles("SUPER_ADMIN", "FARM_OWNER", "AGRI_TECH");

    public boolean canAccess(SessionUser user, String method, String uri) {
        if (user == null) {
            return false;
        }
        String role = user.getRoleCode();
        if ("SUPER_ADMIN".equals(role)) {
            return true;
        }
        String normalizedUri = normalizeUri(uri);
        String normalizedMethod = method == null ? "GET" : method.toUpperCase();

        if (startsWith(normalizedUri, "/providers") || startsWith(normalizedUri, "/admin")) {
            return false;
        }
        if (startsWith(normalizedUri, "/dashboard") || startsWith(normalizedUri, "/plots")) {
            return PRODUCTION.contains(role);
        }
        if (startsWith(normalizedUri, "/batches")) {
            return PRODUCTION.contains(role);
        }
        if (startsWith(normalizedUri, "/crops")) {
            return PRODUCTION.contains(role)
                    || ("WAREHOUSE".equals(role) && "POST".equals(normalizedMethod) && "/crops".equals(normalizedUri));
        }
        if (startsWith(normalizedUri, "/operations")) {
            return OPERATION_USERS.contains(role);
        }
        if (startsWith(normalizedUri, "/stock")) {
            return STOCK_USERS.contains(role);
        }
        if (startsWith(normalizedUri, "/harvest")) {
            return PRODUCTION.contains(role);
        }
        if (startsWith(normalizedUri, "/sales")) {
            return MANAGERS.contains(role) || ("CUSTOMER".equals(role) && "GET".equals(normalizedMethod));
        }
        if (startsWith(normalizedUri, "/market")) {
            return "CUSTOMER".equals(role);
        }
        if (startsWith(normalizedUri, "/ai/chat") || startsWith(normalizedUri, "/ai/stream")) {
            return AI_USERS.contains(role);
        }
        if (startsWith(normalizedUri, "/mobile")) {
            return MOBILE_USERS.contains(role);
        }
        return false;
    }

    public String defaultPath(SessionUser user) {
        if (user == null) {
            return "/login";
        }
        String role = user.getRoleCode();
        if ("WAREHOUSE".equals(role)) {
            return "/stock";
        }
        if ("FIELD_WORKER".equals(role)) {
            return "/operations";
        }
        if ("CUSTOMER".equals(role)) {
            return "/market";
        }
        return "/dashboard";
    }

    public boolean canAccess(SessionUser user, String uri) {
        return canAccess(user, "GET", uri);
    }

    public boolean canCreateSales(SessionUser user) {
        return user != null && MANAGERS.contains(user.getRoleCode());
    }

    public boolean canManageAiProviders(SessionUser user) {
        return user != null && "SUPER_ADMIN".equals(user.getRoleCode());
    }

    private String normalizeUri(String uri) {
        if (uri == null || uri.isEmpty()) {
            return "/";
        }
        return uri;
    }

    private boolean startsWith(String uri, String prefix) {
        return uri.equals(prefix) || uri.startsWith(prefix + "/");
    }

    private static Set<String> roles(String... roles) {
        return new HashSet<String>(Arrays.asList(roles));
    }
}
