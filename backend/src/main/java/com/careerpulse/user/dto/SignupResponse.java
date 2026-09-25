package com.careerpulse.user.dto;

public record SignupResponse(

        Long userId,

        String email,

        String name
) {
}
