import { useState } from "react";
import NavBar from "../components/NavBar";
import LearningPlanPanel from "../components/LearningPlanPanel";
import { analyzeLearning } from "../api/learningApi";
import "./LearningPage.css";

const kinds = { SKILL: "기술", CERTIFICATE: "자격증", EXPERIENCE: "수행 경험" };
const states = { UNKNOWN: "확인 필요", EXPERIENCED: "충분한 경험·보유", PARTIAL: "일부 경험", NO_EXPERIENCE: "경험 없음·미보유 확인" };
const preparationTypes = { TECHNICAL: "기술 학습", CERTIFICATE: "자격증", PROJECT: "프로젝트", DOCUMENT: "서류 보완" };

export default function LearningPage() {
  const [region, setRegion] = useState("");
  const [result, setResult] = useState(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");

  async function submit(event) {
    event.preventDefault();
    setBusy(true); setError(""); setResult(null);
    try { setResult(await analyzeLearning(region.trim())); }
    catch (e) { setError(e.message); }
    finally { setBusy(false); }
  }

  return <main className="learning-page">
    <NavBar />
    <h1>준비 역량 분석</h1>
    <p>등록한 관심 직무와 선택 지역에 맞는 모집 중 공고를 최신순 최대 10개 분석합니다.</p>
    <form onSubmit={submit}>
      <fieldset disabled={busy}>
        <legend>분석 지역</legend>
        <label>근무 지역 <input required maxLength={100} value={region}
          onChange={e => { setRegion(e.target.value); setResult(null); }}
          placeholder="예: 서울특별시 또는 전국" /></label>
        <p>전국 검색도 자동 적용되지 않으므로 필요한 경우 직접 ‘전국’을 입력하세요.</p>
        <button type="submit">{busy ? "분석 중…" : "요구 역량 분석하기"}</button>
      </fieldset>
    </form>
    {error && <p role="alert">{error}</p>}
    {result && <section aria-live="polite">
      <h2>분석 결과</h2>
      <p>{result.message} · 지역 {result.region} · 기준일 {result.referenceDate} · 대상 {result.targetJobCount}건</p>
      {!!result.analysisFailureCount && <p>자동 분석에 실패해 제외된 공고: {result.analysisFailureCount}건</p>}
      {!!result.skippedJobCount && <p>요구사항 분석이 없어 제외된 공고: {result.skippedJobCount}건</p>}
      <p>대상 공고 ID: {result.targetJobIds.join(", ") || "없음"}</p>
      <h3>요구 역량 빈도</h3>
      <div className="learning-table"><table><thead><tr><th>역량</th><th>종류</th><th>등장 공고</th><th>필수</th><th>우대</th></tr></thead><tbody>
        {result.frequency.map(x => <tr key={`${x.type}:${x.name}`}><td>{x.name}</td><td>{kinds[x.type]}</td><td>{x.totalJobCount}</td><td>{x.requiredCount}</td><td>{x.preferredCount}</td></tr>)}
      </tbody></table></div>
      <h3>준비 대상과 우선순위</h3>
      <p>등장 공고 수가 같은 항목은 동순위이며 이름순 표시는 추가 우선순위를 뜻하지 않습니다.</p>
      {!result.preparation.length && <p>확정된 준비 대상이 없습니다.</p>}
      {result.preparation.map(x => <article key={`${x.type}:${x.name}`}><h4>{x.rank}순위 · {x.name} · {preparationTypes[x.preparationType]}</h4><p>{x.totalJobCount}개 공고 · {states[x.userStatus]}</p><p>준비할 항목: {x.targets.join(", ")}</p><p>근거: {x.evidence}</p></article>)}
      <h3>확인 필요·서류 보완</h3>
      {!result.needsConfirmation.length && <p>없음</p>}
      {result.needsConfirmation.map(x => <p key={`${x.type}:${x.name}`}><strong>{x.name}</strong>: {x.reason}</p>)}
      <h3>충분한 경험·보유로 제외</h3>
      {!result.excluded.length && <p>없음</p>}
      {result.excluded.map(x => <p key={`${x.type}:${x.name}`}>{x.name}: {x.evidence}</p>)}
    </section>}
    <LearningPlanPanel region={region.trim()} canGenerate={!busy && !!result?.preparation.length} />
  </main>;
}
