package com.careerpulse.analysis.repository;

import com.careerpulse.analysis.entity.AnalysisItem;
import com.careerpulse.analysis.entity.MatchStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AnalysisItemRepository
        extends JpaRepository<AnalysisItem, Long> {

    List<AnalysisItem> findAllByAnalysisResult_AnalysisIdOrderByItemOrderAsc(
            Long analysisId
    );

    // 특정 분석 결과에서 판정 상태별 건수 (요약 화면용)
    long countByAnalysisResult_AnalysisIdAndMatchStatus(
            Long analysisId,
            MatchStatus matchStatus
    );
}
