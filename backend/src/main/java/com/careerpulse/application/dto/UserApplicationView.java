// 이 파일이 속한 패키지입니다.
package com.careerpulse.application.dto;

// 면접일을 연-월-일 형태로 표현하는 Java 타입입니다.
import java.time.LocalDate;

/**
 * Repository의 조회 결과에서 필요한 값만 읽기 위한 인터페이스입니다.
 * SQL 조회 결과의 각 컬럼 별칭과 아래 메서드 이름이 연결됩니다.
 */
public interface UserApplicationView {

    // SQL의 applicationId 별칭에 해당하는 지원 내역 ID
    Long getApplicationId();

    // SQL의 jobId 별칭에 해당하는 채용 공고 ID
    Long getJobId();

    // SQL의 companyName 별칭에 해당하는 회사명
    String getCompanyName();

    // SQL의 title 별칭에 해당하는 공고 제목 또는 직무명
    String getTitle();

    // SQL의 applicationStatus 별칭에 해당하는 지원 상태
    String getApplicationStatus();

    // SQL의 interviewDate 별칭에 해당하는 면접일
    LocalDate getInterviewDate();
}