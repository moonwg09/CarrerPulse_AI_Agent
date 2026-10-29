package com.careerpulse.job.repository;

import com.careerpulse.job.entity.JobPosting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface JobPostingRepository extends
        JpaRepository<JobPosting, Long>,
        JpaSpecificationExecutor<JobPosting> {

    Optional<JobPosting> findBySourceAndExternalJobId(
            String source,
            String externalJobId
    );
}
