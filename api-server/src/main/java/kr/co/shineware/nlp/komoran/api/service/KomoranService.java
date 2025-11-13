package kr.co.shineware.nlp.komoran.api.service;

import kr.co.shineware.nlp.komoran.constant.DEFAULT_MODEL;
import kr.co.shineware.nlp.komoran.core.Komoran;
import kr.co.shineware.nlp.komoran.model.KomoranResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * KOMORAN 형태소 분석 서비스
 * - 사전 파일 기반 로딩
 * - 주기적 리로딩 지원
 */
@Service
public class KomoranService {

    private static final Logger logger = LoggerFactory.getLogger(KomoranService.class);

    @Value("${komoran.model:FULL}")
    private String modelType;

    @Value("${komoran.user-dic-path:}")
    private String userDicPath;

    @Value("${komoran.fwd-dic-path:}")
    private String fwdDicPath;

    @Value("${komoran.partial-fwd-dic-path:}")
    private String partialFwdDicPath;

    @Value("${komoran.auto-reload:false}")
    private boolean autoReload;

    private Komoran komoran;
    private Map<String, Long> fileLastModifiedMap = new HashMap<>();

    @PostConstruct
    public void init() {
        logger.info("Initializing KOMORAN service...");
        loadKomoran();
        logger.info("KOMORAN service initialized successfully");
    }

    /**
     * KOMORAN 인스턴스 로드 및 사전 설정
     */
    private synchronized void loadKomoran() {
        try {
            // KOMORAN 인스턴스 생성
            // LIGHT → STABLE (models_light), FULL → EXPERIMENT (models_full)
            DEFAULT_MODEL model = "LIGHT".equalsIgnoreCase(modelType)
                    ? DEFAULT_MODEL.STABLE
                    : DEFAULT_MODEL.EXPERIMENT;

            logger.info("Loading KOMORAN with model: {}", model);
            this.komoran = new Komoran(model);

            // 사용자 사전 로드
            if (userDicPath != null && !userDicPath.trim().isEmpty()) {
                File userDicFile = new File(userDicPath);
                if (userDicFile.exists()) {
                    logger.info("Loading user dictionary: {}", userDicPath);
                    komoran.setUserDic(userDicPath);
                    fileLastModifiedMap.put(userDicPath, userDicFile.lastModified());
                } else {
                    logger.warn("User dictionary file not found: {}", userDicPath);
                }
            }

            // FWD 사전 로드
            if (fwdDicPath != null && !fwdDicPath.trim().isEmpty()) {
                File fwdDicFile = new File(fwdDicPath);
                if (fwdDicFile.exists()) {
                    logger.info("Loading FWD dictionary: {}", fwdDicPath);
                    komoran.setFWDic(fwdDicPath);
                    fileLastModifiedMap.put(fwdDicPath, fwdDicFile.lastModified());
                } else {
                    logger.warn("FWD dictionary file not found: {}", fwdDicPath);
                }
            }

            // Partial FWD 사전 로드
            if (partialFwdDicPath != null && !partialFwdDicPath.trim().isEmpty()) {
                File partialFwdDicFile = new File(partialFwdDicPath);
                if (partialFwdDicFile.exists()) {
                    logger.info("Loading Partial FWD dictionary: {}", partialFwdDicPath);
                    komoran.setPartialFWDic(partialFwdDicPath);
                    fileLastModifiedMap.put(partialFwdDicPath, partialFwdDicFile.lastModified());
                } else {
                    logger.warn("Partial FWD dictionary file not found: {}", partialFwdDicPath);
                }
            }

            logger.info("KOMORAN loaded successfully with {} dictionaries", fileLastModifiedMap.size());

        } catch (Exception e) {
            logger.error("Failed to load KOMORAN", e);
            throw new RuntimeException("Failed to initialize KOMORAN service", e);
        }
    }

    /**
     * 형태소 분석 수행
     */
    public KomoranResult analyze(String text) {
        if (komoran == null) {
            throw new IllegalStateException("KOMORAN is not initialized");
        }
        return komoran.analyze(text);
    }

    /**
     * 사전 파일 수동 리로드
     */
    public synchronized void reloadDictionaries() {
        logger.info("Manual dictionary reload requested");
        loadKomoran();
        logger.info("Dictionary reload completed");
    }

    /**
     * 파일 변경 감지 및 자동 리로드 (5분마다)
     */
    @Scheduled(fixedDelay = 300000) // 5분 = 300,000ms
    public void autoReloadIfNeeded() {
        if (!autoReload) {
            return;
        }

        logger.debug("Checking for dictionary file changes...");

        boolean needReload = false;
        for (Map.Entry<String, Long> entry : fileLastModifiedMap.entrySet()) {
            String filePath = entry.getKey();
            Long lastModified = entry.getValue();

            File file = new File(filePath);
            if (file.exists() && file.lastModified() > lastModified) {
                logger.info("Dictionary file changed: {}", filePath);
                needReload = true;
                break;
            }
        }

        if (needReload) {
            logger.info("Auto-reloading dictionaries due to file changes");
            loadKomoran();
        }
    }

    /**
     * 현재 로드된 사전 정보 조회
     */
    public Map<String, Object> getDictionaryInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("modelType", modelType);
        info.put("userDicPath", userDicPath);
        info.put("fwdDicPath", fwdDicPath);
        info.put("partialFwdDicPath", partialFwdDicPath);
        info.put("autoReload", autoReload);
        info.put("loadedDictionaries", fileLastModifiedMap.size());

        Map<String, String> fileStatus = new HashMap<>();
        for (String path : fileLastModifiedMap.keySet()) {
            File file = new File(path);
            fileStatus.put(path, file.exists() ? "OK" : "MISSING");
        }
        info.put("fileStatus", fileStatus);

        return info;
    }
}
