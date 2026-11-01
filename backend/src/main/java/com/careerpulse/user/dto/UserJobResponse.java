package com.careerpulse.user.dto;

import java.util.List;

public record UserJobResponse(

        Long userJobId,
        String jobCode,
        String jobName,
        String searchKeyword,
        List<String> searchExpressions
) {
}
