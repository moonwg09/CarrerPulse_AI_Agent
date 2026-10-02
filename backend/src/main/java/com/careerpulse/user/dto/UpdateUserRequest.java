package com.careerpulse.user.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

public record UpdateUserRequest(

        String name,

        String phone,

        @Pattern(regexp = "NEW|EXPERIENCED")
        String careerType,

        @DecimalMin("0.0")
        @Digits(integer=3, fraction=1)
        BigDecimal careerYears
) {
}
