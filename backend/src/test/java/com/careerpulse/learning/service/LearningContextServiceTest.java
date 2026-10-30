package com.careerpulse.learning.service;

import com.careerpulse.learning.dto.LearningJobResponse;
import com.careerpulse.learning.repository.LearningAnalysisRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LearningContextServiceTest {

    @Test
    void expandsStoredMetadataAndUsesConfirmedLatestJudgments() {
        LearningAnalysisRepository repository = mock(LearningAnalysisRepository.class);
        LearningContextService service = new LearningContextService(repository, new ObjectMapper());
        var job = new LearningJobResponse(
                3L, "회사", "백엔드 개발자", "소프트웨어", "원문",
                LocalDate.of(2026, 10, 20), null, null
        );
        String experienceRule = """
            {"version":1,"skills":["Java","Spring"],"experience":"REST API 개발 경험"}
            """;
        String certificateRule = """
            {"version":1,"certificates":["정보처리기사"]}
            """;
        when(repository.findRequirements(List.of(3L))).thenReturn(List.of(
                new LearningAnalysisRepository.RequirementRow(
                        10L, 3L, "REQUIRED", "EXPERIENCE", "API 경험", experienceRule),
                new LearningAnalysisRepository.RequirementRow(
                        11L, 3L, "PREFERRED", "OBJECTIVE", "정보처리기사", certificateRule)
        ));
        when(repository.findLatestAssessments(7L, List.of(3L))).thenReturn(List.of(
                new LearningAnalysisRepository.AssessmentRow(
                        3L, 10L, "REQUIRED", "EXPERIENCE", "API 경험", experienceRule,
                        "PARTIAL", "일부 구현 경험만 확인됨", false),
                new LearningAnalysisRepository.AssessmentRow(
                        3L, 11L, "PREFERRED", "OBJECTIVE", "정보처리기사", certificateRule,
                        "UNMET", "자격증 미보유", false)
        ));

        var context = service.build(7L, List.of(job));

        assertThat(context.skippedJobCount()).isZero();
        assertThat(context.jobs().get(0).path("requirements").size()).isEqualTo(4);
        var assessmentNodes = new ArrayList<com.fasterxml.jackson.databind.JsonNode>();
        context.assessments().forEach(assessmentNodes::add);
        assertThat(assessmentNodes).anySatisfy(node -> {
            assertThat(node.path("name").asText()).isEqualTo("Java");
            assertThat(node.path("status").asText()).isEqualTo("PARTIAL");
        });
        assertThat(assessmentNodes).anySatisfy(node -> {
            assertThat(node.path("name").asText()).isEqualTo("정보처리기사");
            assertThat(node.path("status").asText()).isEqualTo("NO_EXPERIENCE");
        });
    }

    @Test
    void unconfirmedNoExperienceIsNotAcceptedAsNoExperience() {
        LearningAnalysisRepository repository = mock(LearningAnalysisRepository.class);
        LearningContextService service = new LearningContextService(repository, new ObjectMapper());
        var job = new LearningJobResponse(
                3L, "회사", "백엔드 개발자", "소프트웨어", "원문",
                LocalDate.of(2026, 10, 20), null, null
        );
        String rule = "{\"version\":1,\"skills\":[\"Java\"]}";
        when(repository.findRequirements(List.of(3L))).thenReturn(List.of(
                new LearningAnalysisRepository.RequirementRow(
                        10L, 3L, "REQUIRED", "EXPERIENCE", "Java", rule)
        ));
        when(repository.findLatestAssessments(7L, List.of(3L))).thenReturn(List.of(
                new LearningAnalysisRepository.AssessmentRow(
                        3L, 10L, "REQUIRED", "EXPERIENCE", "Java", rule,
                        "NO_EXPERIENCE", "AI 판정", false)
        ));

        var context = service.build(7L, List.of(job));

        assertThat(context.assessments().get(0).path("status").asText()).isEqualTo("UNKNOWN");
        assertThat(context.assessments().get(0).path("evidence").asText()).isEmpty();
    }
}
