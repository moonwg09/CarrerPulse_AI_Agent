package com.careerpulse.learning.controller;

import com.careerpulse.analysis.service.AnalysisService;
import com.careerpulse.learning.dto.LearningJobResponse;
import com.careerpulse.learning.service.LearningContextService;
import com.careerpulse.learning.service.LearningService;
import com.careerpulse.user.entity.User;
import com.careerpulse.user.service.AuthService;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class LearningAnalysisControllerTest {
    final ObjectMapper mapper = new ObjectMapper();
    LearningService learning;
    LearningContextService contexts;
    AuthService users;
    AnalysisService analyses;
    LearningAnalysisController controller;
    MockRestServiceServer server;
    final UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
            "learner@example.com", "unused", List.of(new SimpleGrantedAuthority("ROLE_USER")));

    @BeforeEach
    void setup() {
        learning = mock(LearningService.class);
        contexts = mock(LearningContextService.class);
        users = mock(AuthService.class);
        analyses = mock(AnalysisService.class);
        User user = mock(User.class);
        when(user.getUserId()).thenReturn(7L);
        when(users.findByEmail(auth.getName())).thenReturn(user);
        var builder = RestClient.builder().baseUrl("http://learning.test");
        server = MockRestServiceServer.bindTo(builder).build();
        controller = new LearningAnalysisController(
                learning, contexts, users, analyses, mapper, builder.build());
    }

    @Test
    void authenticatedUsersRegionAndStoredAnalysisAreUsed() {
        var selected = List.of(new LearningJobResponse(
                42L, "테스트", "백엔드", "소프트웨어", "원문",
                LocalDate.of(2026, 10, 20), null, null));
        when(learning.getMyAvailableJobs(
                auth.getName(),
                List.of("서울특별시", "경기도")
        )).thenReturn(selected);
        var jobs = mapper.createArrayNode();
        jobs.addObject().put("jobId", "42").put("jobCategory", "SELECTED")
                .put("postedDate", "2026-10-20").putArray("requirements")
                .addObject().put("name", "Spring").put("type", "SKILL").put("category", "REQUIRED");
        var assessments = mapper.createArrayNode();
        assessments.addObject().put("name", "Spring").put("type", "SKILL")
                .put("status", "PARTIAL").put("evidence", "저장된 근거").putArray("missingDetails");
        when(contexts.build(7L, selected)).thenReturn(
                new LearningContextService.Context(jobs, assessments, 0, List.of()));

        server.expect(requestTo("http://learning.test/api/v1/learning/analyze"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.interests[0]").value("SELECTED"))
                .andExpect(jsonPath("$.jobs[0].jobId").value("42"))
                .andExpect(jsonPath("$.assessments[0].status").value("PARTIAL"))
                .andRespond(withSuccess("{\"targetJobCount\":1}", MediaType.APPLICATION_JSON));

        var result = controller.analyze(auth, new LearningAnalysisController.AnalyzeInput(
                null,
                List.of("서울특별시", "경기도")
        ));
        assertEquals("DATABASE", result.path("source").asText());
        assertEquals("서울특별시, 경기도", result.path("region").asText());
        assertEquals(2, result.path("regions").size());
        server.verify();
    }

    @Test
    void failedAutomaticAnalysisIsExcludedAndCounted() {
        var selected = List.of(new LearningJobResponse(
                42L, "테스트", "백엔드", "소프트웨어", "원문",
                LocalDate.now(), null, null));
        when(learning.getMyAvailableJobs(auth.getName(), List.of("전국"))).thenReturn(selected);
        when(contexts.build(7L, selected)).thenReturn(new LearningContextService.Context(
                mapper.createArrayNode(), mapper.createArrayNode(), 1, List.of(42L)));
        doThrow(new RuntimeException("분석 실패"))
                .when(analyses).compare(eq(auth.getName()), any());
        when(contexts.build(7L, List.of())).thenReturn(new LearningContextService.Context(
                mapper.createArrayNode(), mapper.createArrayNode(), 0, List.of()));
        server.expect(requestTo("http://learning.test/api/v1/learning/analyze"))
                .andRespond(withSuccess("{\"targetJobCount\":0}", MediaType.APPLICATION_JSON));
        var result = controller.analyze(auth, new LearningAnalysisController.AnalyzeInput("전국"));
        assertEquals(1, result.path("analysisFailureCount").asInt());
    }

    @Test
    void anonymousIsRejected() {
        var anonymous = new AnonymousAuthenticationToken(
                "key", "anonymousUser", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));
        assertEquals(401, assertThrows(ResponseStatusException.class,
                () -> controller.analyze(anonymous, new LearningAnalysisController.AnalyzeInput("서울")))
                .getStatusCode().value());
        verifyNoInteractions(learning, contexts, users);
    }

    @Test
    void upstreamValidationErrorRemains422() {
        when(learning.getMyAvailableJobs(auth.getName(), List.of("서울"))).thenReturn(List.of());
        when(contexts.build(7L, List.of())).thenReturn(new LearningContextService.Context(
                mapper.createArrayNode(), mapper.createArrayNode(), 0, List.of()));
        server.expect(requestTo("http://learning.test/api/v1/learning/analyze"))
                .andRespond(withStatus(HttpStatus.UNPROCESSABLE_ENTITY));
        assertEquals(422, assertThrows(ResponseStatusException.class,
                () -> controller.analyze(auth, new LearningAnalysisController.AnalyzeInput("서울")))
                .getStatusCode().value());
        server.verify();
    }
}
