import { useState } from "react";
import NavBar from "../components/NavBar";
import LearningPlanPanel from "../components/LearningPlanPanel";
import { analyzeLearning } from "../api/learningApi";
import "./LearningPage.css";

const kinds = { SKILL: "기술", CERTIFICATE: "자격증", EXPERIENCE: "수행 경험" };
const states = { UNKNOWN: "확인 필요", EXPERIENCED: "충분한 경험·보유", PARTIAL: "일부 경험", NO_EXPERIENCE: "경험 없음·미보유 확인" };
const preparationTypes = { TECHNICAL: "기술 학습", CERTIFICATE: "자격증", PROJECT: "프로젝트", DOCUMENT: "서류 보완" };
const blank = () => ({ name: "", type: "SKILL", status: "UNKNOWN", evidence: "", details: "" });
const sample = () => [
  { name: "Java", type: "SKILL", status: "EXPERIENCED", evidence: "[가상 사용자] Java API 개발 경험을 확인했습니다.", details: "" },
  { name: "SQL", type: "SKILL", status: "PARTIAL", evidence: "[가상 사용자] 기본 조회 경험은 있지만 실행 계획 분석 경험은 없습니다.", details: "실행 계획 분석" },
  { name: "정보처리기사", type: "CERTIFICATE", status: "NO_EXPERIENCE", evidence: "[가상 사용자] 미보유를 확인했습니다.", details: "" },
  { name: "API 개발", type: "EXPERIENCE", status: "NO_EXPERIENCE", evidence: "[가상 사용자] API 구현 경험이 없다고 확인했습니다.", details: "" },
  { name: "Docker", type: "SKILL", status: "UNKNOWN", evidence: "", details: "" },
];

// 사용자 상태는 명시적으로 입력한 내용만 전송한다. 기간 계획은 이 분석의
// 준비 대상을 서버에서 다시 계산한 뒤 생성하므로 브라우저의 우선순위를 신뢰하지 않는다.
export default function LearningPage() {
  const [source, setSource] = useState("TEST");
  const [rows, setRows] = useState([]);
  const [result, setResult] = useState(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const update = (index, field, value) => {
    setResult(null);
    setRows(old => old.map((row, i) => i === index ? { ...row, [field]: value } : row));
  };
  async function submit(event) {
    event.preventDefault();
    setBusy(true); setError(""); setResult(null);
    try {
      // 세부 항목은 줄 단위로 분리한다. 비어 있는 상태는 서버가 UNKNOWN으로 처리하며
      // 목록에 없다는 이유로 사용자의 실제 경험이 없다고 판단하지 않는다.
      const assessments = rows.map(({ details, ...row }) => ({ ...row,
        name: row.name.trim(), missingDetails: details.split("\n").map(x => x.trim()).filter(Boolean) }));
      setResult(await analyzeLearning(source, assessments));
    } catch (e) { setError(e.message); }
    finally { setBusy(false); }
  }
  return <main className="learning-page">
    <NavBar />
    <h1>준비 역량 분석</h1>
    <p>관심 직무의 모집 중 공고를 최신순 최대 10개 분석합니다. 기준일과 실제 대상 수를 결과에 표시합니다.</p>
    <form onSubmit={submit}>
      <fieldset disabled={busy}>
        <legend>분석 데이터와 사용자 경험</legend>
        <label>공고 출처 <select value={source} onChange={e => { setSource(e.target.value); setResult(null); }}>
          <option value="TEST">자체 가상 공고</option><option value="DATABASE">데이터베이스 공고</option>
        </select></label>
        <p>자체 공고는 BACKEND 12건과 제외 조건 검증용 3건입니다. 관심 직무에 BACKEND가 있어야 합니다. DB에는 저장하지 않습니다.</p>
        <p>경험 입력이 없으면 확인 필요로 분류합니다. 가상 사용자 예시는 실제 본인 분석 정보가 아닙니다.</p>
        <button type="button" onClick={() => { setRows(sample()); setResult(null); }}>가상 사용자 예시 불러오기</button>{" "}
        <button type="button" onClick={() => { setRows(old => [...old, blank()]); setResult(null); }}>경험 항목 추가</button>{" "}
        <button type="button" onClick={() => { setRows([]); setResult(null); }}>경험 입력 비우기</button>
        {rows.map((row, i) => <section className="learning-input" key={i}>
          <label>역량명 <input required maxLength={200} value={row.name} onChange={e => update(i, "name", e.target.value)} /></label>
          <label>종류 <select value={row.type} onChange={e => update(i, "type", e.target.value)}>{Object.entries(kinds).map(([k, v]) => <option key={k} value={k}>{v}</option>)}</select></label>
          <label>상태 <select value={row.status} onChange={e => update(i, "status", e.target.value)}>{Object.entries(states).map(([k, v]) => <option key={k} value={k}>{v}</option>)}</select></label>
          <label>판단 근거·본인 확인 <textarea required={row.status !== "UNKNOWN"} maxLength={4000} value={row.evidence} onChange={e => update(i, "evidence", e.target.value)} /></label>
          {row.status === "PARTIAL" && <label>부족한 세부 항목 (한 줄에 하나, 미확인 시 빈칸) <textarea value={row.details} onChange={e => update(i, "details", e.target.value)} /></label>}
          <button type="button" onClick={() => { setRows(old => old.filter((_, index) => index !== i)); setResult(null); }}>항목 삭제</button>
        </section>)}
        <p><button type="submit">{busy ? "분석 중…" : "PLN-01~03 분석하기"}</button></p>
      </fieldset>
    </form>
    {error && <p role="alert">{error}</p>}
    {result && <section aria-live="polite">
      <h2>분석 결과 · {result.source === "TEST" ? "가상 공고" : "DB 공고"}</h2>
      <p>{result.message} · 기준일 {result.referenceDate} · 대상 {result.targetJobCount}건</p>
      <p>대상 공고 ID: {result.targetJobIds.join(", ") || "없음"}</p>
      <h3>PLN-01 요구 역량 빈도</h3>
      <div className="learning-table"><table><thead><tr><th>역량</th><th>종류</th><th>등장 공고</th><th>필수</th><th>우대</th></tr></thead><tbody>
        {result.frequency.map(x => <tr key={`${x.type}:${x.name}`}><td>{x.name}</td><td>{kinds[x.type]}</td><td>{x.totalJobCount}</td><td>{x.requiredCount}</td><td>{x.preferredCount}</td></tr>)}
      </tbody></table></div>
      <h3>PLN-02·03 준비 대상과 우선순위</h3>
      <p>등장 공고 수가 같은 항목은 동순위입니다. 동순위 항목의 이름순 표시는 추가 우선순위를 뜻하지 않습니다.</p>
      {!result.preparation.length && <p>확정된 준비 대상이 없습니다.</p>}
      {result.preparation.map(x => <article key={`${x.type}:${x.name}`}><h4>{x.rank}순위 · {x.name} · {preparationTypes[x.preparationType]}</h4><p>{x.totalJobCount}개 공고 · {states[x.userStatus]}</p><p>준비할 항목: {x.targets.join(", ")}</p><p>근거: {x.evidence}</p></article>)}
      <h3>확인 필요·서류 보완</h3>
      {!result.needsConfirmation.length && <p>없음</p>}
      {result.needsConfirmation.map(x => <p key={`${x.type}:${x.name}`}><strong>{x.name}</strong>: {x.reason}</p>)}
      <h3>충분한 경험·보유로 제외</h3>
      {!result.excluded.length && <p>없음</p>}
      {result.excluded.map(x => <p key={`${x.type}:${x.name}`}>{x.name}: {x.evidence}</p>)}
    </section>}
    <LearningPlanPanel source={source} canGenerate={!busy && !!result?.preparation.length}
      assessments={rows.map(({ details, ...row }) => ({ ...row, name: row.name.trim(),
        missingDetails: details.split("\n").map(x => x.trim()).filter(Boolean) }))} />
  </main>;
}
