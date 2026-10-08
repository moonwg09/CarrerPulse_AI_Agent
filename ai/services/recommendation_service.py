from datetime import date
from typing import List

from schemas.match_schema import RecommendationStatus
from schemas.recommendation_schema import JobRecommendationItem


def build_recommendation_list(
    jobs: List[JobRecommendationItem]
) -> List[JobRecommendationItem]:

    today = date.today()

    # 1. 마감된 공고 제거
    valid_jobs = [
        job
        for job in jobs
        if job.closeDate >= today
    ]

    # 2. 중복 공고 제거
    unique_jobs = []
    seen_job_ids = set()

    for job in valid_jobs:
        if job.jobId in seen_job_ids:
            continue

        seen_job_ids.add(job.jobId)
        unique_jobs.append(job)

    # 3. 추천 공고만 선택
    recommended_jobs = [
        job
        for job in unique_jobs
        if job.matchResult.recommendationStatus
        == RecommendationStatus.RECOMMENDED
    ]

    # 4. 전체 일치율 높은 순
    #    동률이면 마감일 가까운 순
    recommended_jobs.sort(
        key=lambda job: (
            -job.matchResult.score.overallMatchRate,
            job.closeDate
        )
    )

    # 5. 최대 10개
    return recommended_jobs[:10]

def build_growth_candidate_list(
    jobs: List[JobRecommendationItem],
    limit: int = 5
) -> List[JobRecommendationItem]:

    today = date.today()

    # 1. 마감된 공고 제거
    valid_jobs = [
        job
        for job in jobs
        if job.closeDate >= today
    ]

    # 2. 중복 공고 제거
    unique_jobs = []
    seen_job_ids = set()

    for job in valid_jobs:
        if job.jobId in seen_job_ids:
            continue

        seen_job_ids.add(job.jobId)
        unique_jobs.append(job)

    # 3. 성장 후보 공고만 선택
    growth_candidates = [
        job
        for job in unique_jobs
        if job.matchResult.recommendationStatus
        == RecommendationStatus.GROWTH_CANDIDATE
    ]

    # 4. 전체 일치율 높은 순
    #    동률이면 필수 충족률 높은 순
    #    그다음 평가 가능 비율 높은 순
    #    마지막으로 마감일 가까운 순
    growth_candidates.sort(
        key=lambda job: (
            -job.matchResult.score.overallMatchRate,
            -job.matchResult.score.requiredMatchRate,
            -job.matchResult.score.evaluationCoverage,
            job.closeDate
        )
    )

    # 5. 최대 limit개
    return growth_candidates[:limit]

if __name__ == "__main__":
    from datetime import date

    from schemas.match_schema import (
        JobMatchResult,
        MatchScoreResult,
        RecommendationStatus,
    )

    jobs = [
        JobRecommendationItem(
            jobId="job-1",  # 기존 A회사와 같은 ID
            closeDate=date(2026, 10, 25),
            matchResult=JobMatchResult(
                jobTitle="중복 공고",
                companyName="A회사",
                score=MatchScoreResult(
                    overallMatchRate=95.0,
                    requiredMatchRate=100.0,
                    preferredMatchRate=90.0,
                    objectiveMatchRate=80.0,
                    evaluationCoverage=100.0,
                ),
                recommendationStatus=RecommendationStatus.RECOMMENDED,
            ),
        ),

        JobRecommendationItem(
            jobId="job-2",
            closeDate=date(2026, 10, 12),
            matchResult=JobMatchResult(
                jobTitle="백엔드 개발자 B",
                companyName="B회사",
                score=MatchScoreResult(
                    overallMatchRate=82.0,
                    requiredMatchRate=90.0,
                    preferredMatchRate=60.0,
                    objectiveMatchRate=70.0,
                    evaluationCoverage=100.0,
                ),
                recommendationStatus=RecommendationStatus.RECOMMENDED,
            ),
        ),

        JobRecommendationItem(
            jobId="job-3",
            closeDate=date(2026, 10, 10),
            matchResult=JobMatchResult(
                jobTitle="백엔드 개발자 C",
                companyName="C회사",
                score=MatchScoreResult(
                    overallMatchRate=91.0,
                    requiredMatchRate=100.0,
                    preferredMatchRate=80.0,
                    objectiveMatchRate=80.0,
                    evaluationCoverage=100.0,
                ),
                recommendationStatus=RecommendationStatus.RECOMMENDED,
            ),
        ),

        JobRecommendationItem(
            jobId="job-4",
            closeDate=date(2026, 10, 8),
            matchResult=JobMatchResult(
                jobTitle="백엔드 개발자 D",
                companyName="D회사",
                score=MatchScoreResult(
                    overallMatchRate=48.0,
                    requiredMatchRate=70.0,
                    preferredMatchRate=30.0,
                    objectiveMatchRate=50.0,
                    evaluationCoverage=90.0,
                ),
                recommendationStatus=RecommendationStatus.GROWTH_CANDIDATE,
            ),
        ),

        JobRecommendationItem(
            jobId="job-5",
            closeDate=date(2026, 10, 1),
            matchResult=JobMatchResult(
                jobTitle="마감된 공고",
                companyName="E회사",
                score=MatchScoreResult(
                    overallMatchRate=99.0,
                    requiredMatchRate=100.0,
                    preferredMatchRate=100.0,
                    objectiveMatchRate=100.0,
                    evaluationCoverage=100.0,
                ),
                recommendationStatus=RecommendationStatus.RECOMMENDED,
            ),
        ),

        JobRecommendationItem(
            jobId="job-1",  # 위 A회사와 동일한 ID
            closeDate=date(2026, 10, 30),
            matchResult=JobMatchResult(
                jobTitle="백엔드 개발자 A 중복",
                companyName="A회사 중복",
                score=MatchScoreResult(
                    overallMatchRate=99.0,
                    requiredMatchRate=100.0,
                    preferredMatchRate=100.0,
                    objectiveMatchRate=100.0,
                    evaluationCoverage=100.0,
                ),
                recommendationStatus=RecommendationStatus.RECOMMENDED,
            ),
        ),
    ]

    recommended_result = build_recommendation_list(jobs)
    growth_result = build_growth_candidate_list(jobs)

    print("추천 공고")
    for index, job in enumerate(recommended_result, start=1):
        print(
            index,
            job.matchResult.companyName,
            job.matchResult.score.overallMatchRate,
            job.closeDate,
        )

    print()

    print("성장 후보 공고")
    for index, job in enumerate(growth_result, start=1):
        print(
            index,
            job.matchResult.companyName,
            job.matchResult.score.overallMatchRate,
            job.closeDate,
        )