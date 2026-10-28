package com.careerpulse.job.repository;

import com.careerpulse.job.entity.JobPosting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JobPostingRepository extends JpaRepository<JobPosting, Long> {

    Optional<JobPosting> findBySourceAndExternalJobId(
            String source,
            String externalJobId
    );
}
