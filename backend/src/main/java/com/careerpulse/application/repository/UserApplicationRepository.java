// 이 Repository가 속한 패키지입니다.
package com.careerpulse.application.repository;

// DB의 user_applications 행을 Java 객체로 다루는 Entity입니다.
import com.careerpulse.application.entity.UserApplication;

// 기본 조회·저장·수정 기능을 제공하는 Spring Data JPA Repository입니다.
import org.springframework.data.jpa.repository.JpaRepository;

// 회사명과 직무명을 포함한 조인 조회 결과를 받을 인터페이스입니다.
import com.careerpulse.application.dto.UserApplicationView;

// 직접 작성한 SQL을 Repository 메서드에 연결하는 어노테이션입니다.
import org.springframework.data.jpa.repository.Query;

// SQL 안의 :userId에 메서드 매개변수를 연결할 때 사용합니다.
import org.springframework.data.repository.query.Param;

// 여러 건을 목록으로 반환할 때 사용하는 타입입니다.
import java.util.List;

// 조회 결과가 없을 수도 있는 단건 조회에 사용하는 타입입니다.
import java.util.Optional;

/**
 * UserApplication Entity의 DB 조회와 저장을 담당합니다.
 *
 * JpaRepository를 상속하므로 기본적인 CRUD 메서드도 사용할 수 있습니다.
 * 예: findById(), findAll(), save(), deleteById()
 */
public interface UserApplicationRepository
        extends JpaRepository<UserApplication, Long> {

    /**
     * 특정 사용자의 지원 내역을 면접일 오름차순으로 조회합니다.
     *
     * 메서드 이름을 기준으로 Spring Data JPA가 조회 SQL을 만듭니다.
     * User_UserId는 UserApplication의 user 필드 안에 있는 userId를 뜻합니다.
     * 날짜가 없는 행은 DB 정렬 규칙에 따라 목록 뒤쪽에 올 수 있습니다.
     */
    List<UserApplication> findByUser_UserIdOrderByInterviewDateAsc(Long userId);

    /**
     * 지원 ID와 사용자 ID가 모두 일치하는 지원 내역 하나를 찾습니다.
     *
     * Optional은 결과가 없을 수도 있음을 나타냅니다.
     * 사용자 ID도 조건에 넣어 다른 사용자의 지원 내역을 찾지 않도록 합니다.
     */
    Optional<UserApplication> findByApplicationIdAndUser_UserId(
            Long applicationId,
            Long userId
    );

    /** 로그인 이메일과 지원 ID가 모두 일치하는 지원 내역만 찾습니다. */
    Optional<UserApplication> findByApplicationIdAndUser_Email(
            Long applicationId,
            String email
    );

    /**
     * 면접일이 등록된 지원 내역만 날짜 오름차순으로 조회합니다.
     *
     * IsNotNull은 interviewDate가 null이 아닌 행만 선택한다는 뜻입니다.
     */
    List<UserApplication>
    findByUser_UserIdAndInterviewDateIsNotNullOrderByInterviewDateAsc(Long userId);

    /**
     * 지원 내역에 채용공고 정보를 붙여 조회하는 SQL입니다.
     *
     * nativeQuery = true는 JPQL이 아니라 실제 DB 테이블·컬럼 이름을 쓰는
     * SQL이라는 뜻입니다.
     *
     * job_postings와 조인해 회사명과 공고 제목도 가져옵니다.
     * 각 AS 뒤의 별칭은 UserApplicationView의 getter 이름과 연결됩니다.
     */
    @Query(value = """
        SELECT
            ua.application_id AS applicationId,
            ua.job_id AS jobId,
            jp.company_name AS companyName,
            jp.title AS title,
            ua.application_status AS applicationStatus,
            ua.interview_date AS interviewDate
        FROM user_applications ua
        JOIN job_postings jp ON jp.job_id = ua.job_id
        WHERE ua.user_id = :userId
        ORDER BY ua.interview_date IS NULL, ua.interview_date
        """, nativeQuery = true)

    /**
     * 위 SQL을 실행하고 조회 결과를 UserApplicationView 목록으로 반환합니다.
     *
     * @Param("userId")가 SQL의 :userId 자리에 메서드 인자를 넣습니다.
     */
    List<UserApplicationView> findViewsByUserId(
            @Param("userId") Long userId
    );
}
