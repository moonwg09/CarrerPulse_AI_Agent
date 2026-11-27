package com.careerpulse.user.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UserResponse(

        Long userId,
        String email,
        String name,
        String phone,
        String careerType,
        BigDecimal careerYears,
        LocalDate preparationTargetDate,
        String accountStatus
) {
}
