package com.ielts.data.dto;

public record LoginResponse(String token, Long userId, String email) {}
