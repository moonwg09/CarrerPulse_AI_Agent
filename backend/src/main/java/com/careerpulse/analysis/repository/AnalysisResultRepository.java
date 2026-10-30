package com.careerpulse.analysis.repository;

import com.careerpulse.analysis.entity.AnalysisResult;
import com.careerpulse.analysis.entity.AnalysisStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AnalysisResultRepository extends JpaRepository<AnalysisResult, Long> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE AnalysisResult result
               SET result.analysisStatus = com.careerpulse.analysis.entity.AnalysisStatus.INVALIDATED
             WHERE result.user.userId = :userId
               AND result.analysisStatus = com.careerpulse.analysis.entity.AnalysisStatus.COMPLETED
            """)
    int invalidateCompletedByUserId(@Param("userId") Long userId);

    @EntityGraph(attributePaths = {"items", "items.requirement"})
    Optional<AnalysisResult> findFirstByUser_UserIdAndJobIdOrderByCreatedAtDesc(
            Long userId,
            Long jobId
    );

    @EntityGraph(attributePaths = {"items", "items.requirement"})
    List<AnalysisResult> findAllByUser_UserIdOrderByAnalyzedAtDescCreatedAtDesc(Long userId);

    @EntityGraph(attributePaths = {"items", "items.requirement"})
    List<AnalysisResult> findAllByUser_UserIdAndAnalysisStatusOrderByAnalyzedAtDesc(
            Long userId,
            AnalysisStatus analysisStatus
    );

    @EntityGraph(attributePaths = {"items", "items.requirement"})
    Optional<AnalysisResult> findByAnalysisIdAndUser_UserId(Long analysisId, Long userId);
}
