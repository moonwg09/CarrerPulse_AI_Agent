import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import NavBar from "../components/NavBar";
import {
  getAnalysisDetail,
  getAnalysisRaw,
} from "../api/analysisApi";

const CATEGORY_LABEL = {
  REQUIRED: "필수",
  PREFERRED: "우대",
  UNCLEAR: "구분 불명",
};

function AnalysisDetailPage() {
  const { analysisId } = useParams();

  const [detail, setDetail] = useState(null);
  const [raw, setRaw] = useState(null);
  const [message, setMessage] = useState("");

  useEffect(() => {
    const load = async () => {
      try {
        const data = await getAnalysisDetail(analysisId);
        setDetail(data);
      } catch (error) {
        setMessage(error.message);
      }
    };

    load();
  }, [analysisId]);

  const handleShowRaw = async () => {
    setMessage("");

    try {
      const data = await getAnalysisRaw(analysisId);
      setRaw(data);
    } catch (error) {
      setMessage(error.message);
    }
  };

  if (message && !detail) {
    return (
      <div>
        <NavBar />
        <p>{message}</p>
        <Link to="/analysis">목록으로</Link>
      </div>
    );
  }

  if (!detail) {
    return (
      <div>
        <NavBar />
        <p>분석 결과를 불러오는 중...</p>
      </div>
    );
  }

  const summary = detail.summary;

  return (
    <div>
      <NavBar />

      <Link to="/analysis">← 분석 목록</Link>

      <h1>
        {summary.companyName || "회사 미기재"} /{" "}
        {summary.jobTitle || summary.jobKey}
      </h1>

      <h2>일치율</h2>

      <ul>
        <li>전체 일치율: {summary.overallMatchRate ?? "-"}</li>
        <li>필수 충족률: {summary.requiredMatchRate ?? "-"}</li>
        <li>우대 충족률: {summary.preferredMatchRate ?? "-"}</li>
        <li>객관 조건 충족률: {summary.objectiveMatchRate ?? "-"}</li>
        <li>평가 가능 비율: {summary.evaluationCoverage ?? "-"}</li>
        <li>추천 구분: {summary.recommendationStatus || "-"}</li>
        <li>마감일: {summary.closeDate || "-"}</li>
      </ul>

      <h2>판정 요약</h2>

      <p>
        경험 있음 {detail.statusCount.experienced}건 ·
        일부만 있음 {detail.statusCount.partial}건 ·
        기록 부족 {detail.statusCount.insufficientEvidence}건 ·
        경험 없음 {detail.statusCount.noExperience}건
      </p>

      {detail.warnings && detail.warnings.length > 0 && (
        <div>
          <h2>경고</h2>

          <ul>
            {detail.warnings.map((warning, index) => (
              <li key={index}>{warning}</li>
            ))}
          </ul>
        </div>
      )}

      <h2>요구사항별 판정과 근거</h2>

      {detail.items.length === 0 ? (
        <p>요구사항이 없습니다.</p>
      ) : (
        detail.items.map((item) => (
          <div
            key={item.itemId}
            style={{
              border: "1px solid #ccc",
              padding: "10px",
              marginBottom: "10px",
            }}
          >
            <h3>
              [{CATEGORY_LABEL[item.requirementCategory] ||
                item.requirementCategory}
              ] {item.requirement}
            </h3>

            <p>
              <strong>판정: {item.matchStatusLabel}</strong>
            </p>

            {item.reason && <p>{item.reason}</p>}

            {item.matchedSkills &&
              item.matchedSkills.length > 0 && (
                <p>
                  충족 기술: {item.matchedSkills.join(", ")}
                </p>
              )}

            {item.missingSkills &&
              item.missingSkills.length > 0 && (
                <p>
                  부족 기술: {item.missingSkills.join(", ")}
                </p>
              )}

            <h4>내 서류 근거 (원문)</h4>

            {!item.userEvidence ||
            item.userEvidence.length === 0 ? (
              <p>인용된 근거가 없습니다.</p>
            ) : (
              <ul>
                {item.userEvidence.map((evidence, index) => (
                  <li key={index}>
                    <blockquote>{evidence.text}</blockquote>

                    <small>
                      출처: {evidence.source}
                      {evidence.page != null &&
                        ` / ${evidence.page}p`}
                      {evidence.paragraphIndex != null &&
                        ` / 단락 ${evidence.paragraphIndex}`}
                    </small>
                  </li>
                ))}
              </ul>
            )}

            <h4>공고 근거 (원문)</h4>

            {!item.jobEvidence ||
            item.jobEvidence.length === 0 ? (
              <p>인용된 근거가 없습니다.</p>
            ) : (
              <ul>
                {item.jobEvidence.map((evidence, index) => (
                  <li key={index}>
                    <blockquote>{evidence.text}</blockquote>

                    {evidence.section && (
                      <small>구간: {evidence.section}</small>
                    )}
                  </li>
                ))}
              </ul>
            )}
          </div>
        ))
      )}

      {detail.objectiveConditionMatches &&
        detail.objectiveConditionMatches.length > 0 && (
          <div>
            <h2>객관 조건 (학력 / 경력 / 자격증)</h2>

            <table border="1" cellPadding="6">
              <thead>
                <tr>
                  <th>조건</th>
                  <th>구분</th>
                  <th>판정</th>
                  <th>근거</th>
                </tr>
              </thead>

              <tbody>
                {detail.objectiveConditionMatches.map(
                  (condition, index) => (
                    <tr key={index}>
                      <td>{condition.requirement}</td>
                      <td>{condition.conditionType}</td>
                      <td>{condition.status}</td>
                      <td>{condition.reason}</td>
                    </tr>
                  )
                )}
              </tbody>
            </table>
          </div>
        )}

      <hr />

      <button type="button" onClick={handleShowRaw}>
        AI 응답 원본 JSON 보기
      </button>

      {message && <p>{message}</p>}

      {raw && (
        <pre
          style={{
            whiteSpace: "pre-wrap",
            background: "#f5f5f5",
            padding: "10px",
          }}
        >
          {JSON.stringify(raw, null, 2)}
        </pre>
      )}
    </div>
  );
}

export default AnalysisDetailPage;
