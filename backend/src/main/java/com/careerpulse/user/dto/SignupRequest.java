package com.careerpulse.user.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record SignupRequest(

    @NotBlank
    @Email
    String email,

    @NotBlank
    @Size(min = 8, max = 72)
    String password,

    @NotBlank
    String name,

    @NotBlank
    @Pattern(regexp = "NEW|EXPERIENCED")
    String careerType,

    @DecimalMin("0.0")
    @Digits(integer = 3, fraction = 1)
    BigDecimal careerYears,

    @NotNull
    @AssertTrue
    Boolean consented,

    @NotBlank
    String consentVersion
) {

}
