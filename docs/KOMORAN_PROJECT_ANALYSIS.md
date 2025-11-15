# KOMORAN 프로젝트 분석 문서

## 목차
1. [프로젝트 개요](#프로젝트-개요)
2. [프로젝트 구조](#프로젝트-구조)
3. [Core 모듈 상세 분석](#core-모듈-상세-분석)
4. [Admin 모듈 분석](#admin-모듈-분석)
5. [Elasticsearch Plugin 모듈 분석](#elasticsearch-plugin-모듈-분석)
6. [빌드 및 의존성](#빌드-및-의존성)
7. [핵심 알고리즘](#핵심-알고리즘)
8. [사용 예시](#사용-예시)
9. [참고 자료](#참고-자료)

---

## 프로젝트 개요

**KOMORAN (KOrean MORphological ANalyzer)** 은 Java로 구현된 한국어 형태소 분석기입니다.

### 주요 특징

- **순수 Java 구현**: 100% Java로 개발되어 자바가 설치된 모든 환경에서 사용 가능
- **외부 라이브러리 독립적**: 자체 제작 라이브러리만 사용하여 의존성 문제 없음
- **경량화**: 자소 단위 처리와 TRIE 사전으로 약 50MB 메모리에서도 동작
- **사용 편의성**: 간단한 API로 1줄의 코드만으로 형태소 분석 가능
- **사전 관리 용이**: 일반 텍스트 파일 형태로 구성되어 가독성 높고 편집 용이
- **공백 포함 분석**: 타 형태소 분석기와 달리 공백이 포함된 형태소 단위 분석 가능

### 개발자 및 인용

```
@misc{komoran,
author = {Junsoo Shin, Junghwan Park, Geunho Lee},
title = {komoran},
publisher = {GitHub},
journal = {GitHub repository},
howpublished = {\url{https://github.com/shineware/KOMORAN}}
```

### 버전 정보
- **현재 버전**: KOMORAN 3.0
- **Gradle 버전**: 7.3.3
- **Java 버전**: Java 8 이상 지원

---

## 프로젝트 구조

KOMORAN은 Gradle 기반의 멀티 모듈 프로젝트로 구성되어 있습니다.

```
KOMORAN/
├── core/                      # 핵심 형태소 분석 엔진
│   ├── src/main/java/         # Java 소스 코드
│   ├── models_full/           # 전체 모델 데이터
│   ├── models_light/          # 경량 모델 데이터
│   ├── corpus_build/          # 말뭉치 빌드 데이터
│   ├── resources/             # 리소스 파일
│   └── user_data/             # 사용자 정의 데이터
│
├── admin/                     # 웹 기반 관리 도구 (Spring Boot)
│   └── src/main/java/
│       └── kr/co/shineware/nlp/komoran/admin/
│           ├── controller/    # REST API 컨트롤러
│           ├── service/       # 비즈니스 로직
│           ├── domain/        # 도메인 모델
│           └── util/          # 유틸리티 클래스
│
├── elasticsearch-plugin/      # Elasticsearch 연동 플러그인
│   └── src/main/java/
│       └── kr/co/shineware/nlp/
│           ├── lucene/tokenizer/      # Lucene 토크나이저
│           └── elasticsearch/         # Elasticsearch 플러그인
│
├── docs/                      # 문서 (HTML)
├── _rst/                      # reStructuredText 문서 소스
├── .github/                   # GitHub Actions 워크플로우
├── gradle/                    # Gradle Wrapper
├── README.md                  # 한국어 README
├── README.en.md               # 영어 README
├── settings.gradle            # Gradle 설정
└── LICENSE                    # Apache 2.0 라이선스
```

### 모듈 구성 (settings.gradle)

```gradle
rootProject.name = 'KOMORAN'
include 'core'
include 'admin'
include 'elasticsearch-plugin'
```

---

## Core 모듈 상세 분석

Core 모듈은 KOMORAN의 핵심 형태소 분석 엔진입니다.

### 주요 클래스 및 역할

#### 1. **Komoran.java** - 메인 분석기
**위치**: `core/src/main/java/kr/co/shineware/nlp/komoran/core/Komoran.java`

**주요 메서드**:
```java
// 단일 문장 분석
public KomoranResult analyze(String sentence)

// N-best 결과 반환
public List<KomoranResult> analyze(String sentence, int nbest)

// 배치 처리 (멀티스레드 지원)
public List<KomoranResult> analyze(List<String> sentences, int thread)

// 텍스트 파일 분석
public void analyzeTextFile(String inputFilename, String outputFilename, int thread)

// 사용자 사전 설정
public void setUserDic(String userDicPath)

// 전방 최장일치 사전 설정
public void setFWDic(String fwDicPath)
```

**분석 파이프라인**:
1. 전방 최장일치(FWD) 사전 조회
2. 정규 파싱 (일반 형태소)
3. 불규칙 파싱 (불규칙 활용)
4. 불규칙 확장
5. 기호 파싱 (숫자, 영어, 구두점)
6. 사용자 사전 파싱
7. 연속 기호 처리

#### 2. **Lattice.java** - 그래프 구조
**위치**: `core/src/main/java/kr/co/shineware/nlp/komoran/core/model/Lattice.java`

**데이터 구조**:
```java
private Map<Integer, List<LatticeNode>> lattice;  // 위치 → 노드 리스트
private Transition transition;                     // HMM 전이 확률
private Observation observation;                   // HMM 관측 확률
private IrregularTrie irregularTrie;               // 불규칙 형태소 트라이
```

**핵심 기능**:
- 가능한 모든 형태소 분할을 표현하는 방향성 비순환 그래프(DAG) 구축
- 비터비(Viterbi) 알고리즘을 사용한 최적 경로 탐색
- N-best 경로 탐색 지원

#### 3. **LatticeNode.java** - 그래프 노드
**위치**: `core/src/main/java/kr/co/shineware/nlp/komoran/core/model/LatticeNode.java`

**구조**:
```java
private int beginIdx;          // 시작 위치 (자소 단위)
private int endIdx;            // 종료 위치 (자소 단위)
private MorphTag morphTag;     // 형태소 + 품사 태그 + 태그 ID
private double score;          // 누적 HMM 점수
private int prevNodeIdx;       // 역추적용 이전 노드 인덱스
```

#### 4. **Resources.java** - 모델 컨테이너
**위치**: `core/src/main/java/kr/co/shineware/nlp/komoran/core/model/Resources.java`

**포함 구성 요소**:
```java
private Transition transition;         // P(tag_t | tag_{t-1})
private Observation observation;       // P(morph | tag)
private PosTable table;                // 품사 태그 ↔ ID 매핑
private IrregularTrie irrTrie;         // 불규칙 형태소 사전
```

#### 5. **KomoranResult.java** - 분석 결과
**위치**: `core/src/main/java/kr/co/shineware/nlp/komoran/model/KomoranResult.java`

**출력 메서드**:
```java
// "감기/NNG 는/JX 자주/MAG" 형식
public String getPlainText()

// 문자 위치 정보 포함 토큰 리스트
public List<Token> getTokenList()

// (형태소, 품사) 쌍 리스트
public List<Pair<String, String>> getList()

// 명사만 추출
public List<String> getNouns()

// 특정 품사만 추출
public List<String> getMorphesByTags(String... tags)
```

### 핵심 알고리즘

#### HMM (Hidden Markov Model) 기반 형태소 분석

KOMORAN은 바이그램(bigram) HMM을 사용합니다:

**1. 관측 모델 (Observation Model)**
```
P(형태소 | 품사태그)
```
특정 품사 태그가 주어졌을 때 형태소가 나타날 확률

**2. 전이 모델 (Transition Model)**
```
P(품사태그_t | 품사태그_{t-1})
```
이전 품사 태그가 주어졌을 때 다음 품사 태그가 나타날 확률

**점수 계산**:
```java
double transitionScore = resources.getTransition().get(prevTagId, currentTagId);
double totalScore = transitionScore + observationScore;
```

#### 자소 단위 처리

**KoreanUnitParser.java** 에서 한글 음절을 초성, 중성, 종성으로 분해:

```java
// 19개 초성: ㄱ, ㄲ, ㄴ, ㄷ, ㄸ, ...
public static char[] ChoSung = {0x3131, 0x3132, ...};

// 21개 중성: ㅏ, ㅐ, ㅑ, ㅒ, ...
public static char[] JungSung = {...};

// 28개 종성: (없음), ㄱ, ㄲ, ㄳ, ...
public static char[] JongSung = {0x0000, 0x3131, ...};
```

**예시**: "감" (U+AC10) → ㄱ + ㅏ + ㅁ

#### 결합 규칙 검사

**CombinationRuleChecker** 인터페이스로 한국어 문법 규칙 적용:

```java
public interface CombinationRuleChecker {
    boolean isValidRule(String prevMorph, int prevTagId,
                       String morph, int tagId);
}
```

**규칙 예시**:
- 명사 + 조사: 받침 유무에 따라 "이/가", "을/를" 선택
- 동사 + 어미: 활용 규칙 검증
- 품사 태그 전이 제약 (예: NNG → VV 불가)

### 데이터 구조

#### Transition Matrix
```java
private double[][] scoreMatrix;  // scoreMatrix[이전품사ID][현재품사ID]
```
GZIP 압축된 직렬화된 2D 배열로 저장하여 메모리 효율성 확보

#### Observation Dictionary
Aho-Corasick 자동기계(Automaton)를 사용하여 효율적인 문자열 매칭:
- 시간 복잡도: O(n + m + z)
  - n: 입력 텍스트 길이
  - m: 패턴 길이
  - z: 매칭 개수

### 분석 과정 예시

**입력**: "감기는 자주 걸리는 병이다"

**처리 단계**:
1. 자소 단위로 변환
2. 각 위치에서 가능한 모든 형태소 후보 찾기
3. 격자 그래프 구축
4. HMM 확률과 결합 규칙으로 점수 계산
5. 비터비 알고리즘으로 최적 경로 탐색
6. 형태소-품사 쌍으로 출력

**출력**:
```
감기/NNG 는/JX 자주/MAG 걸리/VV 는/ETM 병/NNG 이/VCP 다/EF
```

### 파일 구조
```
core/src/main/java/kr/co/shineware/nlp/komoran/
├── core/
│   ├── Komoran.java                    # 메인 분석기
│   └── model/
│       ├── Lattice.java                # 격자 그래프
│       ├── LatticeNode.java            # 그래프 노드
│       ├── Resources.java              # 모델 컨테이너
│       ├── MorphUtil.java              # 형태소 유틸리티
│       ├── TagUtil.java                # 태그 유틸리티
│       ├── ContinuousSymbolBuffer.java # 연속 기호 버퍼
│       └── combinationrules/
│           ├── CombinationRuleChecker.java           # 인터페이스
│           └── MergedCombinationRuleChecker.java     # 구현체
├── model/
│   ├── KomoranResult.java              # 분석 결과
│   ├── MorphTag.java                   # 형태소-태그 쌍
│   ├── ScoredTag.java                  # 점수화된 태그
│   └── Token.java                      # 토큰 (위치 정보 포함)
├── modeler/model/
│   ├── PosTable.java                   # 품사 테이블
│   ├── Observation.java                # 관측 모델
│   ├── Transition.java                 # 전이 모델
│   ├── IrregularTrie.java              # 불규칙 트라이
│   └── IrregularNode.java              # 불규칙 노드
├── parser/
│   └── KoreanUnitParser.java           # 자소 파서
└── interfaces/
    ├── UnitParser.java                 # 파서 인터페이스
    └── FileAccessible.java             # 파일 접근 인터페이스
```

---

## Admin 모듈 분석

Admin 모듈은 **Spring Boot 2.6.14** 기반의 웹 애플리케이션으로, KOMORAN의 사전과 모델을 웹 인터페이스로 관리할 수 있는 도구입니다.

### 기술 스택
- **프레임워크**: Spring Boot 2.6.14
- **데이터베이스**: H2 (인메모리)
- **ORM**: Spring Data JPA
- **유틸리티**:
  - JSON 처리: json-simple 1.1.1
  - Diff 비교: java-diff-utils 4.0
  - ZIP 처리: zip4j 2.1.3

### 아키텍처

#### MVC 구조
```
admin/src/main/java/kr/co/shineware/nlp/komoran/admin/
├── controller/           # REST API 엔드포인트
├── service/             # 비즈니스 로직
├── domain/              # JPA 엔티티
└── util/                # 유틸리티 및 파서
```

### 주요 컨트롤러

#### 1. **MorphAnalyzeController**
형태소 분석 API 제공
```java
@RestController
@RequestMapping("/analyze")
public class MorphAnalyzeController {
    // 문장 분석
    // 모델 비교
    // N-best 결과 제공
}
```

#### 2. **DicWordController**
기본 사전 관리
```java
@RestController
@RequestMapping("/dic/word")
public class DicWordController {
    // 사전 단어 조회
    // 사전 단어 추가/수정/삭제
    // 필터링 및 정렬
}
```

#### 3. **DicUserController**
사용자 사전 관리
```java
@RestController
@RequestMapping("/dic/user")
public class DicUserController {
    // 사용자 정의 단어 관리
    // CSV 업로드/다운로드
}
```

#### 4. **GrammarInController**
문법 규칙 관리
```java
@RestController
@RequestMapping("/grammar")
public class GrammarInController {
    // 결합 규칙 관리
    // 불규칙 활용 규칙
}
```

#### 5. **UserModelController**
사용자 모델 관리
```java
@RestController
@RequestMapping("/model")
public class UserModelController {
    // 커스텀 모델 업로드
    // 모델 학습 데이터 관리
    // 모델 배포 (ZIP 패키징)
}
```

### 주요 도메인 모델

#### DicWord
```java
@Entity
public class DicWord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String word;        // 단어
    private String pos;         // 품사
    private String meaning;     // 의미
    private Double frequency;   // 빈도수
}
```

#### DicUser
```java
@Entity
public class DicUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String morph;       // 형태소
    private String pos;         // 품사
    private String type;        // 사전 유형
}
```

#### GrammarType
```java
@Entity
public class GrammarType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String rule;        // 규칙 내용
    private String category;    // 규칙 카테고리
    private Boolean enabled;    // 활성화 여부
}
```

### 주요 서비스

#### MorphAnalyzeService
```java
@Service
public class MorphAnalyzeService {
    // KOMORAN 인스턴스 관리
    // 여러 모델 동시 실행 및 비교
    // 결과 diff 생성
}
```

#### UserModelService
```java
@Service
public class UserModelService {
    // 사용자 모델 빌드
    // 학습 데이터 검증
    // 모델 패키징 및 배포
}
```

### 유틸리티

#### StreamParser 계층
```java
public interface StreamParser {
    void parse(InputStream inputStream);
}

// 구현체들:
- DicWordStreamParser      // 기본 사전 파싱
- DicUserStreamParser      // 사용자 사전 파싱
- GrammarInStreamParser    // 문법 규칙 파싱
- FwdUserStreamParser      // FWD 사전 파싱
```

#### QueryParser 계층
```java
public interface QueryParser {
    Object parse(String query);
}

// 구현체들:
- FilterQueryParser        // 필터 쿼리 파싱
- SorterQueryParser        // 정렬 쿼리 파싱
```

#### ModelValidator
```java
public class ModelValidator {
    // 모델 파일 검증
    // 필수 파일 확인 (dic.word, grammar.in, transition, observation 등)
    // 파일 포맷 검증
}
```

### REST API 예시

#### 형태소 분석
```http
POST /analyze
Content-Type: application/json

{
  "sentence": "감기는 자주 걸리는 병이다",
  "modelType": "STABLE"
}
```

#### 사용자 사전 추가
```http
POST /dic/user
Content-Type: application/json

{
  "morph": "코모란",
  "pos": "NNP"
}
```

#### 모델 업로드
```http
POST /model/upload
Content-Type: multipart/form-data

file: model.zip
name: my-custom-model
```

---

## Elasticsearch Plugin 모듈 분석

Elasticsearch 7.6.2 및 Lucene 8.4.0과 연동되는 커스텀 토크나이저 플러그인입니다.

### 기술 스택
- **Elasticsearch**: 7.6.2
- **Lucene**: 8.4.0
- **의존성**: core 모듈, commons, aho-corasick

### 주요 클래스

#### 1. **KomoranPlugin.java**
**위치**: `elasticsearch-plugin/src/main/java/kr/co/shineware/nlp/elasticsearch/plugin/KomoranPlugin.java`

Elasticsearch 플러그인의 진입점:
```java
public class KomoranPlugin extends Plugin implements AnalysisPlugin {
    @Override
    public Map<String, AnalysisProvider<TokenizerFactory>> getTokenizers() {
        return singletonMap("komoran_tokenizer",
                           KomoranTokenizerFactory::new);
    }
}
```

#### 2. **KomoranTokenizerFactory.java**
**위치**: `elasticsearch-plugin/src/main/java/kr/co/shineware/nlp/elasticsearch/index/KomoranTokenizerFactory.java`

토크나이저 팩토리:
```java
public class KomoranTokenizerFactory extends AbstractTokenizerFactory {
    private final Komoran komoran;

    public KomoranTokenizerFactory(IndexSettings indexSettings,
                                   Environment env,
                                   String name,
                                   Settings settings) {
        super(indexSettings, settings, name);
        this.komoran = new Komoran(DEFAULT_MODEL.FULL);

        // 설정에서 사용자 사전 로드
        String userDicPath = settings.get("user_dic");
        if (userDicPath != null) {
            komoran.setUserDic(userDicPath);
        }
    }

    @Override
    public Tokenizer create() {
        return new KomoranTokenizer(komoran);
    }
}
```

#### 3. **KomoranTokenizer.java**
**위치**: `elasticsearch-plugin/src/main/java/kr/co/shineware/nlp/lucene/tokenizer/KomoranTokenizer.java`

Lucene 토크나이저 구현:
```java
public final class KomoranTokenizer extends Tokenizer {
    private final Komoran komoran;
    private final CharTermAttribute termAtt;
    private final OffsetAttribute offsetAtt;
    private final PositionIncrementAttribute posIncrAtt;
    private final TypeAttribute typeAtt;

    private List<Token> tokens;
    private int tokenIndex;

    @Override
    public boolean incrementToken() throws IOException {
        clearAttributes();

        if (tokenIndex >= tokens.size()) {
            return false;
        }

        Token token = tokens.get(tokenIndex++);
        termAtt.append(token.getMorph());
        offsetAtt.setOffset(token.getBeginIndex(), token.getEndIndex());
        posIncrAtt.setPositionIncrement(1);
        typeAtt.setType(token.getPos());

        return true;
    }
}
```

### Elasticsearch 설정 예시

#### 인덱스 설정
```json
{
  "settings": {
    "analysis": {
      "tokenizer": {
        "komoran_tokenizer": {
          "type": "komoran_tokenizer",
          "user_dic": "path/to/user_dic.txt"
        }
      },
      "analyzer": {
        "komoran_analyzer": {
          "type": "custom",
          "tokenizer": "komoran_tokenizer"
        }
      }
    }
  },
  "mappings": {
    "properties": {
      "content": {
        "type": "text",
        "analyzer": "komoran_analyzer"
      }
    }
  }
}
```

#### 플러그인 설치
```bash
# 플러그인 빌드
./gradlew :elasticsearch-plugin:makePlugin

# Elasticsearch에 설치
bin/elasticsearch-plugin install file:///path/to/komoran-tokenizer.zip

# Elasticsearch 재시작
bin/elasticsearch
```

### 분석 예시

**입력 문서**:
```json
{
  "content": "코모란은 한국어 형태소 분석기입니다"
}
```

**토큰화 결과**:
```
코모란/NNP
은/JX
한국어/NNG
형태소/NNG
분석기/NNG
이/VCP
ㅂ니다/EF
```

각 토큰은 다음 정보를 포함:
- **term**: 형태소
- **type**: 품사 태그
- **offset**: 원문에서의 시작/종료 위치
- **position**: 토큰 순서

---

## 빌드 및 의존성

### Gradle 빌드 구성

#### Root 프로젝트
```bash
# 전체 빌드
./gradlew build

# 특정 모듈 빌드
./gradlew :core:build
./gradlew :admin:build
./gradlew :elasticsearch-plugin:build

# Uber JAR 생성 (core)
./gradlew :core:uberJar

# Elasticsearch 플러그인 생성
./gradlew :elasticsearch-plugin:makePlugin

# 테스트 실행
./gradlew test

# 테스트 커버리지
./gradlew jacocoTestReport
```

### 의존성 관리

#### Core 모듈 의존성
```gradle
dependencies {
    implementation 'com.github.shineware:commons:1.0.1'
    implementation 'com.github.shineware:aho-corasick:1.1.0'
    testImplementation 'junit:junit:4.12'
}
```

**주요 라이브러리**:
- **shineware/commons**: 공통 유틸리티
- **shineware/aho-corasick**: Aho-Corasick 알고리즘 구현 (효율적인 다중 패턴 매칭)

#### Admin 모듈 의존성
```gradle
dependencies {
    implementation project(':core')
    implementation 'com.github.shineware:commons:1.0.1'
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'com.googlecode.json-simple:json-simple:1.1.1'
    implementation 'io.github.java-diff-utils:java-diff-utils:4.0'
    implementation 'net.lingala.zip4j:zip4j:2.1.3'
    runtimeOnly 'com.h2database:h2'
}
```

#### Elasticsearch Plugin 의존성
```gradle
dependencies {
    implementation project(':core')
    implementation 'com.github.shineware:commons:1.0.1'
    implementation 'com.github.shineware:aho-corasick:1.1.0'
    implementation 'org.apache.lucene:lucene-analyzers-common:8.4.0'
    implementation 'org.elasticsearch:elasticsearch:7.6.2'
}
```

### Maven/Gradle 사용법

#### Gradle
```gradle
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation 'com.github.shineware:KOMORAN:3.3.9'
}
```

#### Maven
```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependency>
    <groupId>com.github.shineware</groupId>
    <artifactId>KOMORAN</artifactId>
    <version>3.3.9</version>
</dependency>
```

---

## 핵심 알고리즘

### 1. HMM (Hidden Markov Model)

KOMORAN의 핵심은 통계적 HMM 기반 형태소 분석입니다.

#### 모델 구성
```
λ = (A, B, π)
```
- **A**: 전이 확률 행렬 P(tag_t | tag_{t-1})
- **B**: 관측 확률 행렬 P(morph | tag)
- **π**: 초기 상태 확률 P(tag_0)

#### 비터비 알고리즘 (Viterbi Algorithm)

최적 품사 태그 시퀀스를 찾는 동적 프로그래밍 알고리즘:

```
δ_t(j) = max_{1≤i≤N} [δ_{t-1}(i) × a_ij × b_j(o_t)]
```

여기서:
- `δ_t(j)`: 시간 t에서 상태 j까지의 최대 확률
- `a_ij`: 상태 i에서 j로의 전이 확률
- `b_j(o_t)`: 상태 j에서 관측치 o_t의 확률

**구현 위치**: `Lattice.findPath()`, `Lattice.findNBestPath()`

### 2. Aho-Corasick 알고리즘

효율적인 다중 패턴 매칭을 위한 오토마타 기반 알고리즘:

**시간 복잡도**: O(n + m + z)
- n: 텍스트 길이
- m: 패턴 총 길이
- z: 매칭 개수

**장점**:
- 여러 패턴을 동시에 검색
- 선형 시간 복잡도
- 메모리 효율적

**사용 위치**:
- `Observation` 클래스: 형태소 사전 검색
- `IrregularTrie` 클래스: 불규칙 활용 검색

### 3. 자소 단위 처리

한글 음절을 초성, 중성, 종성으로 분해하여 처리:

**유니코드 분해 공식**:
```
한글 음절 = 0xAC00 + (초성 × 588) + (중성 × 28) + 종성
```

**역변환**:
```java
int syllableIndex = char - 0xAC00;
int choIndex = syllableIndex / 588;
int jungIndex = (syllableIndex % 588) / 28;
int jongIndex = syllableIndex % 28;
```

**장점**:
- 사전 크기 감소 (예: "먹", "먹어", "먹었" 등을 "먹" + "어" + "었"으로 처리)
- 불규칙 활용 처리 용이
- 메모리 효율성 향상

### 4. 격자 그래프 (Lattice Graph)

모든 가능한 형태소 분할을 표현하는 방향성 비순환 그래프(DAG):

```
입력: "감기는"
자소: ㄱㅏㅁㄱㅣㄴㅡㄴ

격자 구조:
START → [감/NNG] → [기/NNG] → [는/JX] → END
  ↓       ↓           ↓
  └──→ [감기/NNG] ──→ [는/JX] ──────→ END
```

각 경로는 HMM 점수로 평가되어 최적 경로가 선택됩니다.

### 5. 결합 규칙 (Combination Rules)

한국어 문법 규칙을 적용하여 비문법적인 형태소 조합 제거:

**규칙 예시**:

1. **조사 선택 규칙**
```
받침 있음 + "이" (O) / + "가" (X)
받침 없음 + "가" (O) / + "이" (X)
```

2. **어미 결합 규칙**
```
동사 어간 + "-ㄴ" (X)
동사 어간 + "-는" (O)
형용사 어간 + "-ㄴ" (O)
```

3. **품사 전이 제약**
```
명사(NNG) + 동사(VV) (X)
명사(NNG) + 조사(JX) (O)
```

**구현**: `MergedCombinationRuleChecker.java`

---

## 사용 예시

### 기본 사용법

```java
import kr.co.shineware.nlp.komoran.core.Komoran;
import kr.co.shineware.nlp.komoran.constant.DEFAULT_MODEL;
import kr.co.shineware.nlp.komoran.model.KomoranResult;

// 1. KOMORAN 인스턴스 생성
Komoran komoran = new Komoran(DEFAULT_MODEL.FULL);

// 2. 문장 분석
String sentence = "감기는 자주 걸리는 병이다";
KomoranResult result = komoran.analyze(sentence);

// 3. 결과 출력
System.out.println(result.getPlainText());
// 출력: 감기/NNG 는/JX 자주/MAG 걸리/VV 는/ETM 병/NNG 이/VCP 다/EF

// 4. 명사만 추출
List<String> nouns = result.getNouns();
System.out.println(nouns);
// 출력: [감기, 병]
```

### 사용자 사전 활용

```java
// user_dic.txt 파일 내용:
// 코모란    NNP
// 샤인웨어  NNP

Komoran komoran = new Komoran(DEFAULT_MODEL.FULL);
komoran.setUserDic("user_dic.txt");

String sentence = "코모란은 샤인웨어에서 개발했습니다";
KomoranResult result = komoran.analyze(sentence);

System.out.println(result.getPlainText());
// 출력: 코모란/NNP 은/JX 샤인웨어/NNP 에서/JKB 개발/NNG 하/XSV 았/EP 습니다/EF
```

### N-best 결과 얻기

```java
Komoran komoran = new Komoran(DEFAULT_MODEL.FULL);
String sentence = "배가 고프다";

// 상위 3개 분석 결과 얻기
List<KomoranResult> results = komoran.analyze(sentence, 3);

for (int i = 0; i < results.size(); i++) {
    System.out.println("결과 " + (i+1) + ": " + results.get(i).getPlainText());
}

// 출력:
// 결과 1: 배/NNG 가/JKS 고프/VA 다/EF
// 결과 2: 배/NNG 가/JKS 고프/VA 다/EC
// 결과 3: 배/NNP 가/JKS 고프/VA 다/EF
```

### 토큰 정보 활용

```java
Komoran komoran = new Komoran(DEFAULT_MODEL.FULL);
String sentence = "자연어 처리는 어렵다";
KomoranResult result = komoran.analyze(sentence);

List<Token> tokens = result.getTokenList();
for (Token token : tokens) {
    System.out.printf("형태소: %s, 품사: %s, 시작: %d, 끝: %d%n",
                     token.getMorph(),
                     token.getPos(),
                     token.getBeginIndex(),
                     token.getEndIndex());
}

// 출력:
// 형태소: 자연어, 품사: NNG, 시작: 0, 끝: 3
// 형태소: 처리, 품사: NNG, 시작: 4, 끝: 6
// 형태소: 는, 품사: JX, 시작: 6, 끝: 7
// 형태소: 어렵, 품사: VA, 시작: 8, 끝: 10
// 형태소: 다, 품사: EF, 시작: 10, 끝: 11
```

### 멀티스레드 배치 처리

```java
Komoran komoran = new Komoran(DEFAULT_MODEL.FULL);

List<String> sentences = Arrays.asList(
    "첫 번째 문장입니다",
    "두 번째 문장입니다",
    "세 번째 문장입니다"
);

// 4개 스레드로 병렬 처리
List<KomoranResult> results = komoran.analyze(sentences, 4);

for (int i = 0; i < results.size(); i++) {
    System.out.println("문장 " + (i+1) + ": " + results.get(i).getPlainText());
}
```

### 특정 품사만 추출

```java
Komoran komoran = new Komoran(DEFAULT_MODEL.FULL);
String sentence = "아름다운 꽃이 피었습니다";
KomoranResult result = komoran.analyze(sentence);

// 명사와 동사만 추출
List<String> nounsAndVerbs = result.getMorphesByTags("NNG", "NNP", "VV");
System.out.println(nounsAndVerbs);
// 출력: [꽃, 피]

// 형용사만 추출
List<String> adjectives = result.getMorphesByTags("VA");
System.out.println(adjectives);
// 출력: [아름답]
```

### 파일 단위 처리

```java
Komoran komoran = new Komoran(DEFAULT_MODEL.FULL);

// input.txt 파일을 읽어 형태소 분석 후 output.txt에 저장
// 4개 스레드 사용
komoran.analyzeTextFile("input.txt", "output.txt", 4);
```

---

## 참고 자료

### 공식 문서 및 리소스

- **공식 문서**: https://docs.komoran.kr
- **공식 홈페이지**: https://www.shineware.co.kr/products/komoran
- **GitHub 저장소**: https://github.com/shineware/KOMORAN
- **데모 페이지**: https://www.shineware.co.kr/products/komoran/#demo
- **Slack 커뮤니티**: https://komoran.slack.com

### Python 래퍼

- **PyKOMORAN** (공식): https://github.com/shineware/PyKOMORAN
- **KOMORAN3Py** (커뮤니티): https://github.com/lovit/komoran3py

### 관련 논문

#### 국제 논문 (2019-2020)

1. **Kwon, S., Ko, Y., & Seo, J. (2019)**. "Effective vector representation for the Korean named-entity recognition." *Pattern Recognition Letters*, 117, 52-57.

2. **Ihm, S. Y., Lee, J. H., & Park, Y. H. (2019)**. "Skip-gram-KR: Korean Word Embedding for Semantic Clustering." *IEEE Access*, 7, 39948-39961.

3. **Heo, Y., Kang, S., & Yoo, D. (2019)**. "Multimodal neural machine translation with weakly labeled images." *IEEE Access*, 7, 54042-54053.

4. **Park, S., & Lee, M. (2020)**. "ARTAS: automatic research trend analysis system for information security." *Proceedings of the 35th Annual ACM Symposium on Applied Computing*.

### 국내 논문 (2019-2020)

1. **최병서, 이익훈, 이상구 (2020)**. "신조어 및 띄어쓰기 오류에 강인한 시퀀스-투-시퀀스 기반 한국어 형태소 분석기." *정보과학회논문지*, 47.1, 70-77.

2. **우윤희, 김현희 (2020)**. "국민청원 주제 분석 및 딥러닝 기반 답변 가능 청원 예측." *정보처리학회논문지*, 9.2, 45-52.

3. **우경진, 정수현 (2019)**. "문장 유형에 따른 한글 형태소 분석기 비교." *한국정보과학회 학술발표논문집*, 1388-1390.

### 품사 태그 세트

KOMORAN은 세종 품사 태그 세트를 사용합니다:

#### 주요 품사

| 태그 | 설명 | 예시 |
|------|------|------|
| **NNG** | 일반 명사 | 사람, 컴퓨터, 책 |
| **NNP** | 고유 명사 | 서울, 홍길동, 삼성 |
| **NNB** | 의존 명사 | 것, 수, 데 |
| **VV** | 동사 | 먹다, 가다, 보다 |
| **VA** | 형용사 | 크다, 작다, 아름답다 |
| **MAG** | 일반 부사 | 매우, 아주, 잘 |
| **JKS** | 주격 조사 | 이, 가 |
| **JKO** | 목적격 조사 | 을, 를 |
| **JX** | 보조사 | 는, 도, 만 |
| **EC** | 연결 어미 | 고, 며, 면 |
| **EF** | 종결 어미 | 다, 요, 니 |
| **ETM** | 관형형 전성 어미 | ㄴ, 는, ㄹ |
| **XSV** | 동사 파생 접미사 | 하, 되 |
| **SF** | 마침표, 물음표, 느낌표 | ., ?, ! |

전체 태그 목록: https://docs.komoran.kr/firststep/postypes.html

### CI/CD

KOMORAN은 GitHub Actions를 통한 지속적 통합을 수행합니다:

- **Java CI**: 푸시 및 PR 시 자동 빌드 및 테스트
- **Test Coverage**: Coveralls를 통한 코드 커버리지 추적
- **워크플로우 파일**: `.github/workflows/`

### 라이선스

KOMORAN은 **Apache License 2.0** 하에 배포됩니다.

주요 내용:
- 상업적 사용 가능
- 수정 및 배포 가능
- 특허권 부여
- 상표권은 부여되지 않음
- 책임 제한

전문: https://github.com/shineware/KOMORAN/blob/master/LICENSE

### 성능 벤치마크

다양한 한국어 형태소 분석기 비교:
- **속도**: 중간 수준 (순수 Java 구현)
- **정확도**: 높음 (HMM + 규칙 기반)
- **메모리**: 약 50MB (경량 모델) ~ 200MB (전체 모델)
- **특징**: 공백 포함 형태소 분석 가능

### 기여 방법

1. **이슈 제기**: GitHub Issues에 버그 리포트 또는 기능 제안
2. **Pull Request**: 코드 기여
3. **사전 기여**: 사용자 사전 및 말뭉치 기여
4. **문서화**: 문서 개선 및 번역

### FAQ

**Q: KOMORAN과 다른 형태소 분석기의 차이점은?**
- 순수 Java 구현으로 크로스 플랫폼 지원
- 외부 라이브러리 의존성 없음
- 공백 포함 형태소 분석 가능 (예: "서울 시", "삼성 전자")

**Q: 사용자 사전은 어떻게 추가하나요?**
```java
Komoran komoran = new Komoran(DEFAULT_MODEL.FULL);
komoran.setUserDic("user_dic.txt");
```
형식: `형태소\t품사` (탭으로 구분)

**Q: 모델은 어떻게 선택하나요?**
- `DEFAULT_MODEL.FULL`: 높은 정확도, 더 많은 메모리 사용
- `DEFAULT_MODEL.LIGHT`: 빠른 속도, 낮은 메모리 사용

**Q: 멀티스레드 처리는 어떻게 하나요?**
```java
List<KomoranResult> results = komoran.analyze(sentences, threadCount);
```

**Q: 불규칙 활용은 어떻게 처리되나요?**
- `IrregularTrie`에 불규칙 활용 패턴 저장
- 분석 시 자동으로 원형 복원
- 예: "먹었다" → "먹/VV 었/EP 다/EF"

---

## 결론

KOMORAN은 순수 Java로 구현된 강력하고 유연한 한국어 형태소 분석기입니다. HMM 기반의 통계적 접근과 규칙 기반 언어학적 지식을 결합하여 높은 정확도를 제공하며, 세 가지 모듈(Core, Admin, Elasticsearch Plugin)을 통해 다양한 환경에서 활용할 수 있습니다.

### 주요 강점

1. **순수 Java**: 플랫폼 독립적, 쉬운 통합
2. **경량**: 50MB 메모리에서도 동작
3. **확장 가능**: 사용자 사전 및 모델 커스터마이징
4. **다양한 출력**: 품사 태그, 명사 추출, 토큰 위치 정보 등
5. **생태계**: Elasticsearch, Python 래퍼 등 다양한 통합

### 적용 분야

- **검색 엔진**: 형태소 단위 인덱싱 및 검색
- **텍스트 마이닝**: 문서 분류, 감성 분석, 주제 모델링
- **자연어 처리**: 개체명 인식, 구문 분석 전처리
- **챗봇**: 의도 분류 및 개체 추출
- **정보 추출**: 핵심 키워드 및 명사구 추출

### 향후 방향

KOMORAN은 활발한 오픈소스 커뮤니티와 함께 지속적으로 발전하고 있으며, 학계와 산업계에서 널리 활용되고 있습니다. 논문 인용 및 실제 서비스 적용 사례가 지속적으로 증가하고 있습니다.

---

## 문서 정보

- **작성일**: 2025-11-11
- **KOMORAN 버전**: 3.0
- **분석 범위**: Core, Admin, Elasticsearch Plugin 모듈
- **문서 작성자**: AI 기반 코드 분석

---

**참고**: 본 문서는 KOMORAN 프로젝트의 소스 코드를 기반으로 작성되었습니다. 최신 정보는 [공식 문서](https://docs.komoran.kr)를 참고하시기 바랍니다.
