package com.farm.fpms.service;

import com.farm.fpms.common.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

class UserRegistrationServiceTest {

    @Test
    void registersCustomerUserWithOwnCustomerScope() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForObject("select count(1) from sys_user where username = ?", Integer.class, "newowner"))
                .thenReturn(0);
        UserRegistrationService service = new UserRegistrationService(jdbcTemplate);

        service.register(" newowner ", "123456", "  新农场主  ");

        verify(jdbcTemplate).update(
                "insert into sys_user(username, password, display_name, role_code, data_scope, enabled) values (?, ?, ?, ?, ?, 1)",
                "newowner", "123456", "新农场主", "CUSTOMER", "OWN_CUSTOMER");
    }

    @Test
    void rejectsDuplicateUsername() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForObject("select count(1) from sys_user where username = ?", Integer.class, "owner"))
                .thenReturn(1);
        UserRegistrationService service = new UserRegistrationService(jdbcTemplate);

        assertThatThrownBy(() -> service.register("owner", "123456", "任意昵称"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("账号已存在，请换一个账号");
    }
}
