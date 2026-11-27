import { useEffect, useState } from "react";
import { Link, NavLink } from "react-router-dom";
import { getMyInfo } from "../api/authApi";

const links = [
  ["/mypage", "내 정보"],
  ["/jobs", "관심 직무"],
  ["/documents", "문서 관리"],
  ["/analysis", "서류·공고 비교"],
  ["/recommendations", "추천 공고"],
  ["/bookmarks", "관심 공고"],
  ["/learning", "준비 역량 분석"],
  ["/applications", "지원 현황"],
  ["/account", "계정 관리"],
];

function NavBar({ publicPage = false }) {
  const [userName, setUserName] = useState("");

  useEffect(() => {
    if (publicPage) return;
    let active = true;
    getMyInfo()
      .then((user) => { if (active) setUserName(user.name || "USER"); })
      .catch(() => { if (active) setUserName("USER"); });
    return () => { active = false; };
  }, [publicPage]);

  return (
    <header className="site-header">
      <div className="brand-bar">
        <Link className="brand-name" to={publicPage ? "/login" : "/mypage"}>CAREERPULSE</Link>
        <span className="user-label">{publicPage ? "GUEST" : (userName || "USER")}</span>
      </div>
      {!publicPage && (
        <nav className="primary-nav" aria-label="주요 메뉴">
          {links.map(([path, label]) => (
            <NavLink key={path} to={path} className={({ isActive }) => isActive ? "active" : ""}>
              {label}
            </NavLink>
          ))}
        </nav>
      )}
    </header>
  );
}

export default NavBar;
