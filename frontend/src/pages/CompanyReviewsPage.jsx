import { useState } from "react";

function CompanyReviewsPage() {
  const [reviews, setReviews] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [searched, setSearched] = useState(false);
  const [companyKeyword, setCompanyKeyword] = useState("");

  const fetchReviews = async () => {
    setLoading(true);
    setError("");
    setSearched(false);
    setReviews([]);

    try {
      const response = await fetch("/api/company-reviews", {
        credentials: "include",
      });

      if (!response.ok) {
        if (response.status === 401) {
          throw new Error("로그인이 필요합니다.");
        }

        throw new Error(`HTTP 오류: ${response.status}`);
      }

      const data = await response.json();

      if (!Array.isArray(data)) {
        throw new Error("후기 데이터 형식이 올바르지 않습니다.");
      }

      setReviews(data);
      setSearched(true);
    } catch (error) {
      setError(`기업 후기를 불러오지 못했습니다. ${error.message}`);
    } finally {
      setLoading(false);
    }
  };

  const filteredReviews = reviews.filter((review) =>
    (review.company_name ?? "")
      .toLowerCase()
      .includes(companyKeyword.trim().toLowerCase())
  );

  const columns = [
    { key: "company_name", label: "기업명" },
    { key: "job_name", label: "직무" },
    { key: "joined_year_month", label: "입사 연월" },
    { key: "preparation_tip", label: "후기 내용" },
    { key: "created_at", label: "작성일" },
  ];

  const cellStyle = {
    border: "1px solid #ccc",
    padding: "12px",
    textAlign: "left",
    verticalAlign: "top",
  };

  return (
    <main style={{ padding: "24px" }}>
      <h1>기업 후기 조회</h1>

      <div style={{ marginBottom: "16px" }}>
        <label htmlFor="company-search">기업명 검색 </label>
        <input
          id="company-search"
          type="search"
          placeholder="예: 새봄"
          value={companyKeyword}
          onChange={(event) => setCompanyKeyword(event.target.value)}
          style={{
            padding: "10px 12px",
            border: "1px solid #ccc",
            borderRadius: "8px",
            fontSize: "16px",
          }}
        />
      </div>

      <button onClick={fetchReviews} disabled={loading}>
        {loading ? "조회 중..." : "기업 후기 불러오기"}
      </button>

      {error && (
        <p role="alert" style={{ color: "#d32f2f" }}>
          {error}
        </p>
      )}

      {searched && <p>조회 결과: {filteredReviews.length}건</p>}

      {searched && filteredReviews.length === 0 && (
        <p>등록된 후기 없음</p>
      )}

      {filteredReviews.length > 0 && (
        <div style={{ overflowX: "auto", marginTop: "16px" }}>
          <table style={{ borderCollapse: "collapse", width: "100%" }}>
            <thead>
              <tr>
                {columns.map((column) => (
                  <th key={column.key} scope="col" style={cellStyle}>
                    {column.label}
                  </th>
                ))}
              </tr>
            </thead>

            <tbody>
              {filteredReviews.map((review) => (
                <tr key={review.review_id}>
                  {columns.map((column) => (
                    <td
                      key={column.key}
                      style={{
                        ...cellStyle,
                        minWidth:
                          column.key === "preparation_tip" ? "360px" : "120px",
                        whiteSpace: "pre-wrap",
                        wordBreak: "keep-all",
                        overflowWrap: "anywhere",
                        lineHeight: 1.8,
                      }}
                    >
                      {review[column.key] ?? "—"}
                    </td>
                  ))}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </main>
  );
}

export default CompanyReviewsPage;
