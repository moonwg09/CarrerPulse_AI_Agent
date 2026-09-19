# CareerPulse AI

채용공고 분석 및 개인 맞춤형 학습계획을 제공하는 AI 기반 취업 지원 프로젝트입니다.

## 1. 기술 스택

| 구분       | 기술          | 버전      |
| -------- | ----------- | ------- |
| Backend  | Java        | 21.0.12 |
| Backend  | Spring Boot | 3.5.16  |
| Backend  | Gradle      | 9.6.0   |
| Frontend | Node.js     | 24.21.0 |
| Frontend | npm         | 11.19.0 |
| Frontend | React       | 19.3.0  |
| Frontend | Vite        | 8.3.0   |
| AI       | Python      | 3.11.16 |
| AI       | FastAPI     | 0.141.1 |
| AI       | Uvicorn     | 0.53.0  |
| Database | MySQL       | 8.4.11  |

## 2. 프로젝트 구조

```text
CareerPulse-AI/
├── frontend/       # React
├── backend/        # Spring Boot
├── ai/             # Python FastAPI
├── .env.example    # 환경 변수 예시
└── README.md
```

## 3. 개발 환경 준비

필수 설치 프로그램:

* JDK 21
* Node.js 24.21.0
* Git
* IntelliJ IDEA 또는 Java 개발이 가능한 IDE
* Visual Studio Code
* Anaconda 또는 Miniconda
* MySQL 8.4

기존 JDK 17을 사용하는 개발자는 CareerPulse AI 실행 시 JDK 21을 선택해야 합니다.

IntelliJ에서는 Project SDK와 Gradle JVM을 JDK 21로 설정합니다.

터미널에서 Gradle을 실행하는 경우에도 JAVA_HOME이 JDK 21을 가리키는지 확인합니다.

## 4. Frontend 실행

```bash
cd frontend
npm ci
npm run dev
```

기본 개발 주소:

http://localhost:5173

## 5. AI 서버 실행

```bash
cd ai
conda env create -f environment.yml
conda activate careerpulse
python -m uvicorn main:app --reload --port 8000
```

이미 careerpulse 환경이 생성되어 있다면 환경 생성 명령은 생략합니다.

API 문서:

http://127.0.0.1:8000/docs

## 6. MySQL 설정

MySQL 서버를 설치하고 실행합니다.

로컬 개발용 데이터베이스:

```sql
CREATE DATABASE IF NOT EXISTS careerpulse_test
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;
```

프로젝트 전용 MySQL 계정을 생성하고 필요한 권한을 부여합니다.

DB 계정과 비밀번호는 팀에서 별도로 안전하게 전달합니다.

## 7. 환경 변수 설정

`.env.example`을 참고하여 개인 환경에 필요한 변수를 설정합니다.

| 변수             | 설명              |
| -------------- | --------------- |
| DB_USERNAME    | MySQL 사용자       |
| DB_PASSWORD    | MySQL 비밀번호      |
| AI_SERVER_URL  | FastAPI 서버 주소   |
| GEMINI_API_KEY | Gemini API 인증 키 |

실제 비밀번호와 API 키는 GitHub에 업로드하지 않습니다.

`.env.example`은 안내용 파일이며, 자동으로 환경 변수를 등록하지 않습니다.

Windows PowerShell에서 Spring Boot를 실행하는 경우:

```powershell
$env:DB_USERNAME = "careerpulse"
$env:DB_PASSWORD = [System.Net.NetworkCredential]::new(
    "",
    (Read-Host "MySQL 비밀번호" -AsSecureString)
).Password
$env:AI_SERVER_URL = "http://127.0.0.1:8000"
```

현재 터미널에서만 설정되므로 새 터미널에서는 다시 설정해야 합니다.

## 8. Backend 실행

```powershell
cd backend
.\gradlew.bat bootRun
```

기본 주소:

http://localhost:8080

## 9. 연동 테스트

다음 주소에서 각각 정상 응답을 확인합니다.

| 주소                                    | 확인 대상                 |
| ------------------------------------- | --------------------- |
| http://localhost:8080/api/test        | Spring Boot           |
| http://localhost:8080/api/python/test | Spring Boot ↔ FastAPI |
| http://localhost:8080/api/db/test     | Spring Boot ↔ MySQL   |
| http://localhost:5173                 | React 전체 연동 테스트       |

테스트 API는 개발 환경 검증용이며, 실제 서비스 배포 전 제거하거나 접근을 제한해야 합니다.

## 10. 개발 시 주의 사항

* 각 프로젝트의 의존성 버전은 팀 협의 없이 변경하지 않습니다.
* frontend/package-lock.json을 Git으로 관리합니다.
* backend의 Gradle Wrapper를 사용합니다.
* Python 패키지를 변경하면 environment.yml도 갱신합니다.
* 실제 API 키와 DB 비밀번호를 GitHub에 업로드하지 않습니다.
* 기능 개발은 별도 브랜치에서 진행하고 Pull Request로 병합합니다.
