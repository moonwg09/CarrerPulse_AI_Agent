// 이 클래스가 속한 패키지이다. 학습 기능의 DB 접근 코드를 모아 둔다.
package com.careerpulse.learning.repository;

import com.fasterxml.jackson.databind.JsonNode; // JSON 객체·배열·값을 공통으로 다루는 타입이다.
import com.fasterxml.jackson.databind.ObjectMapper; // JSON 문자열을 JsonNode로 읽는 변환기이다.
import com.fasterxml.jackson.databind.node.ObjectNode; // 필드를 추가할 수 있는 JSON 객체 타입이다.
import org.springframework.jdbc.core.JdbcTemplate; // SQL 실행, 파라미터 바인딩, JDBC 자원 정리를 돕는다.
import org.springframework.jdbc.support.GeneratedKeyHolder; // INSERT 후 DB가 자동 생성한 기본키를 받는다.
import org.springframework.stereotype.Repository; // 이 클래스를 DB 접근용 Spring 빈으로 등록하는 표시이다.
import org.springframework.transaction.annotation.Transactional; // 여러 DB 작업의 성공·실패를 하나로 묶는다.
import org.springframework.http.HttpStatus; // NOT_FOUND 같은 HTTP 상태 코드를 사용한다.
import org.springframework.web.server.ResponseStatusException; // HTTP 상태와 안내 문구를 전달하는 예외이다.
import java.sql.Statement; // 자동 생성 키 반환 옵션인 RETURN_GENERATED_KEYS를 사용한다.
import java.util.List; // 여러 조회 결과를 목록으로 보관한다.

/** 계획 헤더·활동·응답 스냅샷을 한 트랜잭션으로 저장해 부분 저장을 방지한다. */
@Repository // Spring이 객체를 관리하고 DB 관련 예외 변환의 대상으로 인식하게 한다.
public class LearningPlanRepository { // 학습계획을 저장하고 최신 계획을 읽는 클래스의 시작이다.
    private final JdbcTemplate jdbc; // 생성자로 받은 DB 실행 도구를 보관한다. final은 재할당을 막는다.
    private final ObjectMapper mapper; // 저장된 JSON 문자열을 읽을 변환기를 보관한다.
    public LearningPlanRepository(JdbcTemplate jdbc, ObjectMapper mapper) { // Spring이 두 의존성을 주입하는 생성자이다.
        this.jdbc = jdbc; // 전달받은 JdbcTemplate을 이 객체의 필드에 저장한다.
        this.mapper = mapper; // 전달받은 ObjectMapper를 이 객체의 필드에 저장한다.
    } // 생성자 끝이다.

    // Spring 프록시를 통해 호출되면 헤더·활동·스냅샷을 같은 트랜잭션으로 저장한다.
    // 기본 설정에서는 RuntimeException 등이 발생할 때 전체 저장을 롤백한다.
    @Transactional
    public JsonNode save(long userId, JsonNode result) { // 로그인 사용자 ID와 생성된 계획 JSON을 받는다.
        var key = new GeneratedKeyHolder(); // DB가 생성할 plan_id를 담을 객체를 준비한다.
        jdbc.update(connection -> { // 현재 트랜잭션의 DB 연결로 실행할 PreparedStatement를 만든다.
            // INSERT 줄은 저장할 테이블과 네 컬럼을 지정한다.
            // VALUES 줄의 ? 네 개에는 아래 set 메서드로 값을 순서대로 바인딩한다.
            var statement = connection.prepareStatement("""
                INSERT INTO learning_plans(user_id,start_date,end_date,weekly_available_hours)
                VALUES (?,?,?,?)
                """, Statement.RETURN_GENERATED_KEYS); // SQL 문자열을 닫고 자동 생성 키 반환을 요청한다.
            statement.setLong(1, userId); // 첫 번째 ?에 계획 소유자의 사용자 ID를 넣는다.
            statement.setString(2, result.path("startDate").asText()); // 두 번째 ?에 시작일 문자열을 넣는다.
            statement.setString(3, result.path("endDate").asText()); // 세 번째 ?에 종료일 문자열을 넣는다.
            statement.setBigDecimal(4, result.path("weeklyAvailableHours").decimalValue()); // 네 번째 ?에 주당 시간을 십진수로 넣는다.
            return statement; // 준비한 SQL 실행 객체를 JdbcTemplate에 반환한다.
        }, key); // INSERT를 실행하고 반환된 자동 생성 키를 key에 담는다.
        if (key.getKey() == null) throw new IllegalStateException("생성된 계획 ID가 없습니다."); // 키가 없으면 저장을 중단한다.
        long planId = key.getKey().longValue(); // 반환된 Number를 long으로 바꿔 새 계획 ID로 사용한다.
        for (var week : result.path("weeks")) { // 계획의 주차별 JSON 객체를 하나씩 순회한다.
            int sequence = 0; // 주차가 바뀔 때마다 활동 순서 번호를 초기화한다.
            for (var item : week.path("items")) { // 현재 주차에 배치된 활동을 순서대로 순회한다.
                // INSERT 줄은 활동의 소속 계획, 주차, 순서, 유형, 이름, 설명, 시간을 지정한다.
                // VALUES 줄에는 위 일곱 컬럼에 대응하는 ? 일곱 개가 있다.
                jdbc.update("""
                    INSERT INTO learning_plan_items(plan_id,week_number,sequence_no,category,task_name,description,estimated_hours)
                    VALUES (?,?,?,?,?,?,?)
                    """, planId, week.path("weekNumber").asInt(), ++sequence, // 1~3번째 값: 계획 ID, 주차, 1부터 증가하는 수행 순서이다.
                    item.path("activityType").asText(), item.path("taskName").asText(), // 4~5번째 값: 활동 유형과 활동 이름이다.
                    item.path("description").asText(), item.path("estimatedHours").decimalValue()); // 6~7번째 값: 설명과 예상 시간이다.
            } // 현재 주차의 모든 활동 저장을 마친다.
        } // 모든 주차의 활동 저장을 마친다.
        ObjectNode snapshot = result.deepCopy(); // 원본 JSON을 바꾸지 않도록 객체 전체를 복사한다.
        snapshot.put("planId", planId); // 복사본에 DB에서 발급한 계획 ID를 추가한다.
        // 날짜, 주당 가능 시간, 배치되지 못한 활동도 함께 저장하므로 재조회 때 손실되지 않는다.
        jdbc.update("INSERT INTO learning_plan_snapshots(plan_id,snapshot) VALUES (?,?)", planId, snapshot.toString()); // 계획 ID와 전체 JSON 문자열을 저장한다.
        return snapshot; // 계획 ID가 포함된 결과를 호출자에게 돌려준다. 커밋은 트랜잭션 경계에서 처리된다.
    } // save 메서드 끝이다.

    @Transactional(readOnly = true) // 조회용 트랜잭션이라는 힌트이다. 사용자 접근 제한은 아래 SQL 조건이 담당한다.
    public JsonNode latest(long userId) { // 전달받은 사용자 소유의 최신 ACTIVE 계획을 조회한다.
        // SELECT 줄: p는 계획 테이블의 별칭이며, 반환할 값은 스냅샷 JSON 하나이다.
        // JOIN 줄: 같은 plan_id의 계획과 스냅샷을 연결한다. 스냅샷 없는 계획은 제외한다.
        // WHERE 줄: 소유자와 ACTIVE 상태로 제한하고, ID 내림차순의 첫 한 건만 가져온다.
        List<String> values = jdbc.query("""
            SELECT s.snapshot FROM learning_plans p
            JOIN learning_plan_snapshots s ON s.plan_id=p.plan_id
            WHERE p.user_id=? AND p.status='ACTIVE' ORDER BY p.plan_id DESC LIMIT 1
            """, (rs, row) -> rs.getString(1), userId); // ?에 userId를 넣고 각 결과 행의 첫 컬럼을 문자열로 읽는다.
        // 타 사용자 소유 계획은 쿼리 자체에서 제외한다. 계획이 없을 때는 404로 안내한다.
        if (values.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, // 조회 결과가 없으면 404 예외를 발생시킨다.
                "생성된 학습계획이 없습니다. 시작일·종료일·주당 학습 가능 시간을 입력해 계획을 생성하세요."); // 화면에 전달할 안내 문구이다.
        try { // 저장된 JSON 문자열을 읽는 과정에서 발생할 수 있는 오류를 처리한다.
            return mapper.readTree(values.getFirst()); // 첫 조회 결과를 JsonNode로 변환해 반환한다. Gemini는 호출하지 않는다.
        } catch (java.io.IOException ex) { // 저장된 문자열을 JSON으로 읽지 못했을 때 실행한다.
            throw new IllegalStateException("저장된 학습계획을 읽지 못했습니다.", ex); // 원인 예외를 보존한 채 처리 실패를 알린다.
        } // JSON 변환 예외 처리 끝이다.
    } // latest 메서드 끝이다.
} // LearningPlanRepository 클래스 끝이다.
