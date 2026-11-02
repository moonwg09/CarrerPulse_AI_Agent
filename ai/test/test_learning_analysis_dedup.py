from datetime import date

from services.learning_analysis import AnalysisRequest, analyze


def main():
    request = AnalysisRequest.model_validate({
        "referenceDate": date(2026, 11, 2),
        "interests": ["SELECTED"],
        "assessments": [],
        "jobs": [
            {
                "jobId": "1",
                "jobCategory": "SELECTED",
                "postedDate": "2026-11-01",
                "status": "OPEN",
                "requirements": [
                    {"name": "C#", "type": "SKILL", "category": "REQUIRED"},
                    {"name": "C# 개발 경험", "type": "EXPERIENCE", "category": "PREFERRED"},
                    {"name": "C# / .NET 활용", "type": "EXPERIENCE", "category": "PREFERRED"},
                    {"name": "C++", "type": "SKILL", "category": "REQUIRED"},
                    {"name": "C++ 활용 경험", "type": "EXPERIENCE", "category": "PREFERRED"},
                    {"name": "SQL", "type": "SKILL", "category": "REQUIRED"},
                    {"name": "SQLD", "type": "CERTIFICATE", "category": "PREFERRED"},
                    {"name": "Java", "type": "SKILL", "category": "REQUIRED"},
                    {"name": "JavaScript", "type": "SKILL", "category": "PREFERRED"},
                ],
            }
        ],
    })

    names = [item["name"] for item in analyze(request)["needsConfirmation"]]
    assert names.count("C#") == 1
    assert "C# 개발 경험" not in names
    assert "C# / .NET 활용" not in names
    assert names.count("C++") == 1
    assert "C++ 활용 경험" not in names
    assert "SQL" in names and "SQLD" in names
    assert "Java" in names and "JavaScript" in names
    print("LEARNING ANALYSIS DEDUP TEST: PASS")


if __name__ == "__main__":
    main()
