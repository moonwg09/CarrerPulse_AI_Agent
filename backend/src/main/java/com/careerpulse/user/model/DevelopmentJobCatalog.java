package com.careerpulse.user.model;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;

public enum DevelopmentJobCatalog {
    BACKEND("백엔드 개발자", "백엔드 개발자", List.of("백엔드", "서버", "Backend")),
    FRONTEND(
            "프론트엔드 개발자",
            "프론트엔드 개발자",
            List.of("React", "JavaScript", "Frontend", "웹 개발")
    ),
    FULLSTACK("풀스택 개발자", "풀스택 개발자", List.of("풀스택", "웹개발", "Full Stack")),
    MOBILE("모바일 앱 개발자", "앱 개발자", List.of("앱개발", "Android", "iOS", "모바일")),
    SOFTWARE("소프트웨어·시스템 개발자", "소프트웨어 개발자", List.of("소프트웨어", "시스템 SW")),
    EMBEDDED("임베디드·펌웨어 개발자", "임베디드 개발자", List.of("임베디드", "펌웨어")),
    DATA_ENGINEER("데이터 엔지니어", "데이터 엔지니어", List.of("데이터엔지니어", "데이터 파이프라인", "ETL")),
    DATA_SCIENCE("데이터 분석가·사이언티스트", "데이터 분석가", List.of("데이터분석", "데이터사이언티스트", "통계")),
    AI_ML("AI·머신러닝 엔지니어", "AI 엔지니어", List.of("AI", "머신러닝", "딥러닝")),
    DEVOPS_CLOUD("DevOps·클라우드 엔지니어", "DevOps 엔지니어", List.of("DevOps", "클라우드", "SRE")),
    SYSTEM_NETWORK("시스템·네트워크 엔지니어", "시스템 엔지니어", List.of("시스템엔지니어", "네트워크")),
    QA_TEST("QA·테스트 엔지니어", "QA 엔지니어", List.of("QA", "테스터", "테스트 자동화")),
    SECURITY("정보보안 엔지니어", "정보보안 엔지니어", List.of("정보보안", "보안엔지니어", "보안관제", "침해대응"));

    private final String displayName;
    private final String searchKeyword;
    private final List<String> searchExpressions;

    DevelopmentJobCatalog(String displayName, String searchKeyword, List<String> searchExpressions) {
        this.displayName = displayName;
        this.searchKeyword = searchKeyword;
        this.searchExpressions = searchExpressions;
    }

    public String displayName() { return displayName; }
    public String searchKeyword() { return searchKeyword; }
    public List<String> searchExpressions() { return searchExpressions; }

    public List<String> searchTerms() {
        LinkedHashSet<String> terms = new LinkedHashSet<>();
        terms.add(searchKeyword);
        terms.add(displayName);
        terms.addAll(searchExpressions);
        return List.copyOf(terms);
    }

    public static DevelopmentJobCatalog fromCode(String code) {
        if (code == null) throw new IllegalArgumentException("직무 코드가 필요합니다.");
        return Arrays.stream(values())
                .filter(value -> value.name().equalsIgnoreCase(code.trim()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("지원하지 않는 개발 직무입니다."));
    }
}
