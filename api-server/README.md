# KOMORAN API Server

Spring Boot 기반 KOMORAN 형태소 분석 REST API 서버입니다.

## 빠른 시작

```bash
# 빌드 및 실행
./gradlew :api-server:bootRun

# 테스트
curl -X POST http://localhost:8080/api/analyze \
  -H "Content-Type: application/json" \
  -d '{"text": "나이키 운동화를 샀어"}'
```

## 주요 기능

- ✅ REST API로 형태소 분석
- ✅ 사용자 사전, FWD, Partial FWD 지원
- ✅ 파일 기반 사전 관리
- ✅ 자동/수동 리로드 지원
- ✅ Admin 기능 제거 (경량화)

## 상세 가이드

전체 문서는 [API_SERVER_GUIDE.md](../API_SERVER_GUIDE.md)를 참고하세요.

## API 엔드포인트

- `POST /api/analyze` - 형태소 분석
- `POST /api/nouns` - 명사 추출
- `POST /api/reload` - 사전 리로드
- `GET /api/info` - 사전 정보
- `GET /api/health` - 헬스체크

## 설정

`src/main/resources/application.yml` 또는 환경 변수로 설정:

```bash
export KOMORAN_MODEL=FULL
export USER_DIC_PATH=core/user_data/dic.user
export PARTIAL_FWD_DIC_PATH=core/user_data/partial_fwd.user
export AUTO_RELOAD=true

java -jar api-server-1.0.0.jar
```
