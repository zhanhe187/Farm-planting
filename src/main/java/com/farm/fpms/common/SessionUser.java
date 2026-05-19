package com.farm.fpms.common;

public class SessionUser {

    public static final String SESSION_KEY = "FPMS_USER";

    private final Long id;
    private final String username;
    private final String displayName;
    private final String roleCode;
    private final String dataScope;

    public SessionUser(Long id, String username, String displayName, String roleCode, String dataScope) {
        this.id = id;
        this.username = username;
        this.displayName = displayName;
        this.roleCode = roleCode;
        this.dataScope = dataScope;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getRoleCode() {
        return roleCode;
    }

    public String getDataScope() {
        return dataScope;
    }

    public boolean isAdmin() {
        return "SUPER_ADMIN".equals(roleCode);
    }
}
