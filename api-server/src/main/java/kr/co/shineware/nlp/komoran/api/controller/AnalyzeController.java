package kr.co.shineware.nlp.komoran.api.controller;

import kr.co.shineware.nlp.komoran.api.service.KomoranService;
import kr.co.shineware.nlp.komoran.model.KomoranResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 형태소 분석 API 컨트롤러
 */
@RestController
@RequestMapping("/api")
public class AnalyzeController {

    private static final Logger logger = LoggerFactory.getLogger(AnalyzeController.class);

    @Autowired
    private KomoranService komoranService;

    /**
     * 형태소 분석 API
     *
     * POST /api/analyze
     * Body: { "text": "분석할 문장" }
     */
    @PostMapping("/analyze")
    public ResponseEntity<Map<String, Object>> analyze(@RequestBody Map<String, String> request) {
        try {
            String text = request.get("text");

            if (text == null || text.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(createErrorResponse("Text is required"));
            }

            logger.debug("Analyzing text: {}", text);

            KomoranResult result = komoranService.analyze(text);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("text", text);
            response.put("plainText", result.getPlainText());
            response.put("list", result.getList());
            response.put("tokens", result.getTokenList());
            response.put("nouns", result.getNouns());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error analyzing text", e);
            return ResponseEntity.internalServerError()
                    .body(createErrorResponse("Analysis failed: " + e.getMessage()));
        }
    }

    /**
     * 명사만 추출하는 API
     *
     * POST /api/nouns
     * Body: { "text": "분석할 문장" }
     */
    @PostMapping("/nouns")
    public ResponseEntity<Map<String, Object>> extractNouns(@RequestBody Map<String, String> request) {
        try {
            String text = request.get("text");

            if (text == null || text.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(createErrorResponse("Text is required"));
            }

            KomoranResult result = komoranService.analyze(text);
            List<String> nouns = result.getNouns();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("text", text);
            response.put("nouns", nouns);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error extracting nouns", e);
            return ResponseEntity.internalServerError()
                    .body(createErrorResponse("Noun extraction failed: " + e.getMessage()));
        }
    }

    /**
     * 사전 수동 리로드 API
     *
     * POST /api/reload
     */
    @PostMapping("/reload")
    public ResponseEntity<Map<String, Object>> reloadDictionaries() {
        try {
            logger.info("Dictionary reload requested via API");
            komoranService.reloadDictionaries();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Dictionaries reloaded successfully");
            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error reloading dictionaries", e);
            return ResponseEntity.internalServerError()
                    .body(createErrorResponse("Reload failed: " + e.getMessage()));
        }
    }

    /**
     * 사전 정보 조회 API
     *
     * GET /api/info
     */
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getDictionaryInfo() {
        try {
            Map<String, Object> info = komoranService.getDictionaryInfo();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("info", info);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error getting dictionary info", e);
            return ResponseEntity.internalServerError()
                    .body(createErrorResponse("Failed to get info: " + e.getMessage()));
        }
    }

    /**
     * 헬스체크 API
     *
     * GET /api/health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "KOMORAN API");
        response.put("timestamp", System.currentTimeMillis());

        return ResponseEntity.ok(response);
    }

    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("success", false);
        error.put("error", message);
        error.put("timestamp", System.currentTimeMillis());
        return error;
    }
}
