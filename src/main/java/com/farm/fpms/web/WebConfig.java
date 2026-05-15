package com.farm.fpms.web;

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
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SessionInterceptor())
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
        private static final Set<String> ADMIN_PATHS = new HashSet<>(Arrays.asList(
                "/providers", "/admin"));

        private boolean isAdminPath(String uri) {
            for (String prefix : ADMIN_PATHS) {
                if (uri.startsWith(prefix)) return true;
            }
            return false;
        }

        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
            if (request.getRequestURI().equals("/")) {
                Object user = request.getSession().getAttribute(SessionUser.SESSION_KEY);
                response.sendRedirect(user == null ? "/login" : "/dashboard");
                return false;
            }
            SessionUser user = (SessionUser) request.getSession().getAttribute(SessionUser.SESSION_KEY);
            if (user == null) {
                response.sendRedirect("/login");
                return false;
            }
            if (isAdminPath(request.getRequestURI()) && !user.isAdmin()) {
                response.sendRedirect("/dashboard");
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
            }
        }
    }
}
