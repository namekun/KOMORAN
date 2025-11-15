# 부분 기분석(Partial FWD) 기능

## 개요

KOMORAN에 새로 추가된 **부분 기분석(Partial Forward Analysis)** 기능은 기존 FWD 사전의 제약사항을 극복한 향상된 기분석 기능입니다.

### 기존 FWD 사전의 한계

기존 FWD 사전은 **어절이 100% 일치**해야만 작동합니다:

```java
// fwd.user
나이키 운동화	나이키/NNP 운동화/NNG

// 입력
"나이키 운동화"      → ✅ FWD 적용됨
"나이키 운동화를"    → ❌ FWD 적용 안됨 (조사 '를'이 붙어서 어절이 달라짐)
"나이키 운동화가"    → ❌ FWD 적용 안됨
"나이키 운동화는"    → ❌ FWD 적용 안됨
```

### 부분 기분석의 장점

부분 기분석은 **부분 문자열 매칭**을 지원합니다:

```java
// partial_fwd.user
나이키 운동화	나이키/NNP 운동화/NNG

// 입력
"나이키 운동화"      → ✅ 적용됨
"나이키 운동화를"    → ✅ 적용됨 (부분 매칭!)
"나이키 운동화가"    → ✅ 적용됨
"나이키 운동화는"    → ✅ 적용됨
```

---

## 구현 원리

### 핵심 기술

1. **Aho-Corasick 알고리즘**: 다중 패턴 매칭을 O(n + m + z) 시간에 수행
2. **자소 단위 처리**: 한글을 초성, 중성, 종성으로 분해하여 매칭
3. **여러 형태소 분할**: 미리 분석된 여러 형태소를 lattice에 순차적으로 삽입

### 데이터 구조

```java
// 기존 FWD
private HashMap<String, List<Pair<String, String>>> fwd;
// → 정확한 키 매칭만 가능

// 부분 기분석
private AhoCorasickDictionary<List<Pair<String, String>>> partialFwd;
// → 부분 문자열 매칭 가능
```

### 분석 파이프라인

```
입력 문장
  ↓
자소 단위 변환
  ↓
각 자소 위치에서:
  1. FWD 조회 (어절 단위 완전 일치)
  2. 사용자 사전 (부분 매칭, 단일 형태소)
  3. 부분 기분석 (부분 매칭, 여러 형태소) ← NEW!
  4. 일반 사전
  5. 불규칙 활용
  ↓
Lattice 그래프 구축
  ↓
Viterbi 알고리즘으로 최적 경로 탐색
  ↓
결과 출력
```

---

## 사용 방법

### 1. 부분 기분석 사전 파일 생성

**파일 형식**: `[분석대상]\t[형태소1/품사1] [형태소2/품사2] ...`

**파일 예시** (`core/user_data/partial_fwd.user`):

```
# 상표명/제품명
나이키 운동화	나이키/NNP 운동화/NNG
삼성 갤럭시	삼성/NNP 갤럭시/NNP
애플 아이폰	애플/NNP 아이폰/NNP

# 복합 명사
인공 지능	인공/NNG 지능/NNG
기계 학습	기계/NNG 학습/NNG
자연어 처리	자연어/NNG 처리/NNG

# 고유 명사
서울 특별시	서울/NNP 특별시/NNG
대한민국 정부	대한민국/NNP 정부/NNG

# 복합 표현
밀리언 달러 베이비	밀리언/NNP 달러/NNP 베이비/NNP
바람과 함께 사라지다	바람/NNG 과/JC 함께/MAG 사라지/VV 다/EF
```

### 2. Java 코드에서 사용

```java
import kr.co.shineware.nlp.komoran.core.Komoran;
import kr.co.shineware.nlp.komoran.constant.DEFAULT_MODEL;
import kr.co.shineware.nlp.komoran.model.KomoranResult;

public class PartialFWDExample {
    public static void main(String[] args) {
        // KOMORAN 인스턴스 생성
        Komoran komoran = new Komoran(DEFAULT_MODEL.FULL);

        // 부분 기분석 사전 로드
        komoran.setPartialFWDic("user_data/partial_fwd.user");

        // 분석
        String sentence = "나이키 운동화를 샀어";
        KomoranResult result = komoran.analyze(sentence);

        System.out.println(result.getPlainText());
        // 출력: 나이키/NNP 운동화/NNG 를/JKO 사/VV 았/EP 어/EC
    }
}
```

---

## 사용 예시

### 예시 1: 조사가 붙은 경우

```java
komoran.setPartialFWDic("user_data/partial_fwd.user");

// partial_fwd.user:
// 나이키 운동화	나이키/NNP 운동화/NNG

String[] inputs = {
    "나이키 운동화",          // "나이키 운동화"가 그대로 매칭
    "나이키 운동화를",        // "나이키 운동화" 부분 매칭 후 "를" 별도 분석
    "나이키 운동화가",        // "나이키 운동화" 부분 매칭 후 "가" 별도 분석
    "나이키 운동화는",        // "나이키 운동화" 부분 매칭 후 "는" 별도 분석
};

// 모든 입력에서 "나이키 운동화"가 "나이키/NNP 운동화/NNG"로 분석됨!
```

**결과**:
```
입력: 나이키 운동화
결과: 나이키/NNP 운동화/NNG

입력: 나이키 운동화를
결과: 나이키/NNP 운동화/NNG 를/JKO

입력: 나이키 운동화가
결과: 나이키/NNP 운동화/NNG 가/JKS

입력: 나이키 운동화는
결과: 나이키/NNP 운동화/NNG 는/JX
```

### 예시 2: 복합 표현

```java
// partial_fwd.user:
// 밀리언 달러 베이비	밀리언/NNP 달러/NNP 베이비/NNP

String sentence = "밀리언 달러 베이비랑 뭐가 더 재밌었어?";
KomoranResult result = komoran.analyze(sentence);

System.out.println(result.getPlainText());
// 출력: 밀리언/NNP 달러/NNP 베이비/NNP 랑/JC 뭐/NP 가/JKS 더/MAG 재밌/VA 었/EP 어/EC
```

### 예시 3: FWD vs 부분 기분석 비교

```java
String testSentence = "나이키 운동화를 샀어";

// 1. 기존 FWD 사전
Komoran komoranFWD = new Komoran(DEFAULT_MODEL.FULL);
komoranFWD.setFWDic("user_data/fwd.user");
KomoranResult fwdResult = komoranFWD.analyze(testSentence);
System.out.println("FWD: " + fwdResult.getPlainText());
// → "나이키 운동화를"이 어절로 등록되어 있지 않으면 적용 안됨

// 2. 부분 기분석 사전
Komoran komoranPartialFWD = new Komoran(DEFAULT_MODEL.FULL);
komoranPartialFWD.setPartialFWDic("user_data/partial_fwd.user");
KomoranResult partialResult = komoranPartialFWD.analyze(testSentence);
System.out.println("Partial FWD: " + partialResult.getPlainText());
// → "나이키 운동화"가 부분 매칭되어 기분석 결과 적용!
```

---

## 비교표

| 구분 | FWD | 사용자 사전 | 부분 기분석 (NEW!) |
|------|-----|------------|-------------------|
| **매칭 방식** | 어절 완전 일치 | 부분 문자열 매칭 | 부분 문자열 매칭 |
| **출력** | 여러 형태소 가능 ✅ | 하나의 형태소만 ❌ | 여러 형태소 가능 ✅ |
| **자료구조** | HashMap | Aho-Corasick Trie | Aho-Corasick Trie |
| **적용 단위** | 어절 단위 | 자소 단위 | 자소 단위 |
| **우선순위** | 1순위 (최우선) | 2순위 | 3순위 |
| **메서드** | `setFWDic()` | `setUserDic()` | `setPartialFWDic()` |
| **조사 대응** | ❌ 불가능 | ✅ 가능 | ✅ 가능 |
| **복합어 분할** | ✅ 가능 | ❌ 불가능 | ✅ 가능 |

---

## 활용 시나리오

### 1. 상표명/제품명 처리

```
# 문제: 상표명 뒤에 다양한 조사가 붙음
"삼성 갤럭시를", "삼성 갤럭시가", "삼성 갤럭시는" ...

# 해결: 부분 기분석 사전에 등록
삼성 갤럭시	삼성/NNP 갤럭시/NNP

# 결과: 조사가 달라도 항상 정확하게 분석됨
```

### 2. 전문 용어 처리

```
# AI/ML 용어
인공 지능	인공/NNG 지능/NNG
기계 학습	기계/NNG 학습/NNG
딥 러닝	딥/NNP 러닝/NNP

# 의료 용어
관상 동맥	관상/NNG 동맥/NNG
급성 심근경색	급성/NNG 심근경색/NNG

# 법률 용어
민사 소송	민사/NNG 소송/NNG
형사 재판	형사/NNG 재판/NNG
```

### 3. 고유 명사 처리

```
# 지명
서울 특별시	서울/NNP 특별시/NNG
부산 광역시	부산/NNP 광역시/NNG

# 기관명
국립 중앙 박물관	국립/NNG 중앙/NNG 박물관/NNG
대한민국 정부	대한민국/NNP 정부/NNG
```

### 4. 오분석 교정

```
# 일반 분석 시 잘못 분석되는 복합어 강제 분석
밀리언 달러 베이비	밀리언/NNP 달러/NNP 베이비/NNP
바람과 함께 사라지다	바람/NNG 과/JC 함께/MAG 사라지/VV 다/EF
```

---

## 성능 특징

### 시간 복잡도
- **Aho-Corasick 알고리즘**: O(n + m + z)
  - n: 입력 텍스트 길이
  - m: 패턴 총 길이
  - z: 매칭 개수
- **매우 효율적**: 수천 개의 패턴도 빠르게 매칭

### 메모리 사용
- Trie 구조로 공통 접두사 공유
- 패턴 수에 비례하여 메모리 사용
- 일반적으로 수 MB ~ 수십 MB 수준

---

## 주의사항

### 1. 품사 태그 규칙 준수

```
# ❌ 잘못된 예: 문법 규칙에 맞지 않음
감기는	감/NNG 기/ETM 는/JKG
# NNG 다음에 ETM이 오는 경우가 grammar.in에 없음

# ✅ 올바른 예
감기는	감/NNG 기는/NNG
```

### 2. 파일 인코딩

- **반드시 UTF-8** 인코딩 사용
- 다른 인코딩 사용 시 한글이 깨짐

### 3. 구분자

- 분석대상과 분석결과 사이: **TAB(\t)** 문자
- 형태소 사이: **공백(space)** 문자

### 4. 우선순위 이해

```
우선순위 순서:
1. FWD (어절 완전 일치)
2. 사용자 사전 (부분 매칭, 단일 형태소)
3. 부분 기분석 (부분 매칭, 여러 형태소) ← NEW
4. 일반 사전
5. 불규칙 활용
```

---

## 테스트

테스트 코드: `core/src/test/java/kr/co/shineware/nlp/komoran/core/PartialFWDTest.java`

```bash
# 테스트 실행
./gradlew :core:test --tests PartialFWDTest
```

---

## 구현 상세

### 추가된 코드

#### 1. 필드 추가 (Komoran.java)
```java
private AhoCorasickDictionary<List<Pair<String, String>>> partialFwd;
private FindContext partialFwdFindContext;
```

#### 2. 메서드 추가
- `setPartialFWDic(String filename)`: 부분 기분석 사전 로드
- `partialFwdParsing(Lattice lattice, char jaso, int curIndex)`: 부분 매칭 및 lattice 삽입

#### 3. 분석 파이프라인 수정
```java
this.userDicParsing(lattice, jaso, curIndex);
this.partialFwdParsing(lattice, jaso, curIndex);  // ← 추가
this.regularParsing(lattice, jaso, curIndex);
```

---

## 결론

부분 기분석 기능은 기존 FWD 사전의 제약사항을 극복하고, 보다 유연하고 강력한 기분석 기능을 제공합니다. 특히 **조사가 붙은 복합어**, **전문 용어**, **상표명** 등을 정확하게 분석할 수 있어, 실무 활용도가 매우 높습니다.

### 주요 장점 요약

✅ **부분 문자열 매칭**: 조사가 붙어도 작동
✅ **여러 형태소 분할**: 복합어를 정확하게 분석
✅ **효율적인 성능**: Aho-Corasick 알고리즘
✅ **사용 편의성**: 기존 FWD와 동일한 파일 형식
✅ **높은 확장성**: 수천 개의 패턴 등록 가능

---

**문의 및 기여**: https://github.com/shineware/KOMORAN
