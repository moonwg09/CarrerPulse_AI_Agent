import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { confirmNoExperience, generateGuidance, getAnalysisDetail, rejudgeExperience } from "../api/analysisApi";

function AnalysisDetailPage() {
  const { analysisId } = useParams();
  const [detail, setDetail] = useState(null);
  const [message, setMessage] = useState("");
  const [guidanceByItem, setGuidanceByItem] = useState({});
  const [loadingItemId, setLoadingItemId] = useState(null);
  const [responses, setResponses] = useState({});

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

  const rejudge = async (itemId) => {
    const response = responses[itemId] || {};
    if (!response.userResponseType || !response.userExperienceText?.trim()) {
      setMessage("경험 구분과 수행 내용을 입력해 주세요.");
      return;
    }
    try {
      setLoadingItemId(itemId);
      setDetail(await rejudgeExperience(analysisId, itemId, response));
      setMessage("추가 경험을 반영해 다시 판정했습니다.");
    } catch (error) {
      setMessage(error.message);
    } finally {
      setLoadingItemId(null);
    }
  };

  const guide = async (itemId) => {
    try {
      setLoadingItemId(itemId);
      setMessage("");
      const guidance = await generateGuidance(analysisId, itemId);
      setGuidanceByItem((previous) => ({ ...previous, [itemId]: guidance }));
    } catch (error) {
      setMessage(error.message);
    } finally {
      setLoadingItemId(null);
    }
  };

  if (!detail) {
    return <main>{message || "불러오는 중"}</main>;
  }

  const { summary, items } = detail;
  const completed = summary.analysisStatus === "COMPLETED";
  const rate = (value) => {
    if (!completed) return "재분석 필요";
    return value == null ? "평가 불가" : `${value}%`;
  };
  return (
    <main>
      <Link to="/analysis">분석 목록</Link>
      <h1>{summary.jobTitle || `공고 ${summary.jobId}`}</h1>
      <p>{summary.companyName || "회사 미기재"}</p>
      <p>분석 상태: {summary.analysisStatus}</p>

      <h2>일치율</h2>
      <ul>
        <li>전체: {rate(summary.overallMatchRate)}</li>
        <li>일치 자격요건: {summary.matchedCount ?? 0} / {summary.comparableCount ?? 0}</li>
        <li>필수: {rate(summary.requiredMatchRate)}</li>
        <li>우대: {rate(summary.preferredMatchRate)}</li>
        <li>객관 조건: {rate(summary.objectiveMatchRate)}</li>
        <li>평가 가능 비율: {rate(summary.evaluationCoverage)}</li>
      </ul>
      {completed && summary.overallMatchRate == null && (
        <p>비교 가능한 필수·우대·객관 자격요건이 없어 일치율을 계산할 수 없습니다.</p>
      )}

      <h2>요구조건별 결과</h2>
      {items.map((item) => (
        <section key={item.itemId}>
          <h3>{item.requirement}</h3>
          <p>{item.requirementCategory} / {item.comparisonKind}</p>
          <p>{item.statusLabel}</p>
          <p>{item.reason || "판정 사유 없음"}</p>
          <p>
            {item.evidenceText
              ? `${item.evidenceDocumentType} · ${item.evidenceSourceLocation || "위치 미확인"}: ${item.evidenceText}`
              : "확인 가능한 대표 근거 없음"}
          </p>
          {["EXPERIENCE", "RESPONSIBILITY"].includes(item.comparisonKind) &&
            item.status !== "NO_EXPERIENCE" && (
              <div>
                <select
                  value={responses[item.itemId]?.userResponseType || ""}
                  onChange={(event) => setResponses((previous) => ({
                    ...previous,
                    [item.itemId]: {
                      ...previous[item.itemId],
                      userResponseType: event.target.value,
                    },
                  }))}
                >
                  <option value="">경험 구분 선택</option>
                  <option value="EXPERIENCED">경험 있음</option>
                  <option value="PARTIAL">일부 경험</option>
                </select>
                <textarea
                  placeholder="직접 수행한 내용과 범위를 입력하세요."
                  value={responses[item.itemId]?.userExperienceText || ""}
                  onChange={(event) => setResponses((previous) => ({
                    ...previous,
                    [item.itemId]: {
                      ...previous[item.itemId],
                      userExperienceText: event.target.value,
                    },
                  }))}
                />
                <button type="button" onClick={() => rejudge(item.itemId)}>
                  추가 경험으로 재판정
                </button>
                <button type="button" onClick={() => confirm(item.itemId)}>
                  경험 없음 확인
                </button>
              </div>
            )}
          <button
            type="button"
            disabled={loadingItemId === item.itemId}
            onClick={() => guide(item.itemId)}
          >
            {loadingItemId === item.itemId ? "근거 확인 중" : "작성 방향 보기"}
          </button>
          {guidanceByItem[item.itemId] && (
            <div>
              <h4>서류 작성 방향</h4>
              <ul>
                {guidanceByItem[item.itemId].lines.map((line, index) => (
                  <li key={`${item.itemId}-${index}`}>{line}</li>
                ))}
              </ul>
              <p>검증에 사용한 근거: {guidanceByItem[item.itemId].evidenceCount}개</p>
            </div>
          )}
        </section>
      ))}
      {message && <p>{message}</p>}
    </main>
  );
}

export default AnalysisDetailPage;
