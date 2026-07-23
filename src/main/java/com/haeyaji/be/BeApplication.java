package com.haeyaji.be;

import java.util.TimeZone;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BeApplication {

    public static void main(String[] args) {
        // 기상청 발표시각 계산 등이 KST를 전제하므로 컨테이너 TZ(UTC 등)와 무관하게 고정.
        // systemDefaultZone()/LocalDateTime.now()가 전부 Asia/Seoul 기준이 된다.
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
        SpringApplication.run(BeApplication.class, args);
    }
}
