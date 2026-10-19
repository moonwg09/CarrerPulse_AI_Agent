package com.careerpulse.learning.dto;

import java.time.LocalDate;

// 학습 분석에 사용할 공고 한 건을 담는 DTO.
// DB의 모든 컬럼 대신 분석에 필요한 값만 전달한다.
public record LearningJobResponse(

        // 공고의 실제 DB 식별자
        Long jobId,

        // 기업명
        String companyName,

        // 채용공고 제목
        String title,

        // 공고에 저장된 직무 분류
        String jobCategory,

        // Python AI 서버에 분석을 요청할 공고 본문
        String description,

        // 공고 게시일 최신순 정렬에 사용한다
        LocalDate postedDate,

        // 마감일 DB에서 NULL이면 이 값도 null이다.
        LocalDate deadline
) {
}