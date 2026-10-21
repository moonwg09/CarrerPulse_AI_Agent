import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import NavBar from "../components/NavBar";
import { getRecommendations } from "../api/analysisApi";

const STATUS_OPTIONS = [
  { value: "RECOMMENDED", label: "추천 공고" },
  { value: "GROWTH_CANDIDATE", label: "성장 후보 공고" },
  { value: "EXCLUDED", label: "추천 제외" },
];

function RecommendationPage() {
  const [status, setStatus] = useState("RECOMMENDED");
  const [limit, setLimit] = useState("");

  const [result, setResult] = useState(null);
  const [message, setMessage] = useState("");

  // 같은 조건으로 다시 조회하기 위한 키
  const [reloadKey, setReloadKey] = useState(0);

  useEffect(() => {
    const load = async () => {
      try {
        const data = await getRecommendations(
          status,
          limit === "" ? null : Number(limit)
        );

        setResult(data);
        setMessage("");
      } catch (error) {
        setResult(null);
        setMessage(error.message);
      }
    };

    load();
  }, [status, limit, reloadKey]);

  const handleReload = () => {
    setReloadKey((key) => key + 1);
  };

  return (
    <div>
      <NavBar />

      <h1>추천 공고 조회</h1>

      <p>
        저장된 비교 결과 중 마감되지 않은 공고를 일치율 순으로
        보여줍니다. 추천 공고는 최대 10개, 성장 후보 공고는
        최대 5개가 기본값입니다.
      </p>

      <div>
        <label>추천 구분</label>

        <select
          value={status}
          onChange={(e) => setStatus(e.target.value)}
        >
          {STATUS_OPTIONS.map((option) => (
            <option key={option.value} value={option.value}>
              {option.label}
            </option>
          ))}
        </select>

        <label>개수</label>

        <input
          type="number"
          min="1"
          max="50"
          value={limit}
          onChange={(e) => setLimit(e.target.value)}
          placeholder="기본값"
        />

        <button type="button" onClick={handleReload}>
          다시 조회
        </button>
      </div>

      {message && <p>{message}</p>}

      {!result && !message && <p>불러오는 중...</p>}

      {result && (
        <div>
          <p>
            기준일 {result.baseDate} · {result.totalCount}건
          </p>

          {result.items.length === 0 ? (
            <p>
              조건에 맞는 공고가 없습니다. 먼저 서류와 공고를
              비교해 주세요.
            </p>
          ) : (
            <table border="1" cellPadding="6">
              <thead>
                <tr>
                  <th>순위</th>
                  <th>공고</th>
                  <th>회사</th>
                  <th>마감일</th>
                  <th>전체 일치율</th>
                  <th>필수 충족률</th>
                  <th>평가 가능 비율</th>
                  <th>상세</th>
                </tr>
              </thead>

              <tbody>
                {result.items.map((item, index) => (
                  <tr key={item.analysisId}>
                    <td>{index + 1}</td>
                    <td>{item.jobTitle || item.jobKey}</td>
                    <td>{item.companyName || "-"}</td>
                    <td>{item.closeDate || "-"}</td>
                    <td>{item.overallMatchRate ?? "-"}</td>
                    <td>{item.requiredMatchRate ?? "-"}</td>
                    <td>{item.evaluationCoverage ?? "-"}</td>
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
      )}
    </div>
  );
}

export default RecommendationPage;
