package com.ctu.freshgrocery.admin;

import com.ctu.freshgrocery.dto.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class AdminSecurityConfig {
    @Bean PasswordEncoder adminPasswordEncoder() { return new BCryptPasswordEncoder(12); }
    @Bean UserDetailsService adminUsers(AdminCredentials credentials) {
        return new InMemoryUserDetailsManager(User.withUsername(credentials.username())
                .password(credentials.passwordHash()).roles("ADMIN").build());
    }

    static boolean protectedOperation(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        if (path.startsWith("/api/v1/admin/")) return true;
        if (path.startsWith("/api/v1/benchmark") || path.startsWith("/api/v1/ai/forecast")) return true;
        if (path.equals("/api/v1/orders/concurrency-test")) return true;
        if (path.equals("/api/v1/search/sync")) return true;
        return !request.getMethod().equals("GET") && (path.startsWith("/api/v1/products")
                || path.startsWith("/api/v1/categories") || path.startsWith("/api/v1/promotions"));
    }

    @Bean SecurityFilterChain adminSecurity(HttpSecurity http, ObjectMapper mapper) throws Exception {
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/admin/auth/session", "/api/v1/admin/auth/login").permitAll()
                .requestMatchers("/api/v1/admin/**", "/api/v1/benchmark/**", "/api/v1/ai/forecast/**", "/api/v1/alerts/**").hasRole("ADMIN")
                .requestMatchers("/api/v1/search/sync", "/api/v1/orders/concurrency-test").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/v1/products/**", "/api/v1/categories/**", "/api/v1/promotions/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/v1/products/**", "/api/v1/categories/**", "/api/v1/promotions/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PATCH, "/api/v1/products/**", "/api/v1/categories/**", "/api/v1/promotions/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/v1/products/**", "/api/v1/categories/**", "/api/v1/promotions/**").hasRole("ADMIN")
                .anyRequest().permitAll());
        http.csrf(csrf -> csrf.requireCsrfProtectionMatcher(request -> protectedOperation(request)
                && (!java.util.Set.of("GET", "HEAD", "OPTIONS").contains(request.getMethod())
                    || request.getRequestURI().equals("/api/v1/benchmark/compare")
                    || request.getRequestURI().startsWith("/api/v1/ai/forecast/product/"))));
        http.requestCache(cache -> cache.disable());
        http.formLogin(form -> form.loginProcessingUrl("/api/v1/admin/auth/login")
                .successHandler((request, response, authentication) -> {
                    response.setContentType("application/json;charset=UTF-8");
                    mapper.writeValue(response.getWriter(), ApiResponse.ok("Đăng nhập thành công", java.util.Map.of("username", authentication.getName())));
                })
                .failureHandler((request, response, error) -> {
                    response.setStatus(401); response.setContentType("application/json;charset=UTF-8");
                    mapper.writeValue(response.getWriter(), ApiResponse.error(401, "Tên đăng nhập hoặc mật khẩu không đúng."));
                }));
        http.logout(logout -> logout.logoutUrl("/api/v1/admin/auth/logout").invalidateHttpSession(true)
                .deleteCookies("JSESSIONID").logoutSuccessHandler((request, response, auth) -> {
                    response.setContentType("application/json;charset=UTF-8");
                    mapper.writeValue(response.getWriter(), ApiResponse.ok("Đã đăng xuất", true));
                }));
        http.exceptionHandling(errors -> errors
                .authenticationEntryPoint((request, response, error) -> {
                    response.setStatus(401); response.setContentType("application/json;charset=UTF-8");
                    mapper.writeValue(response.getWriter(), ApiResponse.error(401, "Vui lòng đăng nhập quản trị."));
                })
                .accessDeniedHandler((request, response, error) -> {
                    response.setStatus(403); response.setContentType("application/json;charset=UTF-8");
                    mapper.writeValue(response.getWriter(), ApiResponse.error(403, "Không có quyền hoặc phiên xác thực đã thay đổi. Vui lòng tải lại trang."));
                }));
        http.sessionManagement(session -> session.sessionFixation(fixation -> fixation.changeSessionId()));
        return http.build();
    }
}
