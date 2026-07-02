# haeyaji-be

날씨 기반 추천 투두리스트 백엔드 (Spring Boot). fe ↔ be ↔ nlp 중 **be**(저장/식별/중계) 담당.

- **스택**: Spring Boot 4.0.6 · Java 21 · Gradle 9.4.1 (sapari-be와 버전 통일)
- **포트**: `8090` (sapari-be 기본 8080과 충돌 방지, `SERVER_PORT`로 override)
- **context-path**: `/api`

## 패키지 구조

```
com.haeyaji.be
├── config/           # CORS, WebClient 설정
├── common/           # 공통 응답/예외
├── todo/             # 할일 CRUD (FR-1)          ┐
├── routine/          # 루틴 (FR-5)                │ 기능별 4계층
├── recommend/        # nlp 게이트웨이 (FR-3)       │ api / application(+port)
├── weather/          # 날씨 API 중계 (FR-2) ✅ 구현 │ / domain / infrastructure
├── profile/          # 개인화 (FR-7)              │
└── user/             # 인증/식별 (FR-6)           ┘
```

기능마다 `api / application(+port/in·out) / domain / infrastructure` 4계층 반복 (nlp 헥사고날과 일관).
현재는 **weather만 실구현**, 나머지는 `package-info.java`로 골격만 잡아둠.

## 실행

```bash
cp application-secret.yml.example application-secret.yml   # 실제 키 채우기 (gitignore됨)
./gradlew bootRun
```

> 키 주입: 루트의 `application-secret.yml`(gitignore) 이 자동 로드됩니다.
> CI/운영에선 `KMA_AUTH_KEY`, `DATA_GO_KR_SERVICE_KEY` 환경변수로 주입해도 동일하게 동작.

## API — 날씨 (FR-2)

```
GET /api/weather?lat={위도}&lng={경도}&date={yyyy-MM-dd}
```

- `lat`, `lng`: 필수 (브라우저 geolocation 좌표)
- `date`: 선택. 미지정 시 오늘. 예보 범위(**오늘 ~ +10일**) 밖이면 오늘로 대체.

응답(200, camelCase):

```json
{
  "cond": "sunny", "condKo": "맑음",
  "temp": 26, "hi": 27, "lo": 19, "feels": 27,
  "pop": 5, "humidity": 45, "windMs": 3.2,
  "uvIndex": null, "pm10": null, "pm25": null,
  "hourly": [ { "time": "15:00", "temp": 27, "pop": 5 } ]
}
```

### 날짜별 데이터 소스 (자동 분기)

| 범위 | 소스 | 제공 필드 |
|---|---|---|
| **오늘 ~ +3일** | 단기예보 `getVilageFcst` (격자 5km) | 전 필드 + `hourly` 시간별 |
| **+4 ~ +10일** | 중기예보 `getMidLandFcst`(하늘·강수) + `fct_afs_wc.php`(최저/최고) | `cond/temp/hi/lo/pop`. `feels/humidity/windMs`=**null**, `hourly`=**[]** |

- `cond`: `sunny` | `cloudy` | `rainy` | `snowy` (단기 SKY/PTY, 중기 하늘상태 텍스트 → 4분류)
  - 눈(PTY 3·7) → `snowy`, 비/눈 혼재(PTY 2·6)는 기온 ≤1℃면 `snowy` 아니면 `rainy`
- `uvIndex`: 기상청 생활기상지수(apihub getUVIdxV3) — 대상 날짜 피크값. 보강용이라 실패 시 `null`
- `pm10`/`pm25`: 에어코리아(data.go.kr) 시도별 실시간 평균 — **오늘 조회만** 채움(실시간 관측). 키·실패 시 `null`
- 위 둘은 **fail-soft**: 실패해도 해당 필드만 `null`, 날씨 본체는 `200` 유지
- 단기/중기 본체 API 실패 시에만 `502` → fe 폴백

### be가 처리하는 것
- 기상청 인증키 보관, CORS, **위경도 → 격자(nx,ny) 변환** 및 **→ 중기 지역코드 최근접 매핑**
- 응답 캐싱(기본 30분), cond 4분류 매핑, 체감온도 파생, 발표시각 자동 계산(늦은 밤 이전 발표 폴백)

### 키 2종

**① apihub authKey** (`KMA_AUTH_KEY`) — 날씨 본체
- 동네예보 `VilageFcstInfoService_2.0` (단기) — **필수**
- 중기육상예보 `getMidLandFcst`, 중기기온 `fct_afs_wc.php` — **+4일 이후 조회 시 필수**

**② data.go.kr serviceKey** (`DATA_GO_KR_SERVICE_KEY`) — 보강(UV·미세먼지), 계정당 1키 공용
- `기상청_생활기상지수 조회서비스`(getUVIdxV5) — uvIndex
- `한국환경공단_에어코리아_대기오염정보`(getCtprvnRltmMesureDnsty) — pm10/pm25
- 둘 다 data.go.kr에서 활용신청하면 **같은 serviceKey 하나로** 동작. 없으면 해당 필드 null.

> ⚠️ fe는 be를 `http://localhost:8090/api` 로 호출 (fe `.env`의 `VITE_API_BASE` 갱신 필요).
