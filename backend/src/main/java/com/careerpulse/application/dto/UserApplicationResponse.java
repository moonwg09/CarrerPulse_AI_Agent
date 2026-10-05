// 이 파일이 속한 패키지입니다.
package com.careerpulse.application.dto;

// 면접일처럼 연-월-일로 표현하는 날짜 타입입니다.
import java.time.LocalDate;

/**
 * 지원 내역 조회 결과를 화면이나 클라이언트에 전달하는 DTO입니다.
 * DTO는 필요한 데이터를 담아 전달하는 객체입니다.
 */
public record UserApplicationResponse(

        // 지원 내역의 고유 ID
        Long applicationId,

        // 연결된 채용 공고의 ID
        Long jobId,

        // 회사 이름
        String companyName,

        // 채용 공고의 직무명 또는 제목
        String title,

        // 지원 상태 (예: APPLIED, INTERVIEW, FINAL_PASS)
        String applicationStatus,

        // 면접 예정일. 날짜가 정해지지 않았으면 null일 수 있습니다.
        LocalDate interviewDate

) {
    // record는 위에 선언한 항목들의 생성자와 getter를 자동으로 만들어 줍니다.
}