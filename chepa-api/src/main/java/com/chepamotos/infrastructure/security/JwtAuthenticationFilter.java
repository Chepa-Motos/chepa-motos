package com.chepamotos.infrastructure.security;

import com.chepamotos.domain.port.AppUserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenDecoder jwtTokenDecoder;
    private final AppUserRepository appUserRepository;

    public JwtAuthenticationFilter(JwtTokenDecoder jwtTokenDecoder, AppUserRepository appUserRepository) {
        this.jwtTokenDecoder = jwtTokenDecoder;
        this.appUserRepository = appUserRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authorization.substring(BEARER_PREFIX.length()).trim();
        if (token.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String username = jwtTokenDecoder.extractUsername(token);
            appUserRepository.findByUsername(username)
                    .filter(user -> user.active() && SecurityContextHolder.getContext().getAuthentication() == null)
                    .ifPresent(user -> {
                        var authority = new SimpleGrantedAuthority("ROLE_" + user.role().name());
                        var authentication = new UsernamePasswordAuthenticationToken(
                                user.username(),
                                null,
                                List.of(authority)
                        );
                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    });
        } catch (Exception ignored) {
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}
