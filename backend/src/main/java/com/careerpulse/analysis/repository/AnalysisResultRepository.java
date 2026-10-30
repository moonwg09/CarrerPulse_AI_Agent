package com.careerpulse.analysis.repository;

import com.careerpulse.analysis.entity.AnalysisResult;
import com.careerpulse.analysis.entity.AnalysisStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AnalysisResultRepository extends JpaRepository<AnalysisResult, Long> {

    @EntityGraph(attributePaths = {"items", "items.requirement"})
    Optional<AnalysisResult> findFirstByUser_UserIdAndJobIdOrderByCreatedAtDesc(
            Long userId,
            Long jobId
    );

    @EntityGraph(attributePaths = {"items", "items.requirement"})
    List<AnalysisResult> findAllByUser_UserIdOrderByCreatedAtDesc(Long userId);

    @EntityGraph(attributePaths = {"items", "items.requirement"})
    List<AnalysisResult> findAllByUser_UserIdAndAnalysisStatus(
            Long userId,
            AnalysisStatus analysisStatus
    );

    @EntityGraph(attributePaths = {"items", "items.requirement"})
    Optional<AnalysisResult> findByAnalysisIdAndUser_UserId(Long analysisId, Long userId);
}
