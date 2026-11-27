package com.careerpulse.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PreferredCompanyRequest(
        @NotBlank @Size(max = 255) String companyName
) {}
