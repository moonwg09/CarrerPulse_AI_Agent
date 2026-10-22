import { useState } from "react";
import { generateLearningPlan, getLatestLearningPlan } from "../api/learningApi";

const activityNames = { TECH_STUDY: "기술 학습", PROJECT: "프로젝트", CERTIFICATE: "자격증", DOCUMENT_IMPROVEMENT: "서류 보완" };

// 생성 입력과 저장된 계획의 입력은 별도이다. 조회 결과에는 생성 당시 날짜·시간을
// 함께 표시하므로 현재 입력창의 값으로 과거 계획을 잘못 해석하지 않는다.
export default function LearningPlanPanel({ source, assessments, canGenerate }) {
  const [input, setInput] = useState({ startDate: "", endDate: "", weeklyAvailableHours: "", mode: "TEST" });
  const [plan, setPlan] = useState(null);
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");
  const change = e => setInput(old => ({ ...old, [e.target.name]: e.target.value }));

  async function generate(event) {
    event.preventDefault();
    setBusy(true); setError(""); setMessage("");
    try {
      const saved = await generateLearningPlan({ ...input, source, assessments,
        weeklyAvailableHours: Number(input.weeklyAvailableHours) });
      setPlan(saved);
      setMessage("계획을 생성하고 저장했습니다.");
    } catch (e) { setError(e.message); }
    finally { setBusy(false); }
  }
  async function load() {
    setBusy(true); setError(""); setMessage("");
    try {
      const saved = await getLatestLearningPlan();
      setPlan(saved);
      if (!saved) setMessage("생성된 학습계획이 없습니다. 시작일·종료일·주당 학습 가능 시간을 입력해 생성하세요.");
    } catch (e) { setError(e.message); }
    finally { setBusy(false); }
  }

  return <section aria-label="기간 기반 학습계획">
    <h2>PLN-04 기간 기반 학습계획 생성</h2>
    <p>먼저 준비 역량 분석을 완료하세요. 충분한 경험 및 확인이 필요한 항목은 학습 대상에서 제외됩니다.</p>
    <form onSubmit={generate}>
      <fieldset disabled={busy}>
        <legend>학습 기간과 시간</legend>
        <label>시작일 <input required type="date" name="startDate" value={input.startDate} onChange={change} /></label>
        <label>종료일 <input required type="date" name="endDate" min={input.startDate || undefined} value={input.endDate} onChange={change} /></label>
        <label>주당 학습 가능 시간 <input required type="number" name="weeklyAvailableHours" min="0.01" max="168" step="0.01" value={input.weeklyAvailableHours} onChange={change} /> 시간</label>
        <label>생성 방식 <select name="mode" value={input.mode} onChange={change}>
          <option value="TEST">자체 테스트 계획 (가상 시간)</option>
          <option value="GEMINI">Gemini로 생성</option>
        </select></label>
        <p>주당 시간은 상한이며 남는 시간을 억지로 채우지 않습니다. 마지막 부분 주차는 일수 비례 상한을 적용합니다.</p>
        {input.mode === "GEMINI" && <p>준비 대상·부족 세부 항목·기간·주당 시간을 Gemini에 전달합니다. 서버의 API 키 설정이 필요합니다.</p>}
        <button type="submit" disabled={!canGenerate}>{busy ? "처리 중…" : "계획 생성·저장"}</button>
        {!canGenerate && <p>PLN-01~03 분석 후 확정된 준비 대상이 있어야 생성할 수 있습니다.</p>}
      </fieldset>
    </form>
    <h2>PLN-05 저장된 학습계획 조회</h2>
    <button type="button" disabled={busy} onClick={load}>내 최신 계획 불러오기</button>
    {message && <p role="status">{message}</p>}
    {error && <p role="alert">{error}</p>}
    {plan && <section aria-label="저장된 학습계획 상세" aria-live="polite">
      <h3>계획 #{plan.planId} · {plan.mode === "TEST" ? "가상 테스트 계획" : "Gemini 생성 계획"}</h3>
      <p>입력 기간: {plan.startDate} ~ {plan.endDate} · 주당 가능 시간: {plan.weeklyAvailableHours}시간</p>
      <p>공고 출처: {plan.source === "TEST" ? "자체 가상 공고" : "DB 공고"} · 분석 기준일: {plan.referenceDate} · 대상 공고: {plan.targetJobCount}건</p>
      <p>{plan.estimateNotice}</p>
      {plan.adjustmentMessage && <p>{plan.adjustmentMessage}</p>}
      {!plan.weeks.length && <p>입력한 기간과 시간 안에 배치할 수 있는 활동이 없습니다. 시간을 늘려 다시 생성하세요.</p>}
      {plan.weeks.map(week => <article key={week.weekNumber}>
        <h4>{week.weekNumber}주차 · {week.startDate} ~ {week.endDate}</h4>
        <p>예상 {week.totalEstimatedHours}시간 / 가능 {week.availableHours}시간</p>
        <div className="learning-table"><table><thead><tr><th>준비 항목</th><th>수행 순서</th><th>활동 유형</th><th>활동·산출물</th><th>예상 시간</th></tr></thead><tbody>
          {week.items.map((item, i) => <tr key={i}><td>{item.name}<br />{item.targets.join(", ")}</td><td>{i + 1}</td><td>{activityNames[item.activityType]}</td><td>{item.taskName}<p>{item.description}</p></td><td>{item.estimatedHours}시간</td></tr>)}
        </tbody></table></div>
      </article>)}
      {!!plan.unscheduled.length && <><h4>기간 내 배치하지 못한 활동</h4>{plan.unscheduled.map((x, i) => <p key={i}>{x.name}: {x.taskName} · 남은 예상 {x.remainingHours}시간</p>)}</>}
    </section>}
  </section>;
}
