# 🌊 Ripple — SNS Backend (Spring Boot, Java 21)

## 🎯 프로젝트 목적

Ripple은 **모바일 앱 전용**(웹 미구현/계획 없음) **인스타그램 유사 SNS**의 백엔드 서버입니다. 사용자·피드·댓글·메시지 등 핵심 도메인을 안정적으로 제공하고, **JWT 기반 인증**과 **확장 가능한 아키텍처**로 프론트엔드 앱과 알고리즘 서비스(FastAPI)와의 유연한 연동을 목표로 합니다.

또한 **참여 유도 규칙**을 핵심 정책으로 도입합니다:

* 피드를 새로고침(갱신)할 때 **“피드 5개당 댓글 ≥ 1개”** 조건을 충족하지 않으면 **추가 갱신을 중단**하도록 설계합니다.

## 🧱 기술 스택

* **Language/Runtime**: Java 21
* **Framework**: Spring Boot 3.x
* **Security**: Spring Security, JWT
* **Persistence**: JPA (Hibernate), PostgreSQL
* **Cache/Token Store**: Redis
* **Object Storage**: AWS S3
* **Algorithm Service (옵션)**: FastAPI (Python)
* **Infra/DevOps**: Docker, Docker Compose, GitHub Actions
