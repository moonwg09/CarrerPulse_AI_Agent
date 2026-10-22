package com.careerpulse.learning.controller;

import com.careerpulse.learning.repository.LearningPlanRepository;
import com.careerpulse.user.service.AuthService;
import com.careerpulse.user.entity.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.http.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class LearningPlanControllerTest {
    ObjectMapper mapper = new ObjectMapper();
    LearningAnalysisController analysis;
    LearningPlanRepository plans;
    AuthService auth;
    LearningPlanController controller;
    MockRestServiceServer server;
    final UsernamePasswordAuthenticationToken login = new UsernamePasswordAuthenticationToken("me@example.com", "unused",
            List.of(new SimpleGrantedAuthority("ROLE_USER")));

    @BeforeEach
    void setup() throws Exception {
        analysis = mock(LearningAnalysisController.class);
        plans = mock(LearningPlanRepository.class);
        auth = mock(AuthService.class);
        User user = mock(User.class);
        when(user.getUserId()).thenReturn(7L);
        when(auth.findByEmail(login.getName())).thenReturn(user);
        when(analysis.analyze(eq(login), any())).thenReturn(mapper.readTree("""
            {"referenceDate":"2026-10-20","targetJobCount":10,"preparation":[
            {"name":"SQL","preparationType":"TECHNICAL","rank":1,"targets":["실행 계획"]}]}
            """));
        var builder = RestClient.builder().baseUrl("http://plan.test");
        server = MockRestServiceServer.bindTo(builder).build();
        controller = new LearningPlanController(analysis, plans, auth, mapper, builder.build());
    }

    LearningPlanController.PlanInput input() {
        return new LearningPlanController.PlanInput(LocalDate.of(2026,10,20), LocalDate.of(2026,11,2),
                new BigDecimal("8"), "TEST", "TEST", mapper.createArrayNode());
    }

    @Test
    void generationUsesServerAnalysisAndSavesToAuthenticatedUser() {
        server.expect(requestTo("http://plan.test/api/v1/learning/plans/generate"))
                .andExpect(jsonPath("$.priorities[0].targets[0]").value("실행 계획"))
                .andExpect(jsonPath("$.weeklyAvailableHours").value(8))
                .andRespond(withSuccess("{\"weeks\":[]}", MediaType.APPLICATION_JSON));
        when(plans.save(eq(7L), any())).thenAnswer(call -> call.getArgument(1));
        var result = controller.generate(login, input());
        assertEquals("TEST", result.path("source").asText());
        verify(analysis).analyze(eq(login), any());
        verify(plans).save(eq(7L), any());
        server.verify();
    }

    @Test
    void emptyPreparationRejectsGeneration() throws Exception {
        when(analysis.analyze(eq(login), any())).thenReturn(mapper.readTree("{\"preparation\":[]}"));
        assertEquals(422, assertThrows(ResponseStatusException.class,
                () -> controller.generate(login, input())).getStatusCode().value());
        verifyNoInteractions(plans);
    }

    @Test
    void latestReadsOwnStoredPlanWithoutAi() throws Exception {
        when(plans.latest(7L)).thenReturn(mapper.readTree("{\"planId\":15}"));
        assertEquals(15, controller.latest(login).path("planId").asInt());
        verifyNoInteractions(analysis);
        server.verify();
    }

    @Test
    void aiFailureDoesNotSavePartialPlan() {
        server.expect(requestTo("http://plan.test/api/v1/learning/plans/generate"))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
        assertEquals(503, assertThrows(ResponseStatusException.class,
                () -> controller.generate(login, input())).getStatusCode().value());
        verifyNoInteractions(plans);
    }

    @Test
    void anonymousIsRejected() {
        assertEquals(401, assertThrows(ResponseStatusException.class,
                () -> controller.latest(null)).getStatusCode().value());
        verifyNoInteractions(plans, analysis, auth);
    }
}
