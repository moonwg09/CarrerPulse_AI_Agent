// 이 파일이 속한 패키지입니다.
package com.careerpulse.application.dto;

// 요청에 면접일이 빠지지 않았는지 검증하는 어노테이션입니다.
import jakarta.validation.constraints.NotNull;

// 연-월-일 형태의 날짜를 표현하는 Java 타입입니다.
import java.time.LocalDate;

/**
 * 면접일 입력·수정 API로 전달받는 요청 데이터입니다.
 * JSON 요청 본문을 이 record 형태로 변환해 받습니다.
 */
public record InterviewDateRequest(

        // 요청에서 면접일은 필수입니다.
        // 값이 없거나 null이면 검증 오류가 발생합니다.
        @NotNull
        LocalDate interviewDate

) {
    // record가 interviewDate 필드, 생성자, interviewDate() 접근 메서드를 자동으로 만들어 줍니다.
}