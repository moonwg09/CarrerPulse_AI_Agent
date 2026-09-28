import { Link } from "react-router-dom";

function NavBar() {
  return (
    <nav>
      <Link to="/mypage">내 정보</Link>
      {" | "}
      <Link to="/jobs">관심 직무</Link>
      {" | "}
      <Link to="/account">계정 관리</Link>
    </nav>
  );
}

export default NavBar;