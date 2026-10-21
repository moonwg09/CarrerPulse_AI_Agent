package com.careerpulse.analysis.repository;

import com.careerpulse.analysis.entity.AnalysisResult;
import com.careerpulse.analysis.entity.RecommendationStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AnalysisResultRepository
        extends JpaRepository<AnalysisResult, Long> {

    // 같은 사용자가 같은 공고를 이미 비교했는지 확인 (재실행 시 갱신용)
    Optional<AnalysisResult> findByUser_UserIdAndJobKey(
            Long userId,
            String jobKey
    );

    // 내 분석 결과 전체 조회 (최근 비교 순)
    @EntityGraph(attributePaths = "items")
    List<AnalysisResult> findAllByUser_UserIdOrderByUpdatedAtDesc(
            Long userId
    );

    // 현재 로그인 사용자의 특정 분석 결과 조회
    @EntityGraph(attributePaths = "items")
    Optional<AnalysisResult> findByAnalysisIdAndUser_UserId(
            Long analysisId,
            Long userId
    );

    // 추천조회: 마감되지 않은 공고 중 특정 추천 구분만 가져온다
    @EntityGraph(attributePaths = "items")
    List<AnalysisResult>
    findAllByUser_UserIdAndRecommendationStatusAndCloseDateGreaterThanEqual(
            Long userId,
            RecommendationStatus recommendationStatus,
            LocalDate today
    );
}
