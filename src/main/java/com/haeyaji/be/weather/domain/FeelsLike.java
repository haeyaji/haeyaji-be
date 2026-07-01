package com.haeyaji.be.weather.domain;

/**
 * 체감온도 계산. 기상청은 체감온도를 직접 제공하지 않으므로 기온·습도·풍속으로 파생한다.
 *
 * <ul>
 *   <li>여름철(기온 ≥ 27℃): 기상청 2016 체감온도(습구온도 기반) 공식</li>
 *   <li>겨울철(기온 ≤ 10℃, 풍속 ≥ 1.3 m/s): 기상청 겨울철 체감온도(wind chill) 공식</li>
 *   <li>그 외: 기온 그대로</li>
 * </ul>
 */
public final class FeelsLike {

    private FeelsLike() {
    }

    public static int calculate(double tempC, int humidityPct, double windMs) {
        if (tempC >= 27.0) {
            return Math.round((float) summer(tempC, humidityPct));
        }
        if (tempC <= 10.0 && windMs >= 1.3) {
            return Math.round((float) winter(tempC, windMs));
        }
        return Math.round((float) tempC);
    }

    private static double summer(double t, double rh) {
        double tw = t * Math.atan(0.151977 * Math.sqrt(rh + 8.313659))
                + Math.atan(t + rh)
                - Math.atan(rh - 1.67633)
                + 0.00391838 * Math.pow(rh, 1.5) * Math.atan(0.023101 * rh)
                - 4.686035;
        return -0.2442 + 0.55399 * tw + 0.45535 * t
                - 0.0022 * tw * tw + 0.00278 * tw * t + 3.0;
    }

    private static double winter(double t, double windMs) {
        double vKmh = windMs * 3.6;
        double v016 = Math.pow(vKmh, 0.16);
        return 13.12 + 0.6215 * t - 11.37 * v016 + 0.3965 * t * v016;
    }
}
