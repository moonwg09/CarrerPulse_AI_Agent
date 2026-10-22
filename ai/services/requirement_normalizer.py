def normalize_requirement_name(
    name: str,
    candidate_type: str
) -> str:

    normalized = name.strip()

    if candidate_type == "CERTIFICATE":

        if "관련 자격증" in normalized:
            return "관련 자격증"

        removable_words = [
            "보유자 우대",
            "보유자",
            "보유 우대",
            "보유",
            "자격증",
            "취득자",
            "취득",
            "우대",
        ]

        for word in removable_words:
            normalized = normalized.replace(word, "")

        normalized = normalized.strip()

    return normalized