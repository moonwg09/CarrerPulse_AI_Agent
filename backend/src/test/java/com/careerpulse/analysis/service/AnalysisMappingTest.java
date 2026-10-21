package com.careerpulse.analysis.service;

import com.careerpulse.analysis.dto.AiJobMatchResult;
import com.careerpulse.analysis.entity.MatchStatus;
import com.careerpulse.analysis.entity.RecommendationStatus;
import com.careerpulse.analysis.entity.RequirementCategory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * FastAPI(JobMatchResult) 응답 매핑과 판정 변환 규칙 검증.
 * DB / AI 서버 없이 동작한다.
 */
class AnalysisMappingTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final AnalysisService analysisService =
            new AnalysisService(null, null, null, objectMapper);

    private static final String SAMPLE_RESPONSE = """
            {
              "jobTitle": "백엔드 개발자",
              "companyName": "A회사",
              "requirementMatches": [
                {
                  "requirement": "Spring Boot 기반 REST API 개발 경험",
                  "requirementCategory": "REQUIRED",
                  "status": "EXPERIENCED",
                  "reason": "프로젝트에서 Spring Boot API를 구현한 기록이 있습니다.",
                  "matchedSkills": ["Spring Boot", "JPA"],
                  "missingSkills": [],
                  "jobEvidence": [
                    {
                      "text": "Spring Boot 기반 REST API 개발 경험 필수",
                      "source": "JOB_POSTING",
                      "section": "자격요건"
                    }
                  ],
                  "userEvidence": [
                    {
                      "text": "Spring Boot와 JPA로 사내 API 12개를 개발했습니다.",
                      "source": "RESUME",
                      "page": 2,
                      "paragraphIndex": 4
                    }
                  ]
                },
                {
                  "requirement": "Kubernetes 운영 경험",
                  "requirementCategory": "PREFERRED",
                  "status": "알수없는값",
                  "reason": "관련 기록을 찾지 못했습니다.",
                  "matchedSkills": [],
                  "missingSkills": ["Kubernetes"],
                  "jobEvidence": [],
                  "userEvidence": []
                }
              ],
              "objectiveConditionMatches": [
                {
                  "conditionType": "EDUCATION",
                  "requirement": "학사 이상",
                  "requirementCategory": "REQUIRED",
                  "status": "SATISFIED",
                  "reason": "학력 사항에 학사 졸업이 기재되어 있습니다."
                }
              ],
              "score": {
                "overallMatchRate": 72.345,
                "requiredMatchRate": 80.0,
                "preferredMatchRate": 50.0,
                "objectiveMatchRate": 100.0,
                "evaluationCoverage": 90.0
              },
              "recommendationStatus": "GROWTH_CANDIDATE",
              "warnings": ["우대 요건 중 일부는 근거를 찾지 못했습니다."],
              "someFutureField": "무시되어야 함"
            }
            """;

    @Test
    void FastAPI_응답을_DTO로_매핑한다() throws Exception {

        AiJobMatchResult result = objectMapper.readValue(
                SAMPLE_RESPONSE,
                AiJobMatchResult.class
        );

        assertThat(result.jobTitle()).isEqualTo("백엔드 개발자");
        assertThat(result.companyName()).isEqualTo("A회사");
        assertThat(result.recommendationStatus())
                .isEqualTo("GROWTH_CANDIDATE");

        assertThat(result.requirementMatches()).hasSize(2);
        assertThat(result.objectiveConditionMatches()).hasSize(1);
        assertThat(result.warnings()).hasSize(1);

        assertThat(result.score().overallMatchRate())
                .isEqualTo(72.345);
    }

    @Test
    void 근거_원문이_가공되지_않고_남는다() throws Exception {

        AiJobMatchResult result = objectMapper.readValue(
                SAMPLE_RESPONSE,
                AiJobMatchResult.class
        );

        AiJobMatchResult.RequirementMatch first =
                result.requirementMatches().get(0);

        JsonNode userEvidence = first.userEvidence().get(0);

        assertThat(userEvidence.get("text").asText())
                .isEqualTo("Spring Boot와 JPA로 사내 API 12개를 개발했습니다.");
        assertThat(userEvidence.get("source").asText())
                .isEqualTo("RESUME");
        assertThat(userEvidence.get("page").asInt()).isEqualTo(2);

        // 저장용 JSON 으로 바꿔도 원문이 유지되는지 확인
        String stored = analysisService.toJson(first.userEvidence());
        JsonNode reloaded = analysisService.readJson(stored);

        assertThat(reloaded.get(0).get("text").asText())
                .isEqualTo("Spring Boot와 JPA로 사내 API 12개를 개발했습니다.");
    }

    @Test
    void 알_수_없는_판정값은_기록_부족으로_내린다() {

        assertThat(MatchStatus.from("EXPERIENCED"))
                .isEqualTo(MatchStatus.EXPERIENCED);
        assertThat(MatchStatus.from("partial"))
                .isEqualTo(MatchStatus.PARTIAL);

        // AI 가 예상 밖의 값을 주면 '경험 있음' 으로 올리지 않는다.
        assertThat(MatchStatus.from("알수없는값"))
                .isEqualTo(MatchStatus.INSUFFICIENT_EVIDENCE);
        assertThat(MatchStatus.from(null))
                .isEqualTo(MatchStatus.INSUFFICIENT_EVIDENCE);
    }

    @Test
    void 구분과_추천상태의_기본값을_확인한다() {

        assertThat(RequirementCategory.from("REQUIRED"))
                .isEqualTo(RequirementCategory.REQUIRED);
        assertThat(RequirementCategory.from(null))
                .isEqualTo(RequirementCategory.UNCLEAR);
        assertThat(RequirementCategory.from("기타"))
                .isEqualTo(RequirementCategory.UNCLEAR);

        assertThat(RecommendationStatus.from("RECOMMENDED"))
                .isEqualTo(RecommendationStatus.RECOMMENDED);
        assertThat(RecommendationStatus.from("")).isNull();
        assertThat(RecommendationStatus.from(null)).isNull();
        assertThat(RecommendationStatus.from("없는값")).isNull();
    }

    @Test
    void 일치율은_소수점_둘째자리로_저장한다() {

        assertThat(AnalysisService.rate(72.345))
                .isEqualByComparingTo(new BigDecimal("72.35"));
        assertThat(AnalysisService.rate(80.0))
                .isEqualByComparingTo(new BigDecimal("80.00"));
        assertThat(AnalysisService.rate(null)).isNull();
    }

    @Test
    void 긴_문자열은_컬럼_길이에_맞춰_자른다() {

        String longText = "가".repeat(600);

        assertThat(AnalysisService.truncate(longText, 500))
                .hasSize(500);
        assertThat(AnalysisService.truncate("짧은 값", 500))
                .isEqualTo("짧은 값");
        assertThat(AnalysisService.truncate(null, 500)).isNull();
    }

    @Test
    void 판정_상태를_화면_문구로_바꾼다() {

        assertThat(AnalysisService.statusLabel(
                MatchStatus.EXPERIENCED
        )).isEqualTo("경험 있음");

        assertThat(AnalysisService.statusLabel(
                MatchStatus.PARTIAL
        )).isEqualTo("일부만 있음");

        assertThat(AnalysisService.statusLabel(
                MatchStatus.INSUFFICIENT_EVIDENCE
        )).isEqualTo("기록 부족");

        assertThat(AnalysisService.statusLabel(
                MatchStatus.NO_EXPERIENCE
        )).isEqualTo("경험 없음");
    }
}
