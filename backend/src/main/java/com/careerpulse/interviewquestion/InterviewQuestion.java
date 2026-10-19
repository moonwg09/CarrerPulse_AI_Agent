package com.careerpulse.interviewquestion;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity                                   // 이 클래스는 DB 테이블의 한 줄과 연결돼요
@Table(name = "interview_question")       // 연결할 테이블 이름
public class InterviewQuestion {

    @Id                                                  // 각 줄의 고유 번호(기본키)
    @GeneratedValue(strategy = GenerationType.IDENTITY)  // 1, 2, 3... MySQL이 자동으로 부여
    private Long id;

    @Column(nullable = false, length = 50)
    private String category;       // 직무 분야 (예: IT개발)

    @Column(name = "question_type", nullable = false, length = 30)
    private String questionType;   // 질문 유형 (예: 인성, 직무, 상황)

    @Column(nullable = false, length = 500)
    private String content;        // 면접 질문 내용

    protected InterviewQuestion() {
        // JPA가 내부에서 쓰는 기본 생성자예요 (직접 호출하지 않아요)
    }

    public Long getId() { return id; }
    public String getCategory() { return category; }
    public String getQuestionType() { return questionType; }
    public String getContent() { return content; }
}
