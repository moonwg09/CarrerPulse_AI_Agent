package com.careerpulse.analysis.dto;

import jakarta.validation.constraints.AssertTrue;

public record NoExperienceConfirmationRequest(
        @AssertTrue(message = "경험 없음 확인에는 confirmed=true가 필요합니다.")
        boolean confirmed
) {
}
