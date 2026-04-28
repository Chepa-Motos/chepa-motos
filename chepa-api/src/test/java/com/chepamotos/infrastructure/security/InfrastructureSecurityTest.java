package com.chepamotos.infrastructure.security;

import com.chepamotos.domain.model.AccessToken;
import com.chepamotos.domain.model.AppUser;
import com.chepamotos.domain.model.UserRole;
import com.chepamotos.domain.port.AppUserRepository;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InfrastructureSecurityTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-01-28T10:00:00Z"), ZoneOffset.UTC);

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private JwtTokenDecoder jwtTokenDecoder;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void bcryptPasswordHasher_hashAndMatches() {
        BCryptPasswordHasher hasher = new BCryptPasswordHasher();

        String hash = hasher.hash("secret-password");

        assertNotNull(hash);
        assertFalse(hash.isBlank());
        assertTrue(hasher.matches("secret-password", hash));
        assertFalse(hasher.matches("other-password", hash));
    }

    @Test
    void sha256TokenHashService_hashTrimsAndRejectsBlank() {
        Sha256TokenHashService service = new Sha256TokenHashService();

        String hashA = service.hash("  refresh-token  ");
        String hashB = service.hash("refresh-token");

        assertEquals(hashA, hashB);
        assertEquals(64, hashA.length());
        assertThrows(IllegalArgumentException.class, () -> service.hash("   "));
        assertThrows(IllegalArgumentException.class, () -> service.hash(null));
    }

    @Test
    void jwtAccessTokenService_generateAndDecoder_extractUsername() {
        String secret = "0123456789abcdef0123456789abcdef";
        Clock liveClock = Clock.systemUTC();
        JwtAccessTokenService accessTokenService = new JwtAccessTokenService(secret, 15, liveClock);
        JwtTokenDecoder decoder = new JwtTokenDecoder(secret);
        AppUser user = AppUser.restore(7L, "admin", "hash", UserRole.GERENTE, true,
                LocalDateTime.of(2026, 1, 1, 8, 0));

        AccessToken token = accessTokenService.generate(user);

        assertEquals(900, token.expiresInSeconds());
        assertEquals("admin", decoder.extractUsername(token.token()));
    }

    @Test
    void jwtAccessTokenAndDecoder_rejectShortSecrets() {
        assertThrows(IllegalArgumentException.class,
                () -> new JwtAccessTokenService("short-secret", 15, FIXED_CLOCK));
        assertThrows(IllegalArgumentException.class,
                () -> new JwtTokenDecoder("short-secret"));
    }

    @Test
    void jwtAuthenticationFilter_withBearerTokenAuthenticatesActiveUser() throws Exception {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtTokenDecoder, appUserRepository);
        AppUser user = AppUser.restore(3L, "jose", "hash", UserRole.GERENTE, true,
                LocalDateTime.of(2026, 1, 1, 8, 0));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer valid.jwt.token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        when(jwtTokenDecoder.extractUsername("valid.jwt.token")).thenReturn("jose");
        when(appUserRepository.findByUsername("jose")).thenReturn(Optional.of(user));

        filter.doFilter(request, response, chain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertEquals("jose", authentication.getPrincipal());
        assertEquals(List.of(new SimpleGrantedAuthority("ROLE_GERENTE")), authentication.getAuthorities().stream().toList());
        verify(chain).doFilter(request, response);
    }

    @Test
    void jwtAuthenticationFilter_withoutBearerHeader_skipsAuthentication() throws Exception {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtTokenDecoder, appUserRepository);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(chain).doFilter(request, response);
        verify(jwtTokenDecoder, never()).extractUsername(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void jwtAuthenticationFilter_withInvalidTokenClearsSecurityContext() throws Exception {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtTokenDecoder, appUserRepository);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer invalid.jwt.token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("stale", null, List.of(new SimpleGrantedAuthority("ROLE_GERENTE"))));

        when(jwtTokenDecoder.extractUsername("invalid.jwt.token")).thenThrow(new RuntimeException("boom"));

        filter.doFilter(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(chain).doFilter(request, response);
    }

    @Test
    void apiAuthenticationEntryPoint_writesAuthRequiredJson() throws Exception {
        ApiAuthenticationEntryPoint entryPoint = new ApiAuthenticationEntryPoint(FIXED_CLOCK);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(request, response, new BadCredentialsException("missing auth"));

        assertEquals(401, response.getStatus());
        assertEquals("application/json", response.getContentType());
        var error = new com.fasterxml.jackson.databind.ObjectMapper().readValue(
                response.getContentAsByteArray(),
                com.chepamotos.adapter.dto.ApiErrorResponse.class);
        assertEquals("AUTH_REQUIRED", error.getCode());
        assertEquals("Authentication is required", error.getMessage());
        assertEquals("2026-01-28T10:00", error.getTimestamp().substring(0, 16));
    }

    @Test
    void apiAccessDeniedHandler_writesForbiddenJson() throws Exception {
        ApiAccessDeniedHandler handler = new ApiAccessDeniedHandler(FIXED_CLOCK);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.handle(request, response, new org.springframework.security.access.AccessDeniedException("denied"));

        assertEquals(403, response.getStatus());
        assertEquals("application/json", response.getContentType());
        var error = new com.fasterxml.jackson.databind.ObjectMapper().readValue(
                response.getContentAsByteArray(),
                com.chepamotos.adapter.dto.ApiErrorResponse.class);
        assertEquals("FORBIDDEN", error.getCode());
        assertEquals("You do not have permission to perform this action", error.getMessage());
        assertEquals("2026-01-28T10:00", error.getTimestamp().substring(0, 16));
    }
}