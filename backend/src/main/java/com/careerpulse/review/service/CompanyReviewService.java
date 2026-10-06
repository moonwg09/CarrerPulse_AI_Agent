package com.careerpulse.review.service;

import com.careerpulse.review.dto.CompanyReviewResponse;
import com.careerpulse.review.repository.CompanyReviewRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class CompanyReviewService {

    private final CompanyReviewRepository companyReviewRepository;

    public CompanyReviewService(
            CompanyReviewRepository companyReviewRepository
    ) {
        this.companyReviewRepository = companyReviewRepository;
    }

    public List<CompanyReviewResponse> getCompanyReviews() {
        return companyReviewRepository
                .findAllByOrderByCreatedAtDescReviewIdDesc()
                .stream()
                .map(CompanyReviewResponse::from)
                .toList();
    }
}