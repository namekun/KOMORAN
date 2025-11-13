package kr.co.shineware.nlp.komoran.core;

import kr.co.shineware.nlp.komoran.constant.DEFAULT_MODEL;
import kr.co.shineware.nlp.komoran.model.KomoranResult;
import org.junit.Before;
import org.junit.Test;

/**
 * 부분 기분석 사전 기능 테스트
 */
public class PartialFWDTest {

    private Komoran komoran;

    @Before
    public void init() {
        this.komoran = new Komoran(DEFAULT_MODEL.FULL);
    }

    @Test
    public void testWithoutPartialFWD() {
        System.out.println("=== 부분 기분석 사전 없이 분석 ===");

        String[] testSentences = {
            "나이키 운동화를 샀어",
            "삼성 갤럭시가 좋아",
            "인공 지능 기술이 발전했다",
            "밀리언 달러 베이비랑 바람과 함께 사라지다랑 뭐가 더 재밌었어?"
        };

        for (String sentence : testSentences) {
            KomoranResult result = this.komoran.analyze(sentence);
            System.out.println("입력: " + sentence);
            System.out.println("결과: " + result.getPlainText());
            System.out.println();
        }
    }

    @Test
    public void testWithPartialFWD() {
        System.out.println("=== 부분 기분석 사전 적용 후 분석 ===");

        // 부분 기분석 사전 로드
        this.komoran.setPartialFWDic("user_data/partial_fwd.user");

        String[] testSentences = {
            "나이키 운동화를 샀어",
            "나이키 운동화 좋아",
            "삼성 갤럭시가 좋아",
            "삼성 갤럭시 사고 싶어",
            "인공 지능 기술이 발전했다",
            "인공 지능은 미래다",
            "밀리언 달러 베이비랑 바람과 함께 사라지다랑 뭐가 더 재밌었어?"
        };

        for (String sentence : testSentences) {
            KomoranResult result = this.komoran.analyze(sentence);
            System.out.println("입력: " + sentence);
            System.out.println("결과: " + result.getPlainText());
            System.out.println();
        }
    }

    @Test
    public void testPartialMatching() {
        System.out.println("=== 부분 매칭 테스트 (조사 붙은 경우) ===");

        this.komoran.setPartialFWDic("user_data/partial_fwd.user");

        // "나이키 운동화"가 조사와 함께 나타나는 다양한 경우
        String[] testCases = {
            "나이키 운동화",          // 어절 그대로
            "나이키 운동화가",        // 주격 조사
            "나이키 운동화를",        // 목적격 조사
            "나이키 운동화는",        // 보조사
            "나이키 운동화에",        // 부사격 조사
            "나이키 운동화로",        // 부사격 조사
            "나이키 운동화의",        // 관형격 조사
            "나이키 운동화랑",        // 접속 조사
        };

        for (String sentence : testCases) {
            KomoranResult result = this.komoran.analyze(sentence);
            System.out.println("입력: " + sentence);
            System.out.println("결과: " + result.getPlainText());
            System.out.println();
        }
    }

    @Test
    public void compareWithFWD() {
        System.out.println("=== FWD vs 부분 기분석 비교 ===");

        String testSentence = "나이키 운동화를 샀어";

        // 1. FWD 사전 사용
        System.out.println("1. FWD 사전 (어절 완전 일치 필요):");
        Komoran komoranFWD = new Komoran(DEFAULT_MODEL.FULL);
        komoranFWD.setFWDic("user_data/fwd.user");
        KomoranResult fwdResult = komoranFWD.analyze(testSentence);
        System.out.println("   입력: " + testSentence);
        System.out.println("   결과: " + fwdResult.getPlainText());
        System.out.println("   → 조사 '를'이 붙어서 어절이 달라져 FWD 적용 안됨");
        System.out.println();

        // 2. 부분 기분석 사전 사용
        System.out.println("2. 부분 기분석 사전 (부분 문자열 매칭):");
        Komoran komoranPartialFWD = new Komoran(DEFAULT_MODEL.FULL);
        komoranPartialFWD.setPartialFWDic("user_data/partial_fwd.user");
        KomoranResult partialFwdResult = komoranPartialFWD.analyze(testSentence);
        System.out.println("   입력: " + testSentence);
        System.out.println("   결과: " + partialFwdResult.getPlainText());
        System.out.println("   → '나이키 운동화'가 부분 매칭되어 기분석 결과 적용!");
        System.out.println();
    }
}
