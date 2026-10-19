package com.careerpulse.interviewquestion;

public record InterviewQuestionResponse(
        Long id,
        String category,
        String questionType,
        String content
) {
    // Entity → DTO 변환 도우미
    public static InterviewQuestionResponse from(InterviewQuestion question) {
        return new InterviewQuestionResponse(
                question.getId(),
                question.getCategory(),
                question.getQuestionType(),
                question.getContent()
        );
    }
}
