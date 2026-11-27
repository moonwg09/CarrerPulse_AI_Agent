package com.careerpulse.user.service;

import com.careerpulse.job.client.JobCollectionClient;
import com.careerpulse.user.dto.PreferredCompanyRequest;
import com.careerpulse.user.dto.PreferredCompanyResponse;
import com.careerpulse.user.entity.User;
import com.careerpulse.user.repository.UserPreferredCompanyRepository;
import com.careerpulse.user.repository.UserRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PreferredCompanyServiceTest {

    @Test
    void usesCanonicalWork24NameWhenNormalizedNamesMatch() {
        UserRepository userRepository = mock(UserRepository.class);
        UserPreferredCompanyRepository companyRepository =
                mock(UserPreferredCompanyRepository.class);
        JobCollectionClient jobCollectionClient = mock(JobCollectionClient.class);
        User user = mock(User.class);

        when(user.getUserId()).thenReturn(1L);
        when(userRepository.findByEmail("user@example.com"))
                .thenReturn(Optional.of(user));
        when(companyRepository.countByUser_UserId(1L)).thenReturn(0L);
        when(companyRepository.existsByUser_UserIdAndCompanyNameIgnoreCase(
                1L,
                "주식회사 빅앤딥"
        )).thenReturn(false);
        when(jobCollectionClient.searchCompanies("빅앤딥"))
                .thenReturn(List.of("주식회사 빅앤딥"));
        when(companyRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        PreferredCompanyResponse result = new PreferredCompanyService(
                userRepository,
                companyRepository,
                jobCollectionClient
        ).add("user@example.com", new PreferredCompanyRequest("빅앤딥"));

        assertThat(result.companyName()).isEqualTo("주식회사 빅앤딥");
    }
}
