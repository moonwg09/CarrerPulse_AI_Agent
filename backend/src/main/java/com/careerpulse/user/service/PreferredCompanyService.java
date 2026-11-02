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
        if (repository.existsByUser_UserIdAndCompanyNameIgnoreCase(user.getUserId(), name)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 등록된 관심 기업입니다.");
        }
        String verifiedName;
        try {
            verifiedName = jobCollectionClient.searchCompanies(name).stream()
                    .filter(candidate -> candidate.equalsIgnoreCase(name))
                    .findFirst()
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "고용24 검색 결과에서 정확한 기업명을 선택해 주세요."
                    ));
        } catch (JobCollectionClientException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "고용24 기업 검색을 확인할 수 없습니다. 잠시 후 다시 시도해 주세요.",
                    exception
            );
        }
        return response(repository.save(new UserPreferredCompany(user, verifiedName)));
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

    private PreferredCompanyResponse response(UserPreferredCompany company) {
        return new PreferredCompanyResponse(
                company.getPreferredCompanyId(),
                company.getCompanyName()
        );
    }
}
