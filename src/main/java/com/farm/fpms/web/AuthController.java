package com.farm.fpms.web;

import com.farm.fpms.domain.BusinessException;
import com.farm.fpms.service.PasswordService;
import com.farm.fpms.service.UserRegistrationService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;

@Controller
public class AuthController {

    private final JdbcTemplate jdbcTemplate;
    private final PasswordService passwordService;
    private final UserRegistrationService userRegistrationService;

    public AuthController(JdbcTemplate jdbcTemplate, PasswordService passwordService,
                          UserRegistrationService userRegistrationService) {
        this.jdbcTemplate = jdbcTemplate;
        this.passwordService = passwordService;
        this.userRegistrationService = userRegistrationService;
    }

    @GetMapping("/login")
    public String login(Model model) {
        return "login";
    }

    @PostMapping("/login")
    public String doLogin(@RequestParam String username, @RequestParam String password,
                          HttpSession session, Model model) {
        List<Map<String, Object>> users = jdbcTemplate.queryForList(
                "select * from sys_user where username = ? and enabled = 1", username);
        if (users.isEmpty() || !passwordService.matches(password, String.valueOf(users.get(0).get("password")))) {
            model.addAttribute("error", "账号或密码不正确");
            return "login";
        }
        Map<String, Object> user = users.get(0);
        session.setAttribute(SessionUser.SESSION_KEY, new SessionUser(
                ((Number) user.get("id")).longValue(),
                String.valueOf(user.get("username")),
                String.valueOf(user.get("display_name")),
                String.valueOf(user.get("role_code")),
                String.valueOf(user.get("data_scope"))));
        return "redirect:/dashboard";
    }

    @GetMapping("/register")
    public String register() {
        return "register";
    }

    @PostMapping("/register")
    public String doRegister(@RequestParam String username,
                             @RequestParam(required = false) String displayName,
                             @RequestParam String password,
                             @RequestParam String confirmPassword,
                             Model model) {
        if (!String.valueOf(password).equals(confirmPassword)) {
            model.addAttribute("error", "两次输入的密码不一致");
            return "register";
        }
        try {
            userRegistrationService.register(username, password, displayName);
        } catch (BusinessException ex) {
            model.addAttribute("error", ex.getMessage());
            return "register";
        }
        return "redirect:/login?registered";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }
}
