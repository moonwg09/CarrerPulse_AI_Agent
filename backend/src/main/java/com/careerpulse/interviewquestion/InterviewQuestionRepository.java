package com.careerpulse.interviewquestion;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InterviewQuestionRepository extends JpaRepository<InterviewQuestion, Long> {

    // → select * from interview_question where category = ?
    List<InterviewQuestion> findByCategory(String category);

    // → select * from interview_question where question_type = ?
    List<InterviewQuestion> findByQuestionType(String questionType);

    // → select * from interview_question where category = ? and question_type = ?
    List<InterviewQuestion> findByCategoryAndQuestionType(String category, String questionType);
}
