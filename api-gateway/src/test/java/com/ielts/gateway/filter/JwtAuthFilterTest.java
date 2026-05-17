package com.ielts.gateway.filter;

import com.ielts.gateway.util.JwtValidator;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Optional;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthFilterTest {

    @Mock private JwtValidator jwtValidator;
    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private FilterChain chain;

    private JwtAuthFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthFilter(jwtValidator);
    }

    @Test
    void whitelistedPath_passesThrough() throws Exception {
        when(request.getRequestURI()).thenReturn("/auth/login");

        filter.doFilterInternal(request, response, chain);
        verify(chain).doFilter(request, response);
    }

    @Test
    void validToken_setsUserIdHeader() throws Exception {
        when(request.getRequestURI()).thenReturn("/writing/submit");
        when(request.getHeader("Authorization")).thenReturn("Bearer valid-token");
        when(jwtValidator.validate("valid-token")).thenReturn(Optional.of(42L));

        filter.doFilterInternal(request, response, chain);
        verify(chain).doFilter(any(), any());
    }

    @Test
    void missingToken_returns401() throws Exception {
        when(request.getRequestURI()).thenReturn("/writing/submit");
        when(request.getHeader("Authorization")).thenReturn(null);
        when(response.getWriter()).thenReturn(new PrintWriter(new StringWriter()));

        filter.doFilterInternal(request, response, chain);
        verify(response).setStatus(401);
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void invalidToken_returns401() throws Exception {
        when(request.getRequestURI()).thenReturn("/writing/submit");
        when(request.getHeader("Authorization")).thenReturn("Bearer bad-token");
        when(jwtValidator.validate("bad-token")).thenReturn(Optional.empty());
        when(response.getWriter()).thenReturn(new PrintWriter(new StringWriter()));

        filter.doFilterInternal(request, response, chain);
        verify(response).setStatus(401);
    }
}
