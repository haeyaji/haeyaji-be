package com.haeyaji.be.weather.domain;

/**
 * fe가 사용하는 대표 날씨 4분류. 기상청 SKY(하늘상태)/PTY(강수형태) 코드를 여기로 축약한다.
 *
 * <p>매핑 가이드
 * <ul>
 *   <li>눈(PTY 3)·눈날림(PTY 7) → SNOWY</li>
 *   <li>비/눈(PTY 2)·빗방울눈날림(PTY 6) → 기온 ≤ 1℃ 면 SNOWY, 아니면 RAINY</li>
 *   <li>비(1)·소나기(4)·빗방울(5) → RAINY</li>
 *   <li>맑음(SKY 1) → SUNNY / 구름많음(SKY 3)·흐림(SKY 4) → CLOUDY</li>
 * </ul>
 */
public enum WeatherCondition {

    SUNNY("sunny"),
    CLOUDY("cloudy"),
    RAINY("rainy"),
    SNOWY("snowy");

    /** 비/눈 혼재 시 눈으로 판정하는 기온 상한(℃). */
    private static final int SLEET_SNOW_MAX_TEMP = 1;

    private final String code;

    WeatherCondition(String code) {
        this.code = code;
    }

    /** fe 계약용 소문자 코드 (sunny/cloudy/rainy/snowy). */
    public String code() {
        return code;
    }

    /**
     * 기상청 SKY / PTY 코드를 대표 상태로 변환한다.
     *
     * @param sky   하늘상태 코드 (1 맑음, 3 구름많음, 4 흐림)
     * @param pty   강수형태 코드 (0 없음, 1 비, 2 비/눈, 3 눈, 4 소나기, 5 빗방울, 6 빗방울눈날림, 7 눈날림)
     * @param tempC 기온(℃) — 비/눈 혼재(PTY 2·6) 판정에 사용
     */
    public static WeatherCondition resolve(int sky, int pty, int tempC) {
        return switch (pty) {
            case 3, 7 -> SNOWY;
            case 2, 6 -> tempC <= SLEET_SNOW_MAX_TEMP ? SNOWY : RAINY;
            case 1, 4, 5 -> RAINY;
            default -> sky == 1 ? SUNNY : CLOUDY;
        };
    }

    /**
     * 중기육상예보(getMidLandFcst)의 하늘상태 텍스트(wf)를 대표 상태로 변환한다.
     * 예: "맑음", "구름많음", "흐림", "흐리고 비", "구름많고 눈", "흐리고 비/눈".
     * 비/눈 혼재 텍스트는 눈 우선(겨울철 안전측) 판정.
     */
    public static WeatherCondition fromMidLandText(String wf) {
        if (wf == null) {
            return CLOUDY;
        }
        if (wf.contains("눈")) {
            return SNOWY;
        }
        if (wf.contains("비") || wf.contains("소나기") || wf.contains("뇌우")) {
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
            case 5 -> "빗방울";
            case 6 -> "빗방울/눈날림";
            case 7 -> "눈날림";
            default -> switch (sky) {
                case 1 -> "맑음";
                case 3 -> "구름많음";
                case 4 -> "흐림";
                default -> "정보 없음";
            };
        };
    }
}
