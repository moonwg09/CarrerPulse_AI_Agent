package com.careerpulse.job.repository;

import com.careerpulse.job.entity.JobBookmark;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.List;
import java.util.Optional;

public interface JobBookmarkRepository extends JpaRepository<JobBookmark, Long> {
    Optional<JobBookmark> findByUser_UserIdAndJob_JobId(Long userId, Long jobId);
    boolean existsByUser_UserIdAndJob_JobId(Long userId, Long jobId);

    @EntityGraph(attributePaths = "job")
    List<JobBookmark> findAllByUser_UserIdOrderByCreatedAtDesc(Long userId);
}
