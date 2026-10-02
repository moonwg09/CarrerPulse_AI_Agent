package com.careerpulse.user.service;

import com.careerpulse.user.dto.UpdateUserRequest;
import com.careerpulse.user.dto.UserJobRequest;
import com.careerpulse.user.dto.UserJobResponse;
import com.careerpulse.user.dto.UserResponse;
import com.careerpulse.user.entity.User;
import com.careerpulse.user.entity.UserJob;
import com.careerpulse.user.repository.UserJobRepository;
import com.careerpulse.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserJobRepository userJobRepository;

    public UserService(
            UserRepository userRepository,
            UserJobRepository userJobRepository
    ){
        this.userRepository = userRepository;
        this.userJobRepository = userJobRepository;
    }

    @Transactional(readOnly = true)
    public UserResponse getMyInfo(String email){

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "사용자를 찾을 수 없습니다."
                        )
                );

        return new UserResponse(
                user.getUserId(),
                user.getEmail(),
                user.getName(),
                user.getPhone(),
                user.getCareerType(),
                user.getCareerYears(),
                user.getAccountStatus()
        );
    }

    @Transactional
    public UserResponse updateMyInfo(
            String email,
            UpdateUserRequest request
    ){

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "사용자를 찾을 수 없습니다."
                        )
                );

        if ("EXPERIENCED".equals(request.careerType())
                && request.careerYears() == null) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "경력자는 경력연수를 입력해야 합니다."
            );
        }

        if ("NEW".equals(request.careerType())) {
            user.updateProfile(
                    request.name(),
                    request.phone(),
                    request.careerType(),
                    null
            );
        } else {
            user.updateProfile(
                    request.name(),
                    request.phone(),
                    request.careerType(),
                    request.careerYears()
            );
        }

        return new UserResponse(
                user.getUserId(),
                user.getEmail(),
                user.getName(),
                user.getPhone(),
                user.getCareerType(),
                user.getCareerYears(),
                user.getAccountStatus()
        );
    }
    @Transactional
    public UserJobResponse addUserJob(
            String email,
            UserJobRequest request
    ) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "사용자를 찾을 수 없습니다."
                        )
                );

        boolean exists =
                userJobRepository.existsByUser_UserIdAndJobCode(
                        user.getUserId(),
                        request.jobCode()
                );

        if (exists) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "이미 등록된 관심 직무입니다."
            );
        }

        UserJob userJob = new UserJob(
                user,
                request.jobCode(),
                request.jobName()
        );

        UserJob savedJob =
                userJobRepository.save(userJob);

        return new UserJobResponse(
                savedJob.getUserJobId(),
                savedJob.getJobCode(),
                savedJob.getJobName()
        );
    }

    @Transactional(readOnly = true)
    public List<UserJobResponse> getMyJobs(
            String email
    ) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "사용자를 찾을 수 없습니다."
                        )
                );

        return userJobRepository
                .findAllByUser_UserId(
                        user.getUserId()
                )
                .stream()
                .map(job ->
                        new UserJobResponse(
                                job.getUserJobId(),
                                job.getJobCode(),
                                job.getJobName()
                        )
                )
                .toList();
    }

    @Transactional
    public void deleteMyJob(
            String email,
            Long userJobId
    ) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "사용자를 찾을 수 없습니다."
                        )
                );

        UserJob userJob =
                userJobRepository
                        .findByUserJobIdAndUser_UserId(
                                userJobId,
                                user.getUserId()
                        )
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "관심 직무를 찾을 수 없습니다."
                                )
                        );

        userJobRepository.delete(userJob);
    }

    @Transactional
    public void withdraw(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "사용자를 찾을 수 없습니다."
                        )
                );
        user.withdraw();
    }

}
