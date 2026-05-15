package com.farm.fpms.web;

import com.farm.fpms.service.PasswordService;
import com.farm.fpms.service.UserRegistrationService;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.ui.ExtendedModelMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class AuthControllerRegistrationTest {

    @Test
    void registerRejectsMismatchedPasswordConfirmation() {
        UserRegistrationService registrationService = mock(UserRegistrationService.class);
        AuthController controller = new AuthController(mock(JdbcTemplate.class), new PasswordService(), registrationService);
        ExtendedModelMap model = new ExtendedModelMap();

        String view = controller.doRegister("newowner", "新农场主", "123456", "654321", model);

        assertThat(view).isEqualTo("register");
        assertThat(model.get("error")).isEqualTo("两次输入的密码不一致");
        verify(registrationService, never()).register("newowner", "123456", "新农场主");
    }

    @Test
    void registerCreatesUserAndReturnsToLogin() {
        UserRegistrationService registrationService = mock(UserRegistrationService.class);
        AuthController controller = new AuthController(mock(JdbcTemplate.class), new PasswordService(), registrationService);
        ExtendedModelMap model = new ExtendedModelMap();

        String view = controller.doRegister("newowner", "新农场主", "123456", "123456", model);

        assertThat(view).isEqualTo("redirect:/login?registered");
        verify(registrationService).register("newowner", "123456", "新农场主");
    }
}
