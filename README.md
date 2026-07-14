# haeyaji-be

날씨 기반 추천 투두리스트 백엔드 (Spring Boot). fe ↔ be ↔ nlp 중 **be**(저장/식별/중계) 담당.

- **스택**: Spring Boot 4.0.6 · Java 21 · Gradle 9.4.1 (sapari-be와 버전 통일)
- **포트**: `8090` (sapari-be 기본 8080과 충돌 방지, `SERVER_PORT`로 override)
- **context-path**: `/api`

## 패키지 구조 (도메인별 3-tier)

```
com.haeyaji.be
├── BeApplication.java   # 엔트리포인트
├── config/              # 전역 설정 (CORS, WebClient 빈)
├── common/              # 공통 응답/예외 (ErrorCode, GlobalExceptionHandler ...)
│
├── weather/             # 날씨 API 중계 (FR-2)   ✅ 구현
├── recommend/           # 장소검색·추천 (FR-3·4) ✅ 구현(카카오 프록시)
├── todo/                # 할일 (FR-1)            ⬜ 골격
├── routine/             # 루틴 (FR-5)            ⬜ 골격
├── user/                # 사용자·인증 (FR-6)      ⬜ 골격
└── profile/             # 개인화 (FR-7)          ⬜ 골격
```

**각 도메인은 아래 계층으로 나뉜다** (기술 계층별 3-tier). 새 파일을 만들 땐 아래 표대로 위치를 고르면 된다.

```
<도메인>/
├── controller/   # ① HTTP 진입점
├── dto/          # ② 요청/응답 객체
├── service/      # ③ 비즈니스 로직
├── client/       # ④-a 외부 API 호출 (weather·recommend)   ← 외부 REST를 부르는 도메인
│   또는
├── repository/   # ④-b DB 접근 (todo·user·routine·profile) ← DB를 쓰는 도메인
└── domain/       # ⑤ 핵심 도메인 객체·규칙
```

### 계층별 — "여기엔 이런 걸 넣는다"

| 계층 | 역할 | 넣는 것 | 예시 |
|---|---|---|---|
| `controller` | HTTP 요청 수신·파라미터 검증, `service` 호출, DTO 반환 | `@RestController` 클래스 | `WeatherController` |
| `dto` | 요청/응답 전용 객체 (**camelCase**, 보통 `record` + 정적 팩토리 `from()`) | 응답/요청 DTO | `WeatherResponse` |
| `service` | 비즈니스 로직·조합·트랜잭션 경계. 외부호출/DB는 아래 계층에 위임 | `@Service` 클래스, 파라미터 `record`(`WeatherQuery`) | `WeatherService` |
| `client` | **외부 REST API** 호출·응답 매핑 (`WebClient`) | `@Component` 클라이언트 | `KmaWeatherClient`, `KakaoLocalClient` |
| `repository` | **DB** 접근 (JPA) | `JpaRepository`·Entity (todo 이후 추가 예정) | — |
| `domain` | 핵심 도메인 객체·값·규칙 (프레임워크 의존 X) | `record`/enum/도메인 로직 | `Weather`, `WeatherCondition` |

> **의존 방향(중요)**: `controller → service → (client | repository) → domain`.
> 위 방향으로만 의존한다. 역방향(예: service가 controller를 앎)이나 계층 건너뛰기(controller가 client 직접 호출)는 하지 않는다.
> 외부 API를 쓰는 도메인은 `client`, DB를 쓰는 도메인은 `repository` 를 둔다 (둘 다 필요하면 둘 다).

### 참조 구현 (이대로 따라 만들면 됨)
- **weather** — 외부 API(client) 도메인의 표준. `controller/WeatherController` → `service/WeatherService` → `client/kma/KmaWeatherClient` → `domain/Weather`
- **recommend** — 얇은 프록시라도 계층을 지킴. `controller/PlaceController` → `service/PlaceService` → `client/kakao/KakaoLocalClient`

### 새 도메인 만들 때 (예: todo)
1. `todo/domain/` 에 도메인 객체 (`Todo`, enum 등)
2. `todo/repository/` 에 `TodoEntity` + `TodoRepository`(JpaRepository)  ← DB 도메인이므로 client 아님
3. `todo/service/` 에 `TodoService`(`@Service`) — 로직·트랜잭션
4. `todo/dto/` 에 `TodoRequest`/`TodoResponse`(record)
5. `todo/controller/` 에 `TodoController`(`@RestController`, `@RequestMapping("/todos")`)

> 컨트롤러 매핑엔 `/api` 를 붙이지 않는다 — `context-path` 가 이미 `/api` 다. (`@RequestMapping("/todos")` → 실제 `/api/todos`)

현재 **weather·recommend 실구현**, 나머지는 각 계층에 `package-info.java` 골격만 있음.

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
  "uvIndex": 7, "pm10": 33, "pm25": 21,
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
- `uvIndex`: 기상청 생활기상지수(data.go.kr getUVIdxV5) — 대상 날짜 피크값. 보강용이라 실패 시 `null`
- `pm10`/`pm25`: 에어코리아(data.go.kr) — **사용자 좌표 최근접 측정소** 실측값(동 단위). **오늘 조회만** 채움(실시간 관측). 키·실패 시 `null`
- 위 둘은 **fail-soft**: 실패해도 해당 필드만 `null`, 날씨 본체는 `200` 유지
- 단기/중기 본체 API 실패 시에만 `502` → fe 폴백

### 대기오염(미세먼지) 조회 방식

```
[1콜/일]    getMsrstnList        → 전국 측정소 좌표 673개, 24시간 캐시
[1콜/시간]  getCtprvnRltmMesureDnsty?sidoName=전국
                                 → 전국 실측값 673개(~350KB), 60분 캐시
사용자 좌표 → 최근접 측정소 정렬 → 가장 가까운 측정소의 pm10/pm25
   ├─ 결측("-")이면 다음 측정소로 (최대 10곳)
   └─ 측정소 좌표 미확보 시 시도 평균 폴백
```

- 상위 호출은 사용자·지역 수와 무관하게 **최대 25건/일** (data.go.kr 일일 제한 500건 대비 5%)
- 캐시는 전부 서버 인메모리(`ConcurrentHashMap`/`AtomicReference`) — 총 1MB 미만, 재시작 시 워밍업이 재충전.
  단일 인스턴스 전제라 Redis 불필요, 스케일아웃 시 어댑터 계층만 교체

### be가 처리하는 것
- 기상청 인증키 보관, CORS, **위경도 → 격자(nx,ny) 변환**, **→ 중기 지역코드/시도 최근접 매핑**, **→ 최근접 측정소 매칭**
- 응답 캐싱(기본 30분), cond 4분류 매핑, 체감온도 파생, 발표시각 자동 계산(늦은 밤 이전 발표 폴백)
- data.go.kr 콜드스타트 대비 기동 시 커넥션·캐시 워밍업

### 키 2종

**① apihub authKey** (`KMA_AUTH_KEY`) — 날씨 본체
- 동네예보 `VilageFcstInfoService_2.0` (단기) — **필수**
- 중기육상예보 `getMidLandFcst`, 중기기온 `fct_afs_wc.php` — **+4일 이후 조회 시 필수**

**② data.go.kr serviceKey** (`DATA_GO_KR_SERVICE_KEY`) — 보강(UV·미세먼지), 계정당 1키 공용
- `기상청_생활기상지수 조회서비스(3.0)`(getUVIdxV5) — uvIndex
- `한국환경공단_에어코리아_대기오염정보`(getCtprvnRltmMesureDnsty) — pm10/pm25 실측
- `한국환경공단_에어코리아_측정소정보`(getMsrstnList) — 측정소 좌표(최근접 매칭용)
- 셋 다 data.go.kr에서 활용신청하면 **같은 serviceKey 하나로** 동작. 없으면 해당 필드 null(측정소정보만 없으면 시도 평균 폴백).

## fe 연동

- fe는 be를 `http://localhost:8090/api` 로 호출 (fe `.env`의 `VITE_API_BASE` 갱신 필요).
- 응답은 **envelope 없이 raw JSON**, 네이밍은 **camelCase** (nlp 계약과 톤 일치).
- 에러는 `{ "code": ..., "message": ... }` + HTTP status(400/404/502).
- **CORS**: `allowCredentials(true)` 설정됨 → axios 사용 시 `withCredentials: true` (쿠키 인증 대비). 허용 오리진은 `CORS_ALLOWED_ORIGINS`.
