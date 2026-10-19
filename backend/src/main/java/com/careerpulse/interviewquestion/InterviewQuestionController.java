package com.careerpulse.interviewquestion;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController                              // 화면이 아니라 데이터(JSON)로 응답하는 창구
@RequestMapping("/api/interview-questions")  // 이 클래스의 모든 주소는 이걸로 시작해요
public class InterviewQuestionController {

    private final InterviewQuestionService service;

    public InterviewQuestionController(InterviewQuestionService service) {
        this.service = service;
    }

    // GET /api/interview-questions
    // GET /api/interview-questions?category=IT개발&questionType=직무
    @GetMapping
    public List<InterviewQuestionResponse> getQuestions(
            @RequestParam(name = "category", required = false) String category,
            @RequestParam(name = "questionType", required = false) String questionType) {
        return service.getQuestions(category, questionType);
    }

    // GET /api/interview-questions/3
    @GetMapping("/{id}")
    public InterviewQuestionResponse getQuestion(@PathVariable("id") Long id) {
        return service.getQuestion(id);
    }
}
