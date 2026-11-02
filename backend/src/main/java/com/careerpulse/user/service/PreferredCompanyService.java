package com.careerpulse.user.service;

import com.careerpulse.user.dto.PreferredCompanyRequest;
import com.careerpulse.user.dto.PreferredCompanyResponse;
import com.careerpulse.user.entity.User;
import com.careerpulse.user.entity.UserPreferredCompany;
import com.careerpulse.user.repository.UserPreferredCompanyRepository;
import com.careerpulse.user.repository.UserRepository;
import com.careerpulse.job.client.JobCollectionClient;
import com.careerpulse.job.client.JobCollectionClientException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;

@Service
public class PreferredCompanyService {
    private final UserRepository userRepository;
    private final UserPreferredCompanyRepository repository;
    private final JobCollectionClient jobCollectionClient;

    public PreferredCompanyService(
            UserRepository userRepository,
            UserPreferredCompanyRepository repository,
            JobCollectionClient jobCollectionClient
    ) {
        this.userRepository = userRepository;
        this.repository = repository;
        this.jobCollectionClient = jobCollectionClient;
    }

    @Transactional(readOnly = true)
    public List<PreferredCompanyResponse> list(String email) {
        User user = user(email);
        return repository.findAllByUser_UserIdOrderByCreatedAtAsc(user.getUserId())
                .stream().map(this::response).toList();
    }

    @Transactional
    public PreferredCompanyResponse add(String email, PreferredCompanyRequest request) {
        User user = user(email);
        String name = request.companyName().trim();
        if (repository.countByUser_UserId(user.getUserId()) >= 3) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "관심 기업은 최대 3개까지 등록할 수 있습니다."
            );
        }
        String canonicalName = canonicalCompanyName(name);
        if (repository.existsByUser_UserIdAndCompanyNameIgnoreCase(user.getUserId(), canonicalName)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 등록된 관심 기업입니다.");
        }
        return response(repository.save(new UserPreferredCompany(user, canonicalName)));
    }

    @Transactional
    public void delete(String email, Long id) {
        User user = user(email);
        UserPreferredCompany company = repository
                .findByPreferredCompanyIdAndUser_UserId(id, user.getUserId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "관심 기업을 찾을 수 없습니다."
                ));
        repository.delete(company);
    }

    private User user(String email) {
        return userRepository.findByEmail(email).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));
    }

    private String canonicalCompanyName(String inputName) {
        if (inputName.length() < 2) {
            return inputName;
        }
        String normalizedInput = normalizeCompanyName(inputName);
        try {
            return jobCollectionClient.searchCompanies(inputName).stream()
                    .filter(candidate -> normalizeCompanyName(candidate).equals(normalizedInput))
                    .findFirst()
                    .orElse(inputName);
        } catch (JobCollectionClientException exception) {
            return inputName;
        }
    }

    private String normalizeCompanyName(String value) {
        return value
                .replaceAll("(?i)주식회사|\\(주\\)|㈜", "")
                .replaceAll("[^\\p{L}\\p{N}]", "")
                .toLowerCase(Locale.KOREAN);
    }

    private PreferredCompanyResponse response(UserPreferredCompany company) {
        return new PreferredCompanyResponse(
                company.getPreferredCompanyId(),
                company.getCompanyName()
        );
    }
}
