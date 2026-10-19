INSERT INTO interview_question (category, question_type, content)
SELECT '공통', '인성', '1분 동안 자기소개를 해주세요.'
WHERE NOT EXISTS (
    SELECT 1 FROM interview_question
    WHERE category = '공통' AND question_type = '인성'
      AND content = '1분 동안 자기소개를 해주세요.'
);

INSERT INTO interview_question (category, question_type, content)
SELECT '공통', '인성', '본인의 장점과 단점을 말씀해 주세요.'
WHERE NOT EXISTS (
    SELECT 1 FROM interview_question
    WHERE category = '공통' AND question_type = '인성'
      AND content = '본인의 장점과 단점을 말씀해 주세요.'
);

INSERT INTO interview_question (category, question_type, content)
SELECT '공통', '인성', '우리 회사에 지원한 이유는 무엇인가요?'
WHERE NOT EXISTS (
    SELECT 1 FROM interview_question
    WHERE category = '공통' AND question_type = '인성'
      AND content = '우리 회사에 지원한 이유는 무엇인가요?'
);

INSERT INTO interview_question (category, question_type, content)
SELECT '사무행정', '직무', '문서 작업이나 엑셀로 업무 효율을 높였던 경험이 있나요?'
WHERE NOT EXISTS (
    SELECT 1 FROM interview_question
    WHERE category = '사무행정' AND question_type = '직무'
      AND content = '문서 작업이나 엑셀로 업무 효율을 높였던 경험이 있나요?'
);

INSERT INTO interview_question (category, question_type, content)
SELECT '사무행정', '상황', '업무가 한꺼번에 몰릴 때 우선순위를 어떻게 정하시나요?'
WHERE NOT EXISTS (
    SELECT 1 FROM interview_question
    WHERE category = '사무행정' AND question_type = '상황'
      AND content = '업무가 한꺼번에 몰릴 때 우선순위를 어떻게 정하시나요?'
);

INSERT INTO interview_question (category, question_type, content)
SELECT 'IT개발', '직무', '최근 가장 인상 깊게 사용한 기술과 그 이유를 말씀해 주세요.'
WHERE NOT EXISTS (
    SELECT 1 FROM interview_question
    WHERE category = 'IT개발' AND question_type = '직무'
      AND content = '최근 가장 인상 깊게 사용한 기술과 그 이유를 말씀해 주세요.'
);

INSERT INTO interview_question (category, question_type, content)
SELECT 'IT개발', '상황', '팀원과 기술적 의견이 다를 때 어떻게 해결하시겠어요?'
WHERE NOT EXISTS (
    SELECT 1 FROM interview_question
    WHERE category = 'IT개발' AND question_type = '상황'
      AND content = '팀원과 기술적 의견이 다를 때 어떻게 해결하시겠어요?'
);

INSERT INTO interview_question (category, question_type, content)
SELECT '고객서비스', '상황', '화가 난 고객을 응대했던 경험을 말씀해 주세요.'
WHERE NOT EXISTS (
    SELECT 1 FROM interview_question
    WHERE category = '고객서비스' AND question_type = '상황'
      AND content = '화가 난 고객을 응대했던 경험을 말씀해 주세요.'
);
