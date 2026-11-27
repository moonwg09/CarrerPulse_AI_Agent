package com.careerpulse.job.client;

import java.util.List;

public record AiCompanySearchResponse(
        String query,
        List<String> companies
) {
}
