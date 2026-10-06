package com.careerpulse.review.controller;

import com.careerpulse.review.dto.CompanyReviewResponse;
import com.careerpulse.review.service.CompanyReviewService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class CompanyReviewController {

    private final CompanyReviewService companyReviewService;

    public CompanyReviewController(
            CompanyReviewService companyReviewService
    ) {
        this.companyReviewService = companyReviewService;
    }

    @GetMapping("/api/company-reviews")
    public List<CompanyReviewResponse> getCompanyReviews() {
        return companyReviewService.getCompanyReviews();
    }
}