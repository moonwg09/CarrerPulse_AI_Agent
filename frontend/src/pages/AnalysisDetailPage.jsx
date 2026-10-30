import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { confirmNoExperience, getAnalysisDetail } from "../api/analysisApi";

function AnalysisDetailPage() {
  const { analysisId } = useParams();
  const [detail, setDetail] = useState(null);
  const [message, setMessage] = useState("");

  useEffect(() => {
    getAnalysisDetail(analysisId)
      .then(setDetail)
      .catch((error) => setMessage(error.message));
  }, [analysisId]);

  const confirm = async (itemId) => {
    if (!window.confirm("이 요구조건과 관련된 경험이 없다고 확정할까요?")) {
      return;
    }
    try {
      setDetail(await confirmNoExperience(analysisId, itemId));
    } catch (error) {
      setMessage(error.message);
    }
  };

  if (!detail) {
    return <main>{message || "불러오는 중"}</main>;
  }

  const { summary, items } = detail;
  return (
    <main>
      <Link to="/analysis">분석 목록</Link>
      <h1>{summary.jobTitle || `공고 ${summary.jobId}`}</h1>
      <p>{summary.companyName || "회사 미기재"}</p>

      <h2>일치율</h2>
      <ul>
        <li>전체: {summary.overallMatchRate ?? "평가 중"}%</li>
        <li>필수: {summary.requiredMatchRate ?? "평가 불가"}%</li>
        <li>우대: {summary.preferredMatchRate ?? "평가 불가"}%</li>
        <li>객관 조건: {summary.objectiveMatchRate ?? "평가 불가"}%</li>
        <li>평가 가능 비율: {summary.evaluationCoverage ?? "-"}%</li>
      </ul>

      <h2>요구조건별 결과</h2>
      {items.map((item) => (
        <section key={item.itemId}>
          <h3>{item.requirement}</h3>
          <p>{item.requirementCategory} / {item.comparisonKind}</p>
          <p>{item.statusLabel}</p>
          <p>{item.reason || "판정 사유 없음"}</p>
          <p>
            {item.evidenceChunkId
              ? `대표 근거 청크: ${item.evidenceChunkId}`
              : "확인 가능한 대표 근거 없음"}
          </p>
          {item.comparisonKind === "EXPERIENCE" &&
            item.status !== "NO_EXPERIENCE" && (
              <button type="button" onClick={() => confirm(item.itemId)}>
                경험 없음 확인
              </button>
            )}
        </section>
      ))}
      {message && <p>{message}</p>}
    </main>
  );
}

export default AnalysisDetailPage;
