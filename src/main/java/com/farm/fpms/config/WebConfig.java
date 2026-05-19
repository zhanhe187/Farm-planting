package com.farm.fpms.config;

import com.farm.fpms.common.RoleAccessPolicy;
import com.farm.fpms.common.SessionUser;
import com.farm.fpms.common.StatusLabel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final RoleAccessPolicy roleAccessPolicy = new RoleAccessPolicy();

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SessionInterceptor(roleAccessPolicy))
                .addPathPatterns("/**")
                .excludePathPatterns("/login", "/register", "/logout", "/trace/**", "/m/**", "/css/**", "/js/**",
                        "/images/**", "/webjars/**", "/error");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/css/**").addResourceLocations("classpath:/static/css/");
        registry.addResourceHandler("/js/**").addResourceLocations("classpath:/static/js/");
    }

    private static class SessionInterceptor implements HandlerInterceptor {
        private final RoleAccessPolicy roleAccessPolicy;

        private SessionInterceptor(RoleAccessPolicy roleAccessPolicy) {
            this.roleAccessPolicy = roleAccessPolicy;
        }

        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
            if (request.getRequestURI().equals("/")) {
                Object user = request.getSession().getAttribute(SessionUser.SESSION_KEY);
                response.sendRedirect(user instanceof SessionUser ? roleAccessPolicy.defaultPath((SessionUser) user) : "/login");
                return false;
            }
            SessionUser user = (SessionUser) request.getSession().getAttribute(SessionUser.SESSION_KEY);
            if (user == null) {
                response.sendRedirect("/login");
                return false;
            }
            if (!roleAccessPolicy.canAccess(user, request.getMethod(), request.getRequestURI())) {
                response.sendRedirect(roleAccessPolicy.defaultPath(user));
                return false;
            }
            return true;
        }

        @Override
        public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
                               ModelAndView modelAndView) {
            if (modelAndView != null) {
                modelAndView.addObject("currentUser", request.getSession().getAttribute(SessionUser.SESSION_KEY));
                modelAndView.addObject("label", new StatusLabel());
                modelAndView.addObject("access", roleAccessPolicy);
            }
        }
    }
}
