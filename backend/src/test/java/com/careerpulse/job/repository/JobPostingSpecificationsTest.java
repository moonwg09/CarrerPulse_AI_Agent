package com.careerpulse.job.repository;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JobPostingSpecificationsTest {

    @Test
    void removesGenericDeveloperSuffixFromInterestJobName() {
        assertThat(JobPostingSpecifications.normalizeInterestKeyword("백엔드 개발자"))
                .isEqualTo("백엔드");
        assertThat(JobPostingSpecifications.normalizeInterestKeyword("프론트엔드개발자"))
                .isEqualTo("프론트엔드");
    }

    @Test
    void usesDevelopmentKeywordForGenericDeveloperInterest() {
        assertThat(JobPostingSpecifications.normalizeInterestKeyword("개발자"))
                .isEqualTo("개발");
    }
}
