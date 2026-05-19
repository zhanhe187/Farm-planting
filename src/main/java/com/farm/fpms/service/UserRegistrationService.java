package com.farm.fpms.service;

import com.farm.fpms.common.BusinessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class UserRegistrationService {

    private final JdbcTemplate jdbcTemplate;

    public UserRegistrationService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void register(String username, String password, String displayName) {
        String cleanUsername = trim(username);
        String cleanPassword = trim(password);
        String cleanDisplayName = trim(displayName);
        if (cleanUsername.isEmpty() || cleanPassword.isEmpty()) {
            throw new BusinessException("账号和密码不能为空");
        }
        if (cleanDisplayName.isEmpty()) {
            cleanDisplayName = cleanUsername;
        }

        Integer count = jdbcTemplate.queryForObject(
                "select count(1) from sys_user where username = ?", Integer.class, cleanUsername);
        if (count != null && count > 0) {
            throw new BusinessException("账号已存在，请换一个账号");
        }

        jdbcTemplate.update(
                "insert into sys_user(username, password, display_name, role_code, data_scope, enabled) values (?, ?, ?, ?, ?, 1)",
                cleanUsername, cleanPassword, cleanDisplayName, "CUSTOMER", "OWN_CUSTOMER");
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
