# KOMORAN API Server - Quick Start

Spring Boot 기반 KOMORAN 형태소 분석 REST API 서버입니다.

## 실행 방법

### 1. 사전 파일 경로 설정 (필수!)

사전 파일은 **절대 경로**로 지정해야 합니다:

```bash
# 프로젝트 루트 디렉토리에서
export CURRENT_DIR=$(pwd)
export USER_DIC_PATH=$CURRENT_DIR/core/user_data/dic.user
export FWD_DIC_PATH=$CURRENT_DIR/core/user_data/fwd.user
export PARTIAL_FWD_DIC_PATH=$CURRENT_DIR/core/user_data/partial_fwd.user
```

### 2. 서버 실행

```bash
# Gradle로 직접 실행
./gradlew :api-server:bootRun

# 또는 JAR 빌드 후 실행
./gradlew :api-server:bootJar
java -jar api-server/build/libs/api-server-1.0.0.jar
```

### 3. 테스트

```bash
# 헬스체크
curl http://localhost:8080/api/health

# 형태소 분석
curl -X POST http://localhost:8080/api/analyze \
  -H "Content-Type: application/json" \
  -d '{"text": "나이키 운동화를 샀어"}'
```

## 주요 기능

- ✅ REST API로 형태소 분석
- ✅ 파일 기반 사전 관리
- ✅ 자동 리로드 (5분 간격)
- ✅ 수동 리로드 API
- ✅ Partial FWD 지원
- ✅ Admin 기능 제거 (경량화)

## API 엔드포인트

| 엔드포인트 | 메서드 | 설명 |
|-----------|--------|------|
| `/api/analyze` | POST | 형태소 분석 |
| `/api/nouns` | POST | 명사 추출 |
| `/api/reload` | POST | 사전 리로드 |
| `/api/info` | GET | 사전 정보 |
| `/api/health` | GET | 헬스체크 |

## 상세 가이드

전체 문서는 [API_SERVER_GUIDE.md](../API_SERVER_GUIDE.md)를 참고하세요.

## Docker 실행

```bash
# 빌드
docker build -t komoran-api .

# 실행 (사전 파일 마운트)
docker run -d -p 8080:8080 \
  -e USER_DIC_PATH=/app/core/user_data/dic.user \
  -e FWD_DIC_PATH=/app/core/user_data/fwd.user \
  -e PARTIAL_FWD_DIC_PATH=/app/core/user_data/partial_fwd.user \
  -v $(pwd)/core/user_data:/app/core/user_data \
  komoran-api
```

## 환경 변수

| 변수 | 설명 | 기본값 |
|------|------|--------|
| `KOMORAN_MODEL` | 모델 타입 (FULL/LIGHT) | FULL |
| `USER_DIC_PATH` | 사용자 사전 경로 | (없음) |
| `FWD_DIC_PATH` | FWD 사전 경로 | (없음) |
| `PARTIAL_FWD_DIC_PATH` | Partial FWD 사전 경로 | (없음) |
| `AUTO_RELOAD` | 자동 리로드 활성화 | true |
| `SERVER_PORT` | 서버 포트 | 8080 |

## 주의사항

⚠️ **사전 파일 경로는 절대 경로로 지정하세요!**

```bash
# ❌ 잘못된 예 (상대 경로)
export USER_DIC_PATH=core/user_data/dic.user

# ✅ 올바른 예 (절대 경로)
export USER_DIC_PATH=/home/user/KOMORAN/core/user_data/dic.user

# ✅ 또는 현재 디렉토리 기준
export USER_DIC_PATH=$(pwd)/core/user_data/dic.user
```
