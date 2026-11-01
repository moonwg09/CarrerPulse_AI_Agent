import { useEffect, useState } from "react";
import NavBar from "../components/NavBar";
import { deleteDocument, getDocuments, uploadDocument } from "../api/documentApi";

const types = [
  ["resume", "RESUME", "이력서"],
  ["self-intro", "SELF_INTRO", "자기소개서"],
  ["portfolio", "PORTFOLIO", "포트폴리오"],
];

export default function DocumentsPage() {
  const [documents, setDocuments] = useState([]);
  const [files, setFiles] = useState({});
  const [busy, setBusy] = useState("");
  const [message, setMessage] = useState("");

  const load = async () => {
    try { setDocuments(await getDocuments()); }
    catch (error) { setMessage(error.message); }
  };
  useEffect(() => { getDocuments().then(setDocuments).catch(error => setMessage(error.message)); }, []);

  async function upload(path, label) {
    if (!files[path]) return;
    setBusy(path); setMessage("");
    try {
      const result = await uploadDocument(path, files[path]);
      setMessage(`${label} 처리 상태: ${result.processingStatus}`);
      setFiles(old => ({ ...old, [path]: null }));
      await load();
    } catch (error) { setMessage(error.message); }
    finally { setBusy(""); }
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
    <section>
      <h2>Git 저장소</h2>
      <p>미등록</p>
      <p>Git 저장소가 없어도 등록한 서류만으로 분석합니다.</p>
    </section>
    {message && <p role="status">{message}</p>}
    {types.map(([path, code, label]) => {
      const saved = documents.find(document => document.documentType === code);
      return <section key={code}>
        <h2>{label}</h2>
        {saved ? <p>{saved.originalFilename} · {saved.fileFormat} · {saved.fileSize} bytes · {saved.processingStatus}{saved.processingError ? ` · ${saved.processingError}` : ""}</p> : <p>등록된 문서가 없습니다.</p>}
        <input type="file" accept=".pdf,.docx" disabled={!!busy}
          onChange={event => setFiles(old => ({ ...old, [path]: event.target.files?.[0] || null }))} />
        <button type="button" disabled={!files[path] || !!busy} onClick={() => upload(path, label)}>{busy === path ? "처리 중" : "업로드·교체"}</button>{" "}
        {saved && <button type="button" disabled={!!busy} onClick={() => remove(path, label)}>삭제</button>}
      </section>;
    })}
  </main>;
}
