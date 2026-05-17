package com.ielts.data.service;

import com.ielts.data.dto.LoginRequest;
import com.ielts.data.dto.LoginResponse;
import com.ielts.data.dto.RegisterRequest;
import com.ielts.data.entity.User;
import com.ielts.data.repository.UserRepository;
import com.ielts.data.util.JwtUtil;
import com.ielts.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    private AuthService authService;
    private PasswordEncoder passwordEncoder;
    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        jwtUtil = new JwtUtil("test-secret-key-that-is-at-least-32-bytes-long", 7200000L);
        authService = new AuthService(userRepository, passwordEncoder, jwtUtil);
    }

    @Test
    void register_newUser_succeeds() {
        var request = new RegisterRequest("test@example.com", "password123", "Test");
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(1L);
            return u;
        });

        Long userId = authService.register(request);
        assertEquals(1L, userId);
    }

    @Test
    void register_duplicateEmail_throws() {
        var request = new RegisterRequest("test@example.com", "password123", "Test");
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        assertThrows(BusinessException.class, () -> authService.register(request));
    }

    @Test
    void login_validCredentials_returnsToken() {
        var request = new LoginRequest("test@example.com", "password123");
        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");
        user.setPasswordHash(passwordEncoder.encode("password123"));

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        LoginResponse response = authService.login(request);
        assertNotNull(response.token());
        assertEquals(1L, response.userId());
    }

    @Test
    void login_wrongPassword_throws() {
        var request = new LoginRequest("test@example.com", "wrong");
        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");
        user.setPasswordHash(passwordEncoder.encode("password123"));

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        assertThrows(BusinessException.class, () -> authService.login(request));
    }
}
