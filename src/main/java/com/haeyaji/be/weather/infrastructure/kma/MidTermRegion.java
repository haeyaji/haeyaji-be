package com.haeyaji.be.weather.infrastructure.kma;

import java.util.List;

/**
 * 중기예보 지역코드 매핑. 중기예보는 격자가 아니라 지역코드 기반이라,
 * 위경도를 대표 도시 좌표와 비교해 가장 가까운 지역의 코드를 고른다.
 *
 * <ul>
 *   <li>{@code landRegId}: 중기육상예보(getMidLandFcst)용 예보구역코드(광역)</li>
 *   <li>{@code taRegId}: 중기기온(fct_afs_wc.php)용 지역코드(도시 단위)</li>
 * </ul>
 * 중기예보는 해상도가 낮으므로 대표 도시 최근접 매핑으로 충분하다.
 */
record MidTermRegion(String name, double lat, double lng, String landRegId, String taRegId) {

    private static final List<MidTermRegion> REGIONS = List.of(
            new MidTermRegion("서울", 37.5665, 126.9780, "11B00000", "11B10101"),
            new MidTermRegion("인천", 37.4563, 126.7052, "11B00000", "11B20201"),
            new MidTermRegion("수원", 37.2636, 127.0286, "11B00000", "11B20601"),
            new MidTermRegion("춘천", 37.8813, 127.7300, "11D10000", "11D10301"),
            new MidTermRegion("강릉", 37.7519, 128.8761, "11D20000", "11D20501"),
            new MidTermRegion("청주", 36.6424, 127.4890, "11C10000", "11C10301"),
            new MidTermRegion("대전", 36.3504, 127.3845, "11C20000", "11C20401"),
            new MidTermRegion("전주", 35.8242, 127.1480, "11F10000", "11F10201"),
            new MidTermRegion("광주", 35.1595, 126.8526, "11F20000", "11F20501"),
            new MidTermRegion("대구", 35.8714, 128.6014, "11H10000", "11H10701"),
            new MidTermRegion("안동", 36.5684, 128.7294, "11H10000", "11H10501"),
            new MidTermRegion("부산", 35.1796, 129.0756, "11H20000", "11H20201"),
            new MidTermRegion("울산", 35.5384, 129.3114, "11H20000", "11H20101"),
            new MidTermRegion("창원", 35.2280, 128.6811, "11H20000", "11H20301"),
            new MidTermRegion("제주", 33.4996, 126.5312, "11G00000", "11G00201")
    );

    /** 주어진 좌표에서 가장 가까운 중기예보 지역. */
    static MidTermRegion nearest(double lat, double lng) {
        MidTermRegion best = REGIONS.get(0);
        double bestDist = Double.MAX_VALUE;
        for (MidTermRegion r : REGIONS) {
            double d = squaredDistance(lat, lng, r.lat, r.lng);
            if (d < bestDist) {
                bestDist = d;
                best = r;
            }
        }
        return best;
    }

    private static double squaredDistance(double lat1, double lng1, double lat2, double lng2) {
        // 한국 위도대에서 경도 1도의 실거리 보정(cos ~0.8). 최근접 비교엔 이 근사로 충분.
        double dLat = lat1 - lat2;
        double dLng = (lng1 - lng2) * 0.8;
        return dLat * dLat + dLng * dLng;
    }
}
