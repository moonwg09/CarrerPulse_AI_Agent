package com.careerpulse.interviewquestion;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service                           // "나는 주방장 역할이야" (Spring이 알아서 관리해요)
@Transactional(readOnly = true)    // 조회 전용: 데이터를 바꾸지 않는다는 표시
public class InterviewQuestionService {

    private final InterviewQuestionRepository repository;

    // Spring이 Repository를 알아서 넣어줘요 (생성자 주입)
    public InterviewQuestionService(InterviewQuestionRepository repository) {
        this.repository = repository;
    }

    // 목록 조회
    public List<InterviewQuestionResponse> getQuestions(String category, String questionType) {
        String cat = blankToNull(category);
        String type = blankToNull(questionType);

        List<InterviewQuestion> questions;
        if (cat != null && type != null) {
            questions = repository.findByCategoryAndQuestionType(cat, type);
        } else if (cat != null) {
            questions = repository.findByCategory(cat);
        } else if (type != null) {
            questions = repository.findByQuestionType(type);
        } else {
            questions = repository.findAll();
        }

        return questions.stream()
                .map(InterviewQuestionResponse::from)
                .toList();
    }

    // 상세 조회: 없는 번호면 404(찾을 수 없음)로 응답
    public InterviewQuestionResponse getQuestion(Long id) {
        InterviewQuestion question = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "면접 질문을 찾을 수 없습니다. id=" + id));
        return InterviewQuestionResponse.from(question);
    }

    // 빈 문자열("")이나 공백만 온 값을 "조건 없음(null)"으로 바꿔줘요
    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}
