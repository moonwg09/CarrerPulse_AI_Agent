"""텍스트 정리."""
import re


def clean_text(text: str) -> str:
    """추출한 텍스트의 공백과 빈 줄을 정리한다."""
    # PDF에서 나오는 줄바꿈 없는 공백( )과 연속 공백·빈 줄을 정리한다.
    # 같은 내용이 공백 차이 때문에 다른 문장으로 보이면 검색 품질이 떨어진다.
    text = text.replace(" ", " ")
    text = re.sub(r"[ \t]+", " ", text)
    text = re.sub(r"\n{3,}", "\n\n", text)
    return text.strip()
