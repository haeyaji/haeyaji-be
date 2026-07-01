package com.haeyaji.be.weather.domain;

/**
 * fe가 사용하는 대표 날씨 3분류. 기상청 SKY(하늘상태)/PTY(강수형태) 코드를 여기로 축약한다.
 *
 * <p>매핑 가이드
 * <ul>
 *   <li>강수 있음(PTY != 0) → RAINY</li>
 *   <li>맑음(SKY 1) → SUNNY</li>
 *   <li>구름많음(SKY 3)·흐림(SKY 4) → CLOUDY</li>
 * </ul>
 */
public enum WeatherCondition {

    SUNNY("sunny"),
    CLOUDY("cloudy"),
    RAINY("rainy");

    private final String code;

    WeatherCondition(String code) {
        this.code = code;
    }

    /** fe 계약용 소문자 코드 (sunny/cloudy/rainy). */
    public String code() {
        return code;
    }

    /**
     * 기상청 SKY / PTY 코드를 대표 상태로 변환한다.
     *
     * @param sky 하늘상태 코드 (1 맑음, 3 구름많음, 4 흐림)
     * @param pty 강수형태 코드 (0 없음, 1 비, 2 비/눈, 3 눈, 4 소나기)
     */
    public static WeatherCondition resolve(int sky, int pty) {
        if (pty != 0) {
            return RAINY;
        }
        return sky == 1 ? SUNNY : CLOUDY;
    }

    /**
     * 중기육상예보(getMidLandFcst)의 하늘상태 텍스트(wf)를 대표 상태로 변환한다.
     * 예: "맑음", "구름많음", "흐림", "흐리고 비", "구름많고 비/눈".
     */
    public static WeatherCondition fromMidLandText(String wf) {
        if (wf == null) {
            return CLOUDY;
        }
        if (wf.contains("비") || wf.contains("눈") || wf.contains("소나기") || wf.contains("뇌우")) {
            return RAINY;
        }
        if (wf.contains("흐림") || wf.contains("구름")) {
            return CLOUDY;
        }
        if (wf.contains("맑음")) {
            return SUNNY;
        }
        return CLOUDY;
    }

    /** 사용자 표시 문구(condKo). */
    public static String describeKo(int sky, int pty) {
        return switch (pty) {
            case 1 -> "비";
            case 2 -> "비/눈";
            case 3 -> "눈";
            case 4 -> "소나기";
            default -> switch (sky) {
                case 1 -> "맑음";
                case 3 -> "구름많음";
                case 4 -> "흐림";
                default -> "정보 없음";
            };
        };
    }
}
