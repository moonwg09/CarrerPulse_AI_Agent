package com.careerpulse.user.dto;

import java.util.List;

public record DevelopmentJobResponse(
        String jobCode,
        String jobName,
        String searchKeyword,
        List<String> searchExpressions
) {
}
