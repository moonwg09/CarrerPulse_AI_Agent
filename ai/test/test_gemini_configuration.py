import os
from unittest.mock import patch

from services.gemini_client import DEFAULT_GEMINI_MODEL, get_gemini_client


def test_missing_api_key_fails_at_call_time():
    with patch.dict(os.environ, {}, clear=True):
        try:
            get_gemini_client()
        except RuntimeError as exc:
            assert "GEMINI_API_KEY" in str(exc)
        else:
            raise AssertionError("API 키가 없으면 명시적인 오류가 발생해야 합니다.")


def test_default_model_is_explicit():
    assert DEFAULT_GEMINI_MODEL == "gemini-3.5-flash-lite"


if __name__ == "__main__":
    test_missing_api_key_fails_at_call_time()
    test_default_model_is_explicit()
    print("GEMINI CONFIGURATION TEST: PASS")
