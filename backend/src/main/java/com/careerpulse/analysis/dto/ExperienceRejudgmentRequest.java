package com.careerpulse.analysis.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ExperienceRejudgmentRequest(
        @NotBlank
        @Pattern(regexp = "EXPERIENCED|PARTIAL")
        String userResponseType,

        @NotBlank
        @Size(max = 4000)
        String userExperienceText
) {
}
