package com.careerpulse.analysis.service;

import com.careerpulse.user.model.DevelopmentJobCatalog;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AnalysisRecommendationScopeTest {

    @Test
    void 관심직무와_선택지역에_맞는_공고만_허용한다() {
        assertThat(DevelopmentJobCatalog.FULLSTACK.searchTerms())
                .contains("풀스택 개발자", "웹개발", "Full Stack");

        assertThat(AnalysisRecommendationService.matchesInterest(
                "Java 백엔드 개발자",
                "소프트웨어",
                "Spring API 개발",
                List.of("백엔드", "데이터 엔지니어")
        )).isTrue();

        assertThat(AnalysisRecommendationService.matchesInterest(
                "디자이너",
                "디자인",
                "UI 디자인",
                List.of("백엔드", "데이터 엔지니어")
        )).isFalse();

        assertThat(AnalysisRecommendationService.matchesRegion(
                "서울특별시 구로구",
                "서울특별시"
        )).isTrue();
        assertThat(AnalysisRecommendationService.matchesRegion(
                "부산광역시 해운대구",
                "서울특별시"
        )).isFalse();
        assertThat(AnalysisRecommendationService.matchesRegion(
                "부산광역시 해운대구",
                "전국"
        )).isTrue();
    }
}
