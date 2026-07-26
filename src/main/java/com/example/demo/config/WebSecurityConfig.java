package com.example.demo.config;

import com.example.demo.filter.JwtAuthenticationFilter;
import com.example.demo.models.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@EnableMethodSecurity
public class WebSecurityConfig {

    private final String[] PUBLIC_ENPOINTS = {"/api/v1/users/register", "/api/v1/users/login"};
    private final String[] PUBLIC_GET_ENPOINTS = {"/api/v1/categories/**", "/api/v1/products/**", "/api/v1/orders/**"};
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                // START: sử dụng cho những endpoint không sử dụng @PreAuthorize("hasRole('XXX')")
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(new JwtAuthenticationEntryPoint())// chỉ sử dụng duy nhất ở đây nên khong cần phải @Bean, không cần đưa vao Application Context
                        .accessDeniedHandler(new JwtAccessDeniedHandler()))
                // END
                .authorizeHttpRequests(request -> request
                        .requestMatchers(PUBLIC_ENPOINTS).permitAll()
                        .requestMatchers(HttpMethod.GET, PUBLIC_GET_ENPOINTS).permitAll()

                        .requestMatchers(HttpMethod.POST, "/api/v1/products/**").hasRole(Role.ADMIN)
                        .requestMatchers(HttpMethod.PUT, "/api/v1/products/**").hasRole(Role.ADMIN)
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/products/**").hasRole(Role.ADMIN)

                        .requestMatchers(HttpMethod.POST, "/api/v1/categories/**").hasRole(Role.ADMIN)
                        .requestMatchers(HttpMethod.PUT, "/api/v1/categories/**").hasRole(Role.ADMIN)
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/categories/**").hasRole(Role.ADMIN)

                        .requestMatchers(HttpMethod.POST, "/api/v1/orders/**").hasRole(Role.USER)
                        .requestMatchers(HttpMethod.PUT, "/api/v1/orders/**").hasRole(Role.ADMIN)
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/orders/**").hasRole(Role.ADMIN)

                        .requestMatchers(HttpMethod.POST, "/api/v1/order_details/**").hasRole(Role.USER)
                        .requestMatchers(HttpMethod.GET, "/api/v1/order_details/**").hasAnyRole(Role.USER, Role.ADMIN)
                        .requestMatchers(HttpMethod.PUT, "/api/v1/order_details/**").hasRole(Role.ADMIN)
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/order_details/**").hasRole(Role.ADMIN)
//
//                        .requestMatchers(HttpMethod.GET, "/api/v1/users/**").hasRole(Role.ADMIN)
                        .anyRequest()
                        .authenticated());
        return http.build();
    }

//    @Bean
//    public AuthenticationEntryPoint unauthorizedEntryPoint() {
//        return (request, response, authException) ->
//                response.sendError(HttpStatus.UNAUTHORIZED.value(), "Unauthorized");
//    }
//
//    @Bean
//    public AccessDeniedHandler accessDeniedHandler() {
//        return (request, response, accessDeniedException) ->
//                response.sendError(HttpStatus.FORBIDDEN.value(), "Forbidden");
//    }
}
