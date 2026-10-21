import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import NavBar from "../components/NavBar";
import {
  compareDocumentWithJob,
  getAnalyses,
  checkAiServer,
} from "../api/analysisApi";

const EMPTY_FORM = {
  jobKey: "",
  jobTitle: "",
  companyName: "",
  closeDate: "",
  jobText: "",
};

function AnalysisPage() {
  const [form, setForm] = useState(EMPTY_FORM);

  const [userAnalysisText, setUserAnalysisText] = useState("");
  const [embeddedChunksText, setEmbeddedChunksText] = useState("");

  const [analyses, setAnalyses] = useState([]);

  const [message, setMessage] = useState("");
  const [loading, setLoading] = useState(false);
  const [lastResult, setLastResult] = useState(null);

  // 비교를 실행한 뒤 목록을 다시 불러오기 위한 키
  const [reloadKey, setReloadKey] = useState(0);

  useEffect(() => {
    const load = async () => {
      try {
        const data = await getAnalyses();
        setAnalyses(data);
      } catch (error) {
        setMessage(error.message);
      }
    };

    load();
  }, [reloadKey]);

  const handleChange = (e) => {
    const { name, value } = e.target;

    setForm((prev) => ({
      ...prev,
      [name]: value,
    }));
  };

  // 문서 분석 결과 JSON 파일을 그대로 올려서 쓸 수 있게 한다.
  const handleJsonFile = (e, setText) => {
    const file = e.target.files?.[0];

    if (!file) {
      return;
    }

    const reader = new FileReader();

    reader.onload = () => {
      setText(String(reader.result));
    };

    reader.readAsText(file);
  };

  const parseJson = (text, label, required) => {
    if (!text || text.trim() === "") {
      if (required) {
        throw new Error(`${label} JSON을 입력해 주세요.`);
      }

      return null;
    }

    try {
      return JSON.parse(text);
    } catch (error) {
      throw new Error(
        `${label} JSON 형식이 올바르지 않습니다.`,
        { cause: error }
      );
    }
  };

  const handleCompare = async (e) => {
    e.preventDefault();

    setMessage("");
    setLastResult(null);

    try {
      const userAnalysis = parseJson(
        userAnalysisText,
        "사용자 문서 분석 결과(userAnalysis)",
        true
      );

      const embeddedChunks = parseJson(
        embeddedChunksText,
        "RAG 임베딩 청크(embeddedChunks)",
        false
      );

      setLoading(true);

      const result = await compareDocumentWithJob({
        jobKey: form.jobKey,
        jobTitle: form.jobTitle || null,
        companyName: form.companyName || null,
        closeDate: form.closeDate,
        jobText: form.jobText,
        userAnalysis,
        embeddedChunks: embeddedChunks ?? [],
      });

      setLastResult(result);
      setMessage("비교가 끝났고 결과를 저장했습니다.");

      setReloadKey((key) => key + 1);
    } catch (error) {
      setMessage(error.message);
    } finally {
      setLoading(false);
    }
  };

  const handleAiCheck = async () => {
    setMessage("");

    try {
      const result = await checkAiServer();
      setMessage(`AI 서버 응답: ${result.response}`);
    } catch (error) {
      setMessage(error.message);
    }
  };

  return (
    <div>
      <NavBar />

      <h1>서류와 공고 비교</h1>

      <p>
        이력서/자기소개서 분석 결과와 공고 본문을 AI 서버로 보내
        요구사항별 판정과 근거를 받아 저장합니다.
      </p>

      <button type="button" onClick={handleAiCheck}>
        AI 서버 연결 확인
      </button>

      <form onSubmit={handleCompare}>
        <div>
          <label>공고 식별 키 (jobKey)</label>

          <input
            type="text"
            name="jobKey"
            value={form.jobKey}
            onChange={handleChange}
            placeholder="예: job-001"
          />
        </div>

        <div>
          <label>공고 제목</label>

          <input
            type="text"
            name="jobTitle"
            value={form.jobTitle}
            onChange={handleChange}
            placeholder="예: 백엔드 개발자"
          />
        </div>

        <div>
          <label>회사명</label>

          <input
            type="text"
            name="companyName"
            value={form.companyName}
            onChange={handleChange}
            placeholder="예: A회사"
          />
        </div>

        <div>
          <label>마감일</label>

          <input
            type="date"
            name="closeDate"
            value={form.closeDate}
            onChange={handleChange}
          />
        </div>

        <div>
          <label>공고 본문</label>

          <textarea
            name="jobText"
            rows={10}
            cols={80}
            value={form.jobText}
            onChange={handleChange}
            placeholder="공고 전문을 붙여 넣으세요."
          />
        </div>

        <div>
          <label>
            사용자 문서 분석 결과 JSON (userAnalysis)
          </label>

          <input
            type="file"
            accept=".json,application/json"
            onChange={(e) =>
              handleJsonFile(e, setUserAnalysisText)
            }
          />

          <textarea
            rows={8}
            cols={80}
            value={userAnalysisText}
            onChange={(e) =>
              setUserAnalysisText(e.target.value)
            }
            placeholder='FastAPI /api/v1/documents/analyze 응답을 붙여 넣으세요.'
          />
        </div>

        <div>
          <label>
            RAG 임베딩 청크 JSON (embeddedChunks, 선택)
          </label>

          <input
            type="file"
            accept=".json,application/json"
            onChange={(e) =>
              handleJsonFile(e, setEmbeddedChunksText)
            }
          />

          <textarea
            rows={5}
            cols={80}
            value={embeddedChunksText}
            onChange={(e) =>
              setEmbeddedChunksText(e.target.value)
            }
            placeholder="비워 두면 빈 배열로 보냅니다."
          />
        </div>

        <button type="submit" disabled={loading}>
          {loading ? "비교 중..." : "비교 실행"}
        </button>
      </form>

      {message && <p>{message}</p>}

      {lastResult && (
        <div>
          <h2>방금 저장한 결과</h2>

          <p>
            {lastResult.summary.companyName} /{" "}
            {lastResult.summary.jobTitle} - 전체 일치율{" "}
            {lastResult.summary.overallMatchRate}%
          </p>

          <p>
            경험 있음 {lastResult.statusCount.experienced} ·
            일부만 있음 {lastResult.statusCount.partial} ·
            기록 부족 {lastResult.statusCount.insufficientEvidence} ·
            경험 없음 {lastResult.statusCount.noExperience}
          </p>

          <Link to={`/analysis/${lastResult.summary.analysisId}`}>
            요구사항별 판정과 근거 보기
          </Link>
        </div>
      )}

      <hr />

      <h2>저장된 분석 결과</h2>

      {analyses.length === 0 ? (
        <p>저장된 분석 결과가 없습니다.</p>
      ) : (
        <table border="1" cellPadding="6">
          <thead>
            <tr>
              <th>공고</th>
              <th>회사</th>
              <th>마감일</th>
              <th>전체 일치율</th>
              <th>필수 충족률</th>
              <th>추천 구분</th>
              <th>요구사항</th>
              <th>상세</th>
            </tr>
          </thead>

          <tbody>
            {analyses.map((item) => (
              <tr key={item.analysisId}>
                <td>{item.jobTitle || item.jobKey}</td>
                <td>{item.companyName || "-"}</td>
                <td>{item.closeDate || "-"}</td>
                <td>{item.overallMatchRate ?? "-"}</td>
                <td>{item.requiredMatchRate ?? "-"}</td>
                <td>{item.recommendationStatus || "-"}</td>
                <td>{item.requirementCount}건</td>
                <td>
                  <Link to={`/analysis/${item.analysisId}`}>
                    보기
                  </Link>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}

export default AnalysisPage;
