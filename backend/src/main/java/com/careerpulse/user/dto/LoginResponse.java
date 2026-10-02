package com.careerpulse.user.dto;

public record LoginResponse(

        Long userId,
        String email,
        String name
) {
}
