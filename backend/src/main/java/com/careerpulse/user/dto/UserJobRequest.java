package com.careerpulse.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserJobRequest(

        @NotBlank
        @Size(max = 50)
        String jobCode,

        @NotBlank
        @Size(max = 100)
        String jobName
) {
}
