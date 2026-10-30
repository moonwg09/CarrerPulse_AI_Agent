package com.careerpulse.analysis.repository;

import com.careerpulse.analysis.entity.JobRequirement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JobRequirementRepository extends JpaRepository<JobRequirement, Long> {
    Optional<JobRequirement> findByJobIdAndRequirementKey(Long jobId, String requirementKey);
}
