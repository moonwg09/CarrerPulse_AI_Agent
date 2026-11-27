from schemas.raw_job_schema import RawJobPosting


def format_raw_job_for_analysis(job: RawJobPosting) -> str:
    lines = [
        f"회사명: {job.companyName}",
        f"채용공고 제목: {job.title}",
    ]

    if job.region:
        lines.append(f"근무지역: {job.region}")
    if job.closeDate:
        lines.append(f"접수 마감일: {job.closeDate.isoformat()}")

    lines.extend(["", job.content.strip()])
    return "\n".join(lines).strip()
