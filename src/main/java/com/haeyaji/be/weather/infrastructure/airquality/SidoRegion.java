package com.haeyaji.be.weather.infrastructure.airquality;

import java.util.List;

/**
 * 시도 단위 지역 매핑. 위경도를 가장 가까운 시도 대표 좌표로 매핑해
 * 자외선지수 행정구역코드(areaNo)와 에어코리아 시도명(sidoName)을 얻는다.
 *
 * <p>UV·미세먼지는 시도/시군구 단위라 대표 좌표 최근접 매핑으로 충분하다.
 */
public record SidoRegion(String name, double lat, double lng, String areaNo, String sidoName) {

    private static final List<SidoRegion> REGIONS = List.of(
            new SidoRegion("서울", 37.5665, 126.9780, "1100000000", "서울"),
            new SidoRegion("부산", 35.1796, 129.0756, "2600000000", "부산"),
            new SidoRegion("대구", 35.8714, 128.6014, "2700000000", "대구"),
            new SidoRegion("인천", 37.4563, 126.7052, "2800000000", "인천"),
            new SidoRegion("광주", 35.1595, 126.8526, "2900000000", "광주"),
            new SidoRegion("대전", 36.3504, 127.3845, "3000000000", "대전"),
            new SidoRegion("울산", 35.5384, 129.3114, "3100000000", "울산"),
            new SidoRegion("세종", 36.4800, 127.2890, "3600000000", "세종"),
            new SidoRegion("경기", 37.2636, 127.0286, "4100000000", "경기"),
            new SidoRegion("강원", 37.8813, 127.7300, "5100000000", "강원"),
            new SidoRegion("충북", 36.6424, 127.4890, "4300000000", "충북"),
            new SidoRegion("충남", 36.6588, 126.6720, "4400000000", "충남"),
            new SidoRegion("전북", 35.8242, 127.1480, "5200000000", "전북"),
            new SidoRegion("전남", 34.8118, 126.3922, "4600000000", "전남"),
            new SidoRegion("경북", 36.5684, 128.7294, "4700000000", "경북"),
            new SidoRegion("경남", 35.2280, 128.6811, "4800000000", "경남"),
            new SidoRegion("제주", 33.4996, 126.5312, "5000000000", "제주")
    );

    public static SidoRegion nearest(double lat, double lng) {
        SidoRegion best = REGIONS.get(0);
        double bestDist = Double.MAX_VALUE;
        for (SidoRegion r : REGIONS) {
            double dLat = lat - r.lat;
            double dLng = (lng - r.lng) * 0.8; // 한국 위도대 경도 보정
            double d = dLat * dLat + dLng * dLng;
            if (d < bestDist) {
                bestDist = d;
                best = r;
            }
        }
        return best;
    }
}
