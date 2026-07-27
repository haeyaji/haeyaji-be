package com.haeyaji.be.notification.mail;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 메일 본문에 넣을 앱 링크. 로그인 콜백 URL과 <b>같은 오리진</b>을 쓴다 —
 * 메일만 다른 주소를 가리키면 로컬·배포 환경이 갈릴 때 조용히 엇나간다.
 */
@Component
public class MailLinks {

    private final String origin;

    public MailLinks(@Value("${app.frontend.callback-url}") String frontendCallbackUrl) {
        int pathStart = frontendCallbackUrl.indexOf('/', frontendCallbackUrl.indexOf("//") + 2);
        this.origin = pathStart > 0 ? frontendCallbackUrl.substring(0, pathStart) : frontendCallbackUrl;
    }

    /** 약속 화면 (fe 라우팅: {@code /meetup/{shareToken}}). */
    public String meeting(String shareToken) {
        return origin + "/meetup/" + shareToken;
    }

    /**
     * 앱 첫 화면. 할 일에는 개별 딥링크 경로가 없어(fe가 {@code /meetup/*}만 해석) 루트로 보낸다 —
     * 없는 경로로 보내면 새로고침 시 빈 화면이 뜬다.
     */
    public String app() {
        return origin + "/";
    }
}
