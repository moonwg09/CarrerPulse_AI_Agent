import { useEffect, useRef, useState } from "react";
import NavBar from "../components/NavBar";
import { deleteDocument, getDocuments, uploadDocument } from "../api/documentApi";

const types = [
  ["resume", "RESUME", "이력서"],
  ["self-intro", "SELF_INTRO", "자기소개서"],
  ["portfolio", "PORTFOLIO", "포트폴리오"],
];

const statusLabels = {
  PENDING: "처리 대기",
  PROCESSING: "처리 중",
  COMPLETED: "등록 완료",
  FAILED: "처리 실패",
};

function formatFileSize(size) {
  if (!Number.isFinite(Number(size))) return "크기 정보 없음";
  const bytes = Number(size);
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${Math.round(bytes / 1024)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

export default function DocumentsPage() {
  const [documents, setDocuments] = useState([]);
  const [files, setFiles] = useState({});
  const [busy, setBusy] = useState("");
  const [saveProgress, setSaveProgress] = useState("");
  const [message, setMessage] = useState("");
  const fileInputs = useRef({});

  const load = async () => {
    try { setDocuments(await getDocuments()); }
    catch (error) { setMessage(error.message); }
  };
  useEffect(() => { getDocuments().then(setDocuments).catch(error => setMessage(error.message)); }, []);

  async function save() {
    const pending = types.filter(([path]) => files[path]);
    if (pending.length === 0) return;

    setBusy("save"); setMessage("");
    const succeeded = [];
    const failed = [];

    for (let index = 0; index < pending.length; index += 1) {
      const [path, code, label] = pending[index];
      setSaveProgress(`${label} 저장 및 색인 중 (${index + 1}/${pending.length})`);
      try {
        const result = await uploadDocument(path, files[path]);
        succeeded.push(path);
        setDocuments(old => [result, ...old.filter(document => document.documentType !== code)]);
      } catch (error) {
        failed.push(`${label}: ${error.message}`);
      }
    }

    setFiles(old => {
      const next = { ...old };
      succeeded.forEach(path => { delete next[path]; });
      return next;
    });
    succeeded.forEach(path => {
      if (fileInputs.current[path]) fileInputs.current[path].value = "";
    });
    await load();

    if (failed.length > 0) {
      setMessage(`일부 문서를 저장하지 못했습니다. ${failed.join(" / ")}`);
    } else {
      setMessage(`${succeeded.length}개 문서를 저장했습니다.`);
    }
    setSaveProgress(""); setBusy("");
  }

  async function remove(path, label) {
    setBusy(path); setMessage("");
    try { await deleteDocument(path); setMessage(`${label}를 삭제했습니다.`); await load(); }
    catch (error) { setMessage(error.message); }
    finally { setBusy(""); }
  }

  return <main>
    <NavBar />
    <h1>사용자 문서</h1>
    <p>PDF 또는 DOCX 파일을 등록합니다. 같은 유형을 다시 올리면 기존 문서를 교체합니다.</p>
    {message && <p role="status">{message}</p>}
    {types.map(([path, code, label]) => {
      const saved = documents.find(document => document.documentType === code);
      const pendingFile = files[path];
      return <section key={code}>
        <h2>{label}</h2>
        {pendingFile ? <div className="document-file-summary document-file-summary--pending">
          <div className="document-file-info">
            <strong title={pendingFile.name}>{pendingFile.name}</strong>
            <small>{formatFileSize(pendingFile.size)}{saved ? " · 기존 문서 교체 예정" : " · 새 문서"}</small>
          </div>
          <span className="document-status document-status--pending">저장 대기</span>
        </div> : saved ? <>
          <div className="document-file-summary">
            <div className="document-file-info">
              <strong title={saved.originalFilename}>{saved.originalFilename}</strong>
              <small>{saved.fileFormat} · {formatFileSize(saved.fileSize)}</small>
            </div>
            <span className={`document-status document-status--${saved.processingStatus.toLowerCase()}`}>
              {statusLabels[saved.processingStatus] || saved.processingStatus}
            </span>
          </div>
          {saved.processingError && <p className="document-processing-error">{saved.processingError}</p>}
        </> : <p className="document-empty">등록된 문서가 없습니다.</p>}
        <div className="document-actions">
          <input ref={element => { fileInputs.current[path] = element; }} type="file" accept=".pdf,.docx" disabled={!!busy}
            onChange={event => setFiles(old => ({ ...old, [path]: event.target.files?.[0] || null }))} />
          {saved && <button className="button-compact button-danger" type="button" disabled={!!busy} onClick={() => remove(path, label)}>삭제</button>}
        </div>
      </section>;
    })}
    <button type="button" disabled={!Object.values(files).some(Boolean) || !!busy} onClick={save}>
      {busy === "save" ? "저장 중" : "저장"}
    </button>
    {saveProgress && <p role="status">{saveProgress}</p>}
  </main>;
}
