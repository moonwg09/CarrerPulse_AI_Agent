import os

from dotenv import load_dotenv


load_dotenv()

DEFAULT_GEMINI_MODEL = "gemini-3.5-flash-lite"
GEMINI_MODEL = os.getenv("GEMINI_MODEL", DEFAULT_GEMINI_MODEL)


def get_gemini_client():
    """Gemini 기능을 실제 호출할 때만 SDK client를 생성한다."""
    api_key = os.getenv("GEMINI_API_KEY")
    if not api_key:
        raise RuntimeError("GEMINI_API_KEY가 설정되어 있지 않습니다.")

    try:
        from google import genai
    except ImportError as exc:
        raise RuntimeError("google-genai 패키지가 설치되어 있지 않습니다.") from exc

    return genai.Client(api_key=api_key)
