package com.careerpulse.user.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

public record UpdateUserRequest(

        String name,

        @Pattern(
                regexp = "^$|^0\\d{1,2}-?\\d{3,4}-?\\d{4}$",
                message = "전화번호를 010-1234-5678 형식으로 입력해 주세요."
        )
        String phone,

        @Pattern(regexp = "NEW|EXPERIENCED")
        String careerType,

        @DecimalMin("0.0")
        @Digits(integer=3, fraction=1)
        BigDecimal careerYears
) {
}
