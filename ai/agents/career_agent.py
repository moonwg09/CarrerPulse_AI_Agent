from langgraph.graph import (
    StateGraph,
    START,
    END,
)

from agents.career_agent_state import (
    CareerAgentState,
)

from services.job_analyzer import (
    analyze_job_posting,
)

from services.job_matcher import (
    match_user_to_job,
)

from services.rag_validation_service import (
    validate_match_result_with_rag,
)

from schemas.match_schema import (
    MatchStatus,
)


# -------------------------------------------------
# 1. 공고 분석 Node
# -------------------------------------------------
def analyze_job_node(state):

    print("\n" + "=" * 80)
    print("Agent - 공고 분석 Node")
    print("=" * 80)

    job_text = state["jobText"]

    print("전달받은 jobText 길이:", len(job_text))
    print("전달받은 jobText:")
    print(job_text[:1000])

    job_analysis = analyze_job_posting(
        job_text
    )

    print("공고 분석 결과:")
    print(
        job_analysis.model_dump_json(
            indent=2
        )
    )

    return {
        "jobAnalysis": job_analysis
    }

# -------------------------------------------------
# 2. 1차 Matcher Node
# -------------------------------------------------
def match_job_node(
    state: CareerAgentState,
):
    print()
    print("=" * 80)
    print("Agent - 1차 Matcher Node")
    print("=" * 80)

    match_result = match_user_to_job(
        user_analysis=state["userAnalysis"],
        job_analysis=state["jobAnalysis"],
    )

    return {
        "matchResult": match_result
    }


# -------------------------------------------------
# 3. RAG 2차 검증 Node
# -------------------------------------------------
def rag_validation_node(
    state: CareerAgentState,
):
    print()
    print("=" * 80)
    print("Agent - RAG 2차 검증 Node")
    print("=" * 80)

    validated_result = (
        validate_match_result_with_rag(
            user_analysis=state["userAnalysis"],
            job_analysis=state["jobAnalysis"],
            match_result=state["matchResult"],
            embedded_chunks=state["embeddedChunks"],
            top_k=3,
        )
    )

    return {
        "matchResult": validated_result
    }


# -------------------------------------------------
# 4. LangGraph 생성
# -------------------------------------------------
def build_career_agent():

    builder = StateGraph(
        CareerAgentState
    )

    # Node 등록
    builder.add_node(
        "analyze_job",
        analyze_job_node,
    )

    builder.add_node(
        "match_job",
        match_job_node,
    )

    builder.add_node(
        "rag_validation",
        rag_validation_node,
    )

    # 흐름 연결
    builder.add_edge(
        START,
        "analyze_job",
    )

    builder.add_edge(
        "analyze_job",
        "match_job",
    )

    builder.add_conditional_edges(
        "match_job",
        route_after_match,
        {
            "rag": "rag_validation",
            "end": END,
        },
    )

    builder.add_edge(
        "rag_validation",
        END,
    )

    # 실제 실행 가능한 Graph 생성
    return builder.compile()

# -------------------------------------------------
# RAG 실행 여부 판단
# -------------------------------------------------
def route_after_match(
    state: CareerAgentState,
) -> str:

    match_result = state[
        "matchResult"
    ]

    for item in (
        match_result.requirementMatches
    ):

        # PARTIAL이면 RAG 재검증
        if (
            item.status
            == MatchStatus.PARTIAL
        ):
            print(
                "Agent 판단: "
                "PARTIAL 항목 발견 → RAG 실행"
            )

            return "rag"

        # INSUFFICIENT_EVIDENCE면 RAG 재검증
        if (
            item.status
            == MatchStatus.INSUFFICIENT_EVIDENCE
        ):
            print(
                "Agent 판단: "
                "INSUFFICIENT_EVIDENCE 항목 발견 "
                "→ RAG 실행"
            )

            return "rag"

        # userEvidence 자체가 없으면 RAG 재검증
        if not item.userEvidence:

            print(
                "Agent 판단: "
                "근거 없는 항목 발견 → RAG 실행"
            )

            return "rag"

    print(
        "Agent 판단: "
        "추가 RAG 검증 불필요"
    )

    return "end"