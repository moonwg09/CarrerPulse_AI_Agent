import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { confirmNoExperience, generateGuidance, getAnalysisDetail, rejudgeExperience } from "../api/analysisApi";
import NavBar from "../components/NavBar";
import PageToast from "../components/PageToast";

function AnalysisDetailPage() {
  const { analysisId } = useParams();
  const [detail, setDetail] = useState(null);
  const [message, setMessage] = useState("");
  const [guidanceByItem, setGuidanceByItem] = useState({});
  const [guidanceErrorByItem, setGuidanceErrorByItem] = useState({});
  const [loadingItemId, setLoadingItemId] = useState(null);
  const [responses, setResponses] = useState({});
  const [notice, setNotice] = useState("");

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
      setMessage("");
      setDetail(await confirmNoExperience(analysisId, itemId));
      setNotice("경험 없음으로 확정했습니다.");
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
      setMessage("");
      setLoadingItemId(itemId);
      setDetail(await rejudgeExperience(analysisId, itemId, response));
      setNotice("추가 경험을 반영해 다시 판정했습니다.");
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
      setGuidanceErrorByItem((previous) => ({ ...previous, [itemId]: "" }));
      const guidance = await generateGuidance(analysisId, itemId);
      setGuidanceByItem((previous) => ({ ...previous, [itemId]: guidance }));
    } catch (error) {
      setGuidanceByItem((previous) => {
        const next = { ...previous };
        delete next[itemId];
        return next;
      });
      setGuidanceErrorByItem((previous) => ({
        ...previous,
        [itemId]: error.message || (
          "확인된 원문 근거가 부족해 작성 방향을 생성할 수 없습니다. " +
          "직접 수행한 역할·사용 기술·결과를 추가한 뒤 다시 시도해 주세요."
        ),
      }));
    } finally {
      setLoadingItemId(null);
    }
  };

  if (!detail) {
    return <main><NavBar />{message || "불러오는 중"}</main>;
  }

  const { summary, items } = detail;
  const completed = summary.analysisStatus === "COMPLETED";
  const rate = (value) => {
    if (!completed) return "재분석 필요";
    return value == null ? "평가 불가" : `${value}%`;
  };
  const statusClassName = (status) => (
    `analysis-status analysis-status--${String(status || "UNKNOWN").toLowerCase().replaceAll("_", "-")}`
  );
  return (
    <main>
      <NavBar />
      <PageToast message={notice} onClose={setNotice} />
      <Link to="/analysis">분석 목록</Link>
      <h1>{summary.jobTitle || `공고 ${summary.jobId}`}</h1>
      <p>{summary.companyName || "회사 미기재"}</p>
      <p>분석 상태: {summary.analysisStatus}</p>

      <h2>일치율</h2>
      <ul className="metric-grid">
        <li>전체 일치율<strong>{rate(summary.overallMatchRate)}</strong></li>
        <li>일치 자격요건<strong>{summary.matchedCount ?? 0} / {summary.comparableCount ?? 0}</strong></li>
        <li>필수 충족률<strong>{rate(summary.requiredMatchRate)}</strong></li>
        <li>우대 충족률<strong>{rate(summary.preferredMatchRate)}</strong></li>
        <li>객관 조건<strong>{rate(summary.objectiveMatchRate)}</strong></li>
        <li>평가 가능 비율<strong>{rate(summary.evaluationCoverage)}</strong></li>
      </ul>
      {completed && summary.overallMatchRate == null && (
        <p>비교 가능한 필수·우대·객관 자격요건이 없어 일치율을 계산할 수 없습니다.</p>
      )}

      <h2>요구조건별 결과</h2>
      {items.map((item) => (
        <section className="analysis-item" key={item.itemId}>
          <div>
            <h3>{item.requirement}</h3>
            <p>{item.requirementCategory} / {item.comparisonKind}</p>
            <span className={statusClassName(item.status)}>{item.statusLabel}</span>
          </div>
          <div>
            <div className="analysis-reason">
              <strong>판정 사유</strong>
              <p>{item.reason || "판정 사유 없음"}</p>
            </div>
            <div className={`analysis-evidence${item.evidenceText ? "" : " analysis-evidence--empty"}`}>
              <strong>원문 근거</strong>
              {item.evidenceText ? (
                <>
                  <p className="analysis-evidence__source">
                    {item.evidenceDocumentType} · {item.evidenceSourceLocation || "위치 미확인"}
                  </p>
                  <blockquote>{item.evidenceText}</blockquote>
                </>
              ) : (
                <p>확인 가능한 대표 근거 없음</p>
              )}
            </div>
          {["EXPERIENCE", "RESPONSIBILITY"].includes(item.comparisonKind) &&
            item.status !== "NO_EXPERIENCE" && (
              <div className="analysis-response-form">
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
            <div className="guidance-panel">
              <h4>서류 작성 방향</h4>
              <div className="guidance-list">
                {guidanceByItem[item.itemId].lines.map((line, index) => (
                  <div className="guidance-row" key={`${item.itemId}-${index}`}>
                    <span className="record-index">{String(index + 1).padStart(2, "0")}</span>
                    <p>{line
                      .replace(/^\s*[-*•]+\s*/, "")
                      .replace(/\s*[\[(]?EV-\d{4}[\])]?/gi, "")
                      .trim()}</p>
                  </div>
                ))}
              </div>
              <p>검증에 사용한 근거: {guidanceByItem[item.itemId].evidenceCount}개</p>
            </div>
          )}
          {guidanceErrorByItem[item.itemId] && (
            <div className="guidance-panel guidance-panel--empty" role="status">
              <h4>서류 작성 방향</h4>
              <p>{guidanceErrorByItem[item.itemId]}</p>
              <p>관련 경험의 직접 수행 역할·사용 기술·결과를 서류에 보완한 뒤 다시 시도해 주세요.</p>
            </div>
          )}
          </div>
        </section>
      ))}
      {message && <p>{message}</p>}
    </main>
  );
}

export default AnalysisDetailPage;
