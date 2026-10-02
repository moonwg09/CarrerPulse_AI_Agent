package com.careerpulse.user.dto;

import java.math.BigDecimal;

public record UserResponse(

        Long userId,
        String email,
        String name,
        String phone,
        String careerType,
        BigDecimal careerYears,
        String accountStatus
) {
}
