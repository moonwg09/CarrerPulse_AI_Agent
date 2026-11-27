import { useEffect } from "react";

export default function PageToast({ message, onClose }) {
  useEffect(() => {
    if (!message) return undefined;
    const timer = window.setTimeout(() => onClose(""), 3000);
    return () => window.clearTimeout(timer);
  }, [message, onClose]);

  if (!message) return null;

  return <div className="page-toast" role="status">
    <span>{message}</span>
    <button type="button" onClick={() => onClose("")}>닫기</button>
  </div>;
}
