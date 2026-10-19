package com.careerpulse.interviewquestion;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InterviewQuestionServiceTest {

    @Mock
    private InterviewQuestionRepository repository;

    private InterviewQuestionService service;

    @BeforeEach
    void setUp() {
        service = new InterviewQuestionService(repository);
    }

    @Test
    void getQuestionsWithoutFiltersReturnsAllQuestions() {
        InterviewQuestion question = question(1L, "공통", "인성", "자기소개를 해주세요.");
        when(repository.findAll()).thenReturn(List.of(question));

        List<InterviewQuestionResponse> result = service.getQuestions(null, null);

        assertEquals(List.of(new InterviewQuestionResponse(
                1L, "공통", "인성", "자기소개를 해주세요.")), result);
        verify(repository).findAll();
        verifyNoMoreInteractions(repository);
    }

    @Test
    void getQuestionsWithBlankFiltersReturnsAllQuestions() {
        when(repository.findAll()).thenReturn(List.of());

        List<InterviewQuestionResponse> result = service.getQuestions("  ", " ");

        assertEquals(List.of(), result);
        verify(repository).findAll();
        verifyNoMoreInteractions(repository);
    }

    @Test
    void getQuestionsWithCategoryAndTypeUsesBothFilters() {
        InterviewQuestion question = question(2L, "IT개발", "직무", "사용한 기술을 설명해 주세요.");
        when(repository.findByCategoryAndQuestionType("IT개발", "직무"))
                .thenReturn(List.of(question));

        List<InterviewQuestionResponse> result = service.getQuestions(" IT개발 ", " 직무 ");

        assertEquals(List.of(new InterviewQuestionResponse(
                2L, "IT개발", "직무", "사용한 기술을 설명해 주세요.")), result);
        verify(repository).findByCategoryAndQuestionType("IT개발", "직무");
        verifyNoMoreInteractions(repository);
    }

    @Test
    void getQuestionsWithOnlyCategoryUsesCategoryFilter() {
        when(repository.findByCategory("IT개발")).thenReturn(List.of());

        assertEquals(List.of(), service.getQuestions("IT개발", null));

        verify(repository).findByCategory("IT개발");
        verifyNoMoreInteractions(repository);
    }

    @Test
    void getQuestionsWithOnlyTypeUsesTypeFilter() {
        when(repository.findByQuestionType("상황")).thenReturn(List.of());

        assertEquals(List.of(), service.getQuestions(null, "상황"));

        verify(repository).findByQuestionType("상황");
        verifyNoMoreInteractions(repository);
    }

    @Test
    void getQuestionReturnsQuestionById() {
        InterviewQuestion question = question(3L, "사무행정", "상황", "우선순위를 어떻게 정하시나요?");
        when(repository.findById(3L)).thenReturn(Optional.of(question));

        InterviewQuestionResponse result = service.getQuestion(3L);

        assertEquals(new InterviewQuestionResponse(
                3L, "사무행정", "상황", "우선순위를 어떻게 정하시나요?"), result);
        verify(repository).findById(3L);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void getQuestionForUnknownIdThrowsNotFound() {
        when(repository.findById(999L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.getQuestion(999L));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(repository).findById(999L);
        verifyNoMoreInteractions(repository);
    }

    private InterviewQuestion question(Long id, String category, String type, String content) {
        InterviewQuestion question = new InterviewQuestion();
        setField(question, "id", id);
        setField(question, "category", category);
        setField(question, "questionType", type);
        setField(question, "content", content);
        return question;
    }

    private void setField(InterviewQuestion question, String fieldName, Object value) {
        try {
            Field field = InterviewQuestion.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(question, value);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Could not set test field: " + fieldName, exception);
        }
    }
}
