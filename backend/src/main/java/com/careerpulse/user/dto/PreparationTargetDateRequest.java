package com.careerpulse.user.dto;

import jakarta.validation.constraints.FutureOrPresent;

import java.time.LocalDate;

public record PreparationTargetDateRequest(
        @FutureOrPresent LocalDate preparationTargetDate
) {
}
