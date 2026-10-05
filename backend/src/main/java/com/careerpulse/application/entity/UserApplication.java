// 이 Entity가 속한 패키지입니다.
package com.careerpulse.application.entity;

// 지원 내역의 사용자 정보를 연결할 User Entity입니다.
import com.careerpulse.user.entity.User;

// JPA의 Entity, 테이블, 컬럼, 관계 설정 어노테이션을 가져옵니다.
import jakarta.persistence.*;

// 면접일처럼 연-월-일만 나타내는 날짜 타입입니다.
import java.time.LocalDate;

// 생성일·수정일처럼 날짜와 시간을 함께 나타내는 타입입니다.
import java.time.LocalDateTime;

/**
 * user_applications 테이블과 연결되는 JPA Entity입니다.
 * Entity는 DB 행 하나를 Java 객체로 표현합니다.
 */
@Entity
@Table(
        // 연결할 실제 데이터베이스 테이블 이름
        name = "user_applications",

        // 같은 사용자가 같은 공고에 중복 지원하지 못하게 하는 제약 조건
        uniqueConstraints = {
                @UniqueConstraint(
                        // 데이터베이스에 정의된 제약 조건 이름
                        name = "uk_user_application",

                        // 이 두 컬럼의 조합이 중복되면 안 됩니다.
                        columnNames = {"user_id", "job_id"}
                )
        }
)
public class UserApplication {

    // 이 행의 기본 키입니다.
    @Id

    // 새 행을 저장할 때 MySQL이 ID 값을 자동으로 생성합니다.
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "application_id")
    private Long applicationId;

    // 지원 내역과 사용자(User) 사이의 다대일 관계입니다.
    // 여러 지원 내역이 한 명의 사용자를 가리킬 수 있습니다.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 연결된 채용 공고의 ID입니다.
    // 현재 프로젝트에는 JobPosting Entity가 없어 숫자 ID로 저장합니다.
    @Column(name = "job_id", nullable = false)
    private Long jobId;

    // 지원 상태입니다. 새 객체의 기본 상태는 APPLIED입니다.
    @Column(name = "application_status", nullable = false, length = 30)
    private String applicationStatus = "APPLIED";

    // 면접 예정일입니다. 아직 정해지지 않았으면 null일 수 있습니다.
    @Column(name = "interview_date")
    private LocalDate interviewDate;

    // DB가 자동으로 기록하는 생성 시각입니다.
    // insertable=false, updatable=false는 JPA가 이 값을 쓰거나 수정하지 않게 합니다.
    @Column(
            name = "created_at",
            insertable = false,
            updatable = false
    )
    private LocalDateTime createdAt;

    // DB가 자동으로 기록하고 갱신하는 수정 시각입니다.
    // 이 Entity에서는 JPA가 값을 직접 변경하지 않습니다.
    @Column(
            name = "updated_at",
            insertable = false,
            updatable = false
    )
    private LocalDateTime updatedAt;

    // JPA가 DB에서 읽어온 값을 채울 때 사용하는 기본 생성자입니다.
    // protected로 두어 일반 코드에서 함부로 빈 객체를 만들지 못하게 합니다.
    protected UserApplication() {
    }

    // 새 지원 내역 객체를 만들 때 사용자와 공고 ID를 지정합니다.
    public UserApplication(User user, Long jobId) {
        this.user = user;
        this.jobId = jobId;
    }

    // 면접일을 입력하거나 수정합니다.
    public void updateInterviewDate(LocalDate interviewDate) {
        this.interviewDate = interviewDate;
    }

    // 아래 getter들은 각 필드 값을 읽기 위한 메서드입니다.

    public Long getApplicationId() {
        return applicationId;
    }

    public User getUser() {
        return user;
    }

    public Long getJobId() {
        return jobId;
    }

    public String getApplicationStatus() {
        return applicationStatus;
    }

    public LocalDate getInterviewDate() {
        return interviewDate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}