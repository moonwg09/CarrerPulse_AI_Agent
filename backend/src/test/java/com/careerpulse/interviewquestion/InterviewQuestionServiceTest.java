package com.careerpulse.interviewquestion;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@SpringBootTest
@ActiveProfiles("test")
@Transactional(readOnly = true)
class InterviewQuestionServiceTest {

    @Autowired
    private InterviewQuestionService service;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void readsInterviewQuestionsFromMySqlTable() {
        Long tableRowCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM interview_question", Long.class);
        List<InterviewQuestionResponse> results = service.getQuestions(null, null);

        printResults("MySQL interview_question 전체 조회", results);
        assertEquals(tableRowCount, (long) results.size(),
                "JPA 조회 결과 건수가 interview_question 테이블의 실제 건수와 일치해야 합니다.");
    }

    @Test
    void readsOneInterviewQuestionByIdFromMySqlTable() {
        List<InterviewQuestionResponse> databaseRows = jdbcTemplate.query(
                "SELECT id, category, question_type, content " +
                        "FROM interview_question ORDER BY id LIMIT 1",
                (resultSet, rowNumber) -> new InterviewQuestionResponse(
                        resultSet.getLong("id"),
                        resultSet.getString("category"),
                        resultSet.getString("question_type"),
                        resultSet.getString("content")));

        assertFalse(databaseRows.isEmpty(),
                "단건 조회 테스트를 위해 interview_question 테이블에 데이터가 1건 이상 있어야 합니다.");

        InterviewQuestionResponse expected = databaseRows.get(0);
        InterviewQuestionResponse result = service.getQuestion(expected.id());

        printResults("MySQL interview_question ID 단건 조회", List.of(result));
        assertEquals(expected, result,
                "실제 테이블의 ID로 조회한 값이 DB 행과 일치해야 합니다.");
    }

    private void printResults(String label, List<InterviewQuestionResponse> results) {
        System.out.printf("%n[%s] 총 %d건%n", label, results.size());
        if (results.isEmpty()) {
            System.out.println("테이블에 조회할 데이터가 없습니다.");
            return;
        }

        results.forEach(result -> System.out.printf(
                "ID: %d | 분류: %s | 질문 유형: %s | 질문: %s%n",
                result.id(),
                result.category(),
                result.questionType(),
                result.content()));
    }
}
