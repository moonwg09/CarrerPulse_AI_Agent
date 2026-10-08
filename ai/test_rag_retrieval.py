from schemas.rag_schema import DocumentChunk

from services.rag_embedding_service import (
    embed_document_chunks,
)

from services.rag_retriever import (
    search_relevant_chunks,
)


def main():

    # ---------------------------------------------
    # 1. 테스트용 사용자 원문 Chunk
    # ---------------------------------------------
    chunks = [
        DocumentChunk(
            chunkId="RESUME_0",
            documentType="RESUME",
            text=(
                "MBook 프로젝트에서 Spring Boot와 JPA를 사용하여 "
                "REST API를 구현했습니다."
            ),
            page=1,
        ),

        DocumentChunk(
            chunkId="RESUME_1",
            documentType="RESUME",
            text=(
                "AWS EC2 환경에 Docker 컨테이너로 서비스를 배포하고 "
                "GitHub Actions를 이용해 CI를 구성했습니다."
            ),
            page=1,
        ),

        DocumentChunk(
            chunkId="RESUME_2",
            documentType="RESUME",
            text=(
                "Python과 Oracle Database를 사용하여 "
                "계좌이체 Transaction을 구현했습니다."
            ),
            page=2,
        ),

        DocumentChunk(
            chunkId="SELF_INTRO_0",
            documentType="SELF_INTRO",
            text=(
                "프로젝트에서 팀원들과 기능 우선순위를 조정하고 "
                "협업하여 실시간 채팅 기능을 구현했습니다."
            ),
            paragraphIndex=3,
        ),

        DocumentChunk(
            chunkId="SELF_INTRO_1",
            documentType="SELF_INTRO",
            text=(
                "Linux 수동 배포, Docker 컨테이너화, AWS 배포, "
                "GitHub Actions CI 순서로 배포 환경을 구성했습니다."
            ),
            paragraphIndex=8,
        ),
    ]

    # ---------------------------------------------
    # 2. 사용자 문서 Chunk embedding
    # ---------------------------------------------
    print("=" * 80)
    print("문서 embedding 생성")
    print("=" * 80)

    embedded_chunks = embed_document_chunks(
        chunks
    )

    print(
        f"embedding 생성 완료: "
        f"{len(embedded_chunks)}개"
    )

    # ---------------------------------------------
    # 3. 테스트 Query
    # ---------------------------------------------
    test_queries = [
        "Spring Boot 기반 REST API 개발 경험",
        "AWS 환경에서 서비스 배포 경험",
        "데이터베이스 트랜잭션 처리 경험",
        "Kubernetes 운영 및 배포 경험",
    ]

    # ---------------------------------------------
    # 4. Query별 Top-3 검색
    # ---------------------------------------------
    for query in test_queries:

        print()
        print("=" * 80)
        print(f"QUERY: {query}")
        print("=" * 80)

        results = search_relevant_chunks(
            query=query,
            embedded_chunks=embedded_chunks,
            top_k=3,
        )

        for rank, result in enumerate(
            results,
            start=1,
        ):

            chunk = result.chunk

            print(
                f"{rank}위 | "
                f"similarity={result.similarity}"
            )

            print(
                f"출처: {chunk.documentType}"
            )

            if chunk.page is not None:
                print(
                    f"페이지: {chunk.page}"
                )

            if (
                chunk.paragraphIndex
                is not None
            ):
                print(
                    f"문단: "
                    f"{chunk.paragraphIndex}"
                )

            print(
                f"내용: {chunk.text}"
            )

            print("-" * 80)


if __name__ == "__main__":
    main()