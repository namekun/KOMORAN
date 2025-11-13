package kr.co.shineware.nlp.komoran.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * KOMORAN API 서버 메인 애플리케이션
 */
@SpringBootApplication
@EnableScheduling
public class KomoranApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(KomoranApiApplication.class, args);
    }
}
