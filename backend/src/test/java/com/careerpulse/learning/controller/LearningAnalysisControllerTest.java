package com.careerpulse.learning.controller;

import com.careerpulse.learning.dto.LearningJobResponse;
import com.careerpulse.learning.service.LearningService;
import com.careerpulse.user.dto.UserJobResponse;
import com.careerpulse.user.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

/** 로그인 사용자 기준의 데이터 선택/전달 계약을 DB와 네트워크 없이 검증한다. */
class LearningAnalysisControllerTest {
    ObjectMapper mapper = new ObjectMapper();
    LearningService learning;
    UserService users;
    LearningAnalysisController controller;
    MockRestServiceServer server;
    final UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
            "learner@example.com", "unused", List.of(new SimpleGrantedAuthority("ROLE_USER")));

    @BeforeEach
    void setup() {
        learning = mock(LearningService.class);
        users = mock(UserService.class);
        var builder = RestClient.builder().baseUrl("http://learning.test");
        server = MockRestServiceServer.bindTo(builder).build();
        controller = new LearningAnalysisController(learning, users, mapper, builder.build());
        when(users.getMyJobs(auth.getName())).thenReturn(List.of(new UserJobResponse(1L, "BACKEND", "백엔드")));
    }

    LearningAnalysisController.AnalyzeInput input(String source) {
        return new LearningAnalysisController.AnalyzeInput(source, mapper.createArrayNode());
    }

    @Test
    void testSourceUsesOwnInterestsAndDoesNotReadDatabasePostings() {
        server.expect(requestTo("http://learning.test/api/v1/learning/analyze"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.interests[0]").value("BACKEND"))
                // 전체 자체 공고 21개를 전달하고 Python에서 관심 직무별 최대 10개를 선택한다.
                .andExpect(jsonPath("$.jobs.length()").value(21))
                .andExpect(jsonPath("$.jobs[0].postedDate").isNotEmpty())
                .andExpect(jsonPath("$.jobs[0].postedOffsetDays").doesNotExist())
                .andRespond(withSuccess("{\"targetJobCount\":10}", MediaType.APPLICATION_JSON));
        var result = controller.analyze(auth, input("TEST"));
        assertEquals("TEST", result.path("source").asText());
        verifyNoInteractions(learning);
        verify(users).getMyJobs("learner@example.com");
        server.verify();
    }

    @Test
    void databaseUsesAuthenticatedUsersJobsAndStructuredRequirements() {
        when(learning.getMyAvailableJobs(auth.getName())).thenReturn(List.of(new LearningJobResponse(
                42L, "테스트", "백엔드", "BACKEND", "{\"requirements\":[]}",
                LocalDate.of(2026, 10, 20), null, null)));
        server.expect(requestTo("http://learning.test/api/v1/learning/analyze"))
                .andExpect(jsonPath("$.jobs[0].jobId").value("42"))
                .andExpect(jsonPath("$.jobs[0].requirements").isArray())
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
        assertEquals("DATABASE", controller.analyze(auth, input("DATABASE")).path("source").asText());
        server.verify();
    }

    @Test
    void rawDescriptionsAreNotSilentlyAnalyzedAsEmptyRequirements() {
        when(learning.getMyAvailableJobs(auth.getName())).thenReturn(List.of(new LearningJobResponse(
                42L, "테스트", "백엔드", "BACKEND", "일반 본문", LocalDate.now(), null, null)));
        assertEquals(422, assertThrows(ResponseStatusException.class,
                () -> controller.analyze(auth, input("DATABASE"))).getStatusCode().value());
    }

    @Test
    void rejectsAnonymousAndInvalidSource() {
        var anonymous = new AnonymousAuthenticationToken("key", "anonymousUser", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));
        assertEquals(401, assertThrows(ResponseStatusException.class,
                () -> controller.analyze(anonymous, input("TEST"))).getStatusCode().value());
        assertEquals(400, assertThrows(ResponseStatusException.class,
                () -> controller.analyze(auth, input("INVALID"))).getStatusCode().value());
        verifyNoInteractions(users, learning);
    }

    @Test
    void upstreamValidationErrorRemains422() {
        server.expect(requestTo("http://learning.test/api/v1/learning/analyze"))
                .andRespond(withStatus(HttpStatus.UNPROCESSABLE_ENTITY));
        assertEquals(422, assertThrows(ResponseStatusException.class,
                () -> controller.analyze(auth, input("TEST"))).getStatusCode().value());
        server.verify();
    }
}
