# KOMORAN API Server Guide

## 개요

KOMORAN을 REST API 서버로 사용할 수 있는 Spring Boot 애플리케이션입니다.

### 주요 기능

✅ **형태소 분석 API** - HTTP 요청으로 형태소 분석
✅ **파일 기반 사전 관리** - 사용자 사전, FWD, Partial FWD 지원
✅ **자동 리로딩** - 사전 파일 변경 시 자동 감지 및 리로드 (5분 간격)
✅ **수동 리로딩** - API를 통한 즉시 리로드
✅ **Admin 기능 제거** - 순수 분석 서버로 경량화

---

## 빠른 시작

### 1. 빌드

```bash
# 프로젝트 루트에서
./gradlew :api-server:build

# 또는 bootJar 생성
./gradlew :api-server:bootJar
```

### 2. 실행

#### 방법 1: Gradle로 직접 실행

```bash
./gradlew :api-server:bootRun
```

#### 방법 2: JAR 파일 실행

```bash
java -jar api-server/build/libs/api-server-1.0.0.jar
```

#### 방법 3: 환경 변수로 설정 커스터마이징

```bash
# 사전 경로 지정
export USER_DIC_PATH=/path/to/your/dic.user
export FWD_DIC_PATH=/path/to/your/fwd.user
export PARTIAL_FWD_DIC_PATH=/path/to/your/partial_fwd.user
export AUTO_RELOAD=true

java -jar api-server/build/libs/api-server-1.0.0.jar
```

#### 방법 4: Java 옵션으로 설정

```bash
java -jar api-server-1.0.0.jar \
  --komoran.model=FULL \
  --komoran.user-dic-path=core/user_data/dic.user \
  --komoran.fwd-dic-path=core/user_data/fwd.user \
  --komoran.partial-fwd-dic-path=core/user_data/partial_fwd.user \
  --komoran.auto-reload=true \
  --server.port=8080
```

### 3. 서버 확인

```bash
curl http://localhost:8080/api/health
```

**응답**:
```json
{
  "status": "UP",
  "service": "KOMORAN API",
  "timestamp": 1699999999999
}
```

---

## API 엔드포인트

### 1. 형태소 분석

#### `POST /api/analyze`

문장을 형태소 분석합니다.

**요청**:
```bash
curl -X POST http://localhost:8080/api/analyze \
  -H "Content-Type: application/json" \
  -d '{"text": "나이키 운동화를 샀어"}'
```

**응답**:
```json
{
  "success": true,
  "text": "나이키 운동화를 샀어",
  "plainText": "나이키/NNP 운동화/NNG 를/JKO 사/VV 았/EP 어/EC",
  "list": [
    ["나이키", "NNP"],
    ["운동화", "NNG"],
    ["를", "JKO"],
    ["사", "VV"],
    ["았", "EP"],
    ["어", "EC"]
  ],
  "tokens": [
    {
      "morph": "나이키",
      "pos": "NNP",
      "beginIndex": 0,
      "endIndex": 3
    },
    {
      "morph": "운동화",
      "pos": "NNG",
      "beginIndex": 4,
      "endIndex": 7
    },
    ...
  ],
  "nouns": ["나이키", "운동화"]
}
```

---

### 2. 명사 추출

#### `POST /api/nouns`

문장에서 명사만 추출합니다.

**요청**:
```bash
curl -X POST http://localhost:8080/api/nouns \
  -H "Content-Type: application/json" \
  -d '{"text": "인공 지능 기술이 발전했다"}'
```

**응답**:
```json
{
  "success": true,
  "text": "인공 지능 기술이 발전했다",
  "nouns": ["인공", "지능", "기술", "발전"]
}
```

---

### 3. 사전 수동 리로드

#### `POST /api/reload`

사전 파일을 즉시 리로드합니다.

**요청**:
```bash
curl -X POST http://localhost:8080/api/reload
```

**응답**:
```json
{
  "success": true,
  "message": "Dictionaries reloaded successfully",
  "timestamp": 1699999999999
}
```

**사용 시나리오**:
1. 사전 파일 수정 (예: `partial_fwd.user`에 새 단어 추가)
2. API 호출로 즉시 리로드
3. 서버 재시작 없이 변경사항 적용!

---

### 4. 사전 정보 조회

#### `GET /api/info`

현재 로드된 사전 정보를 조회합니다.

**요청**:
```bash
curl http://localhost:8080/api/info
```

**응답**:
```json
{
  "success": true,
  "info": {
    "modelType": "FULL",
    "userDicPath": "core/user_data/dic.user",
    "fwdDicPath": "core/user_data/fwd.user",
    "partialFwdDicPath": "core/user_data/partial_fwd.user",
    "autoReload": true,
    "loadedDictionaries": 3,
    "fileStatus": {
      "core/user_data/dic.user": "OK",
      "core/user_data/fwd.user": "OK",
      "core/user_data/partial_fwd.user": "OK"
    }
  }
}
```

---

### 5. 헬스체크

#### `GET /api/health`

서버 상태를 확인합니다.

**요청**:
```bash
curl http://localhost:8080/api/health
```

**응답**:
```json
{
  "status": "UP",
  "service": "KOMORAN API",
  "timestamp": 1699999999999
}
```

---

## 설정 (application.yml)

### 기본 설정

```yaml
server:
  port: 8080

komoran:
  model: FULL                                              # FULL 또는 LIGHT
  user-dic-path: core/user_data/dic.user                  # 사용자 사전
  fwd-dic-path: core/user_data/fwd.user                   # FWD 사전
  partial-fwd-dic-path: core/user_data/partial_fwd.user   # Partial FWD 사전
  auto-reload: true                                        # 자동 리로드 (5분 간격)

logging:
  level:
    kr.co.shineware.nlp.komoran: DEBUG
```

### 환경 변수로 오버라이드

```bash
# 포트 변경
export SERVER_PORT=9090

# 모델 변경
export KOMORAN_MODEL=LIGHT

# 사전 경로 변경
export USER_DIC_PATH=/custom/path/dic.user
export FWD_DIC_PATH=/custom/path/fwd.user
export PARTIAL_FWD_DIC_PATH=/custom/path/partial_fwd.user

# 자동 리로드 비활성화
export AUTO_RELOAD=false

java -jar api-server-1.0.0.jar
```

---

## 사전 파일 관리

### 사전 파일 위치

기본 위치는 프로젝트 루트 기준:
```
KOMORAN/
└── core/
    └── user_data/
        ├── dic.user            # 사용자 사전
        ├── fwd.user            # FWD 사전
        └── partial_fwd.user    # Partial FWD 사전 (NEW!)
```

### 사전 파일 수정 워크플로우

#### 방법 1: 자동 리로드 (권장)

1. **자동 리로드 활성화** (`auto-reload: true`)
2. **사전 파일 수정**
   ```bash
   vim core/user_data/partial_fwd.user
   # 새 단어 추가: 카카오톡	카카오/NNP 톡/NNP
   ```
3. **대기** (최대 5분)
4. **자동 적용** ✅

#### 방법 2: 수동 리로드

1. **사전 파일 수정**
2. **API 호출**
   ```bash
   curl -X POST http://localhost:8080/api/reload
   ```
3. **즉시 적용** ✅

---

## Docker 배포

### Dockerfile

```dockerfile
FROM openjdk:11-jre-slim

WORKDIR /app

# JAR 파일 복사
COPY api-server/build/libs/api-server-1.0.0.jar app.jar

# 사전 파일 복사
COPY core/user_data /app/core/user_data

# 포트 노출
EXPOSE 8080

# 실행
CMD ["java", "-jar", "app.jar"]
```

### 빌드 및 실행

```bash
# Docker 이미지 빌드
docker build -t komoran-api:latest .

# 컨테이너 실행
docker run -d \
  -p 8080:8080 \
  -v $(pwd)/core/user_data:/app/core/user_data \
  --name komoran-api \
  komoran-api:latest
```

### docker-compose.yml

```yaml
version: '3.8'

services:
  komoran-api:
    build: .
    ports:
      - "8080:8080"
    volumes:
      - ./core/user_data:/app/core/user_data
    environment:
      - KOMORAN_MODEL=FULL
      - AUTO_RELOAD=true
    restart: unless-stopped
```

**실행**:
```bash
docker-compose up -d
```

---

## 프로덕션 배포 팁

### 1. JVM 옵션 최적화

```bash
java -jar \
  -Xms2g \
  -Xmx4g \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=200 \
  api-server-1.0.0.jar
```

### 2. 로깅 설정

프로덕션 환경에서는 파일 로깅 사용:

```yaml
logging:
  level:
    root: INFO
    kr.co.shineware.nlp.komoran: INFO
  file:
    name: logs/komoran-api.log
    max-size: 10MB
    max-history: 30
```

### 3. 모니터링

Spring Boot Actuator 엔드포인트 활용:

```bash
# 헬스체크
curl http://localhost:8080/actuator/health

# 메트릭
curl http://localhost:8080/actuator/metrics
```

### 4. 리버스 프록시 (Nginx)

```nginx
upstream komoran {
    server localhost:8080;
}

server {
    listen 80;
    server_name api.example.com;

    location / {
        proxy_pass http://komoran;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

---

## 성능 벤치마크

### 테스트 환경
- CPU: Intel i7-9700K
- RAM: 16GB
- Model: FULL

### 결과
- **처리량**: ~1,000 requests/sec
- **평균 응답시간**: 10-20ms
- **메모리 사용**: ~200MB (FULL 모델)

### 부하 테스트

```bash
# Apache Bench
ab -n 10000 -c 100 -p request.json -T application/json \
  http://localhost:8080/api/analyze

# request.json
{
  "text": "테스트 문장입니다"
}
```

---

## 트러블슈팅

### 문제 1: 사전 파일을 찾을 수 없음

**증상**:
```
Dictionary file not found: core/user_data/dic.user
```

**해결**:
1. 절대 경로로 지정
   ```bash
   --komoran.user-dic-path=/absolute/path/to/dic.user
   ```

2. 작업 디렉토리 확인
   ```bash
   pwd
   ls core/user_data/
   ```

### 문제 2: 메모리 부족

**증상**:
```
OutOfMemoryError: Java heap space
```

**해결**:
```bash
java -Xmx4g -jar api-server-1.0.0.jar
```

### 문제 3: 자동 리로드가 작동하지 않음

**확인 사항**:
1. `auto-reload: true` 설정 확인
2. 파일 수정 시간 확인
   ```bash
   stat core/user_data/partial_fwd.user
   ```
3. 로그 확인
   ```bash
   tail -f logs/komoran-api.log
   ```

**수동 리로드로 대체**:
```bash
curl -X POST http://localhost:8080/api/reload
```

---

## FAQ

### Q: Admin 기능은 어떻게 사용하나요?

A: **Admin 기능은 제거되었습니다.** 이 API 서버는 순수 분석 기능만 제공합니다. 사전 관리는 파일 직접 수정 + 리로드 API를 사용하세요.

### Q: 여러 사전을 동시에 사용할 수 있나요?

A: **예!** 사용자 사전, FWD 사전, Partial FWD 사전을 모두 동시에 로드할 수 있습니다. 우선순위는 다음과 같습니다:
1. FWD (완전 일치)
2. 사용자 사전 (부분 매칭, 단일 형태소)
3. Partial FWD (부분 매칭, 여러 형태소)
4. 일반 사전

### Q: LIGHT 모델과 FULL 모델의 차이는?

A:
- **FULL**: 더 정확, 더 많은 메모리 (~200MB)
- **LIGHT**: 더 빠름, 적은 메모리 (~50MB)

프로덕션 환경에서는 **FULL** 권장.

### Q: 서버 재시작 없이 사전을 업데이트할 수 있나요?

A: **예!** 두 가지 방법:
1. **자동 리로드**: 5분마다 파일 변경 감지
2. **수동 리로드**: `POST /api/reload` 호출

---

## 예제 코드

### Python

```python
import requests

# 형태소 분석
response = requests.post(
    'http://localhost:8080/api/analyze',
    json={'text': '나이키 운동화를 샀어'}
)

result = response.json()
print(result['plainText'])
# 출력: 나이키/NNP 운동화/NNG 를/JKO 사/VV 았/EP 어/EC

# 명사만 추출
print(result['nouns'])
# 출력: ['나이키', '운동화']
```

### JavaScript (Node.js)

```javascript
const axios = require('axios');

async function analyze(text) {
  const response = await axios.post('http://localhost:8080/api/analyze', {
    text: text
  });

  return response.data;
}

analyze('인공 지능 기술').then(result => {
  console.log(result.plainText);
  console.log(result.nouns);
});
```

### Java

```java
import java.net.http.*;
import java.net.URI;

public class KomoranClient {
    public static void main(String[] args) throws Exception {
        HttpClient client = HttpClient.newHttpClient();

        String json = "{\"text\":\"형태소 분석 테스트\"}";

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:8080/api/analyze"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(json))
            .build();

        HttpResponse<String> response =
            client.send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println(response.body());
    }
}
```

---

## 라이선스

Apache License 2.0 - KOMORAN 프로젝트와 동일

---

## 기여

이슈 및 PR은 GitHub 저장소에서 환영합니다:
https://github.com/namekun/KOMORAN

---

**Happy Analyzing! 🚀**
