package com.careerpulse.review.repository;

import com.careerpulse.review.entity.CompanyReview;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CompanyReviewRepository
        extends JpaRepository<CompanyReview, Long> {

    List<CompanyReview> findAllByOrderByCreatedAtDescReviewIdDesc();
}