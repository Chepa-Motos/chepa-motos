package com.chepamotos.infrastructure.config;

import com.chepamotos.infrastructure.security.ApiAccessDeniedHandler;
import com.chepamotos.infrastructure.security.ApiAuthenticationEntryPoint;
import com.chepamotos.infrastructure.security.JwtAuthenticationFilter;
import com.chepamotos.infrastructure.security.JwtTokenDecoder;
import com.chepamotos.domain.port.AppUserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.time.Clock;

@Configuration
public class SecurityConfig {

        @Bean
        public JwtAuthenticationFilter jwtAuthenticationFilter(JwtTokenDecoder jwtTokenDecoder,
                                                                                                                   AppUserRepository appUserRepository) {
                return new JwtAuthenticationFilter(jwtTokenDecoder, appUserRepository);
        }

        @Bean
        public ApiAuthenticationEntryPoint apiAuthenticationEntryPoint(Clock clock) {
                return new ApiAuthenticationEntryPoint(clock);
        }

        @Bean
        public ApiAccessDeniedHandler apiAccessDeniedHandler(Clock clock) {
                return new ApiAccessDeniedHandler(clock);
        }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtAuthenticationFilter jwtAuthenticationFilter,
                                                   ApiAuthenticationEntryPoint authenticationEntryPoint,
                                                   ApiAccessDeniedHandler accessDeniedHandler) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/mechanics", "/api/mechanics/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/vehicles/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/invoice-items/**").permitAll()
                        .requestMatchers("/api/invoices/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/liquidations").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/mechanics").hasRole("GERENTE")
                        .requestMatchers(HttpMethod.PATCH, "/api/mechanics/*/status").hasRole("GERENTE")
                        .requestMatchers(HttpMethod.POST, "/api/liquidations").hasRole("GERENTE")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
