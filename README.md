# haeyaji-be

날씨 기반 추천 투두리스트 백엔드 (Spring Boot). fe ↔ be ↔ nlp 중 **be**(저장·인증·중계) 담당.

- **스택**: Spring Boot 4.0.6 · Java 21 · Gradle 9.4.1
- **저장소**: MySQL 8 (JPA) · Redis 7 (캐시·리프레시 토큰·알림 pub/sub)
- **포트**: `8090` (`SERVER_PORT`로 override) · **context-path**: `/api`

## 도메인

각 도메인은 아래 계층으로 나뉜다 (도메인별 3-tier).

| 도메인 | 하는 일 |
|---|---|
| `member` | 소셜 로그인(카카오·네이버·구글), JWT 발급·재발급, 닉네임·이메일 온보딩 |
| `todo` | 할 일 CRUD, 라벨 필터, 공유(초대·권한·나가기) |
| `routine` | 반복 일정 정의, 매일 자정 그날치 할 일로 전개 |
| `label` | 할 일 분류용 라벨 |
| `friend` | 친구 요청·수락·거절·삭제 |
| `meeting` | 약속 생성·수정·삭제·초대·가능시간 투표·집계·확정 |
| `notification` | 알림 저장·조회, SSE 실시간 푸시, 리마인더 배치, 메일 발송 |
| `weather` | 기상청·에어코리아 중계 (단기/중기 예보, 자외선, 미세먼지) |
| `recommend` | 카카오 장소 검색·지오코딩, nlp 게이트웨이 |
| `profile` | 개인화 — 설문 취향 + 행동 가중치 학습 |

## 패키지 구조

```
<도메인>/
├── controller/    ① HTTP 진입점        @RestController
├── dto/           ② 요청/응답 객체      record + 정적 팩토리 from()
├── service/       ③ 비즈니스 로직·트랜잭션 경계
├── client/        ④-a 외부 REST 호출    WebClient  (weather·recommend)
├── repository/    ④-b DB 접근          JpaRepository + Entity
└── domain/        ⑤ 도메인 객체·규칙·이벤트  (프레임워크 의존 X)
```

> **의존 방향**: `controller → service → (client | repository) → domain`.
> 역방향이나 계층 건너뛰기(controller가 client 직접 호출)는 하지 않는다.
> 컨트롤러 매핑에 `/api`를 붙이지 않는다 — `context-path`가 이미 `/api`다.

### 참조 구현

| 만들려는 것 | 따라 볼 곳 |
|---|---|
| DB 도메인 표준 | `todo` — Entity 정적 팩토리 + `toDomain()`, `member_id`는 연관관계 없이 `@Column UUID` |
| 외부 API 도메인 | `weather` — `WeatherService` → `client/kma/*` → `domain/Weather` |
| 도메인 간 연동 | `meeting` → 이벤트 발행 → `notification`·`todo`가 각자 구독 (아래 참조) |

### 도메인 간 연동은 이벤트로

한 도메인이 다른 도메인의 서비스·레포지토리를 직접 부르지 않는다. 도메인 이벤트를 발행하고,
관심 있는 쪽이 `@TransactionalEventListener`로 구독한다.

```
MeetingConfirmedEvent  → todo: 공유 할 일 생성      (MEET-10)
                       → notification: 확정 알림·메일
MeetingDeletedEvent    → todo: 파생 할 일 회수
                       → notification: 깨진 링크 알림 정리
TodoSharedEvent        → notification: 초대 알림·메일
FriendRequestedEvent   → notification: 친구 요청 알림
```

> **`AFTER_COMMIT` 리스너에서 저장할 땐 `Propagation.REQUIRES_NEW`**.
> 기본 `REQUIRED`로 두면 이미 커밋된 트랜잭션에 참여해 **저장이 조용히 사라진다**
> (알림이 통째로 유실됐던 실제 원인 — `NotificationService.send` 참고).

## 실행

```bash
cp application-secret.yml.example application-secret.yml   # 실제 키 채우기 (gitignore됨)
docker compose up -d                                       # MySQL 3306 · Redis 6379
./gradlew bootRun
```

- 포트 충돌 시 `.env`에 `MYSQL_PORT`/`REDIS_PORT` 지정 (팀 관례: `3316`/`6389`).
- 스키마는 `data/init.sql`이 MySQL 최초 기동 때 적용된다. **손으로 고치지 말 것** —
  실행 중인 앱 스키마에서 `mysqldump --no-data`로 재생성한다(파일 상단 주석 참고).
- 테스트는 별도 DB(`haeyaji_test`)·Redis DB 1을 쓴다(`src/test/resources/application.yml`).
  개발 DB를 건드리지 않으므로 `./gradlew test`를 아무 때나 돌려도 된다.

## API

응답 형식이 **두 가지**다. 새 엔드포인트는 봉투 형식을 쓴다.

| 형식 | 대상 | 모양 |
|---|---|---|
| 봉투 (`ApiResponse`) | 그 외 전부 | `{ success, code, message, data, timestamp }` |
| raw JSON | `/weather`, `/places/*`, `/auth/*`, `/notifications/stream` | 본문 그대로 |

- 네이밍은 **camelCase** (nlp 계약과 톤 일치).
- 에러 `code`는 현재 **HTTP 상태 이름**(`CONFLICT` 등)이 나간다. 세부 사유는 `message`로 구분한다.

### 인증

- 소셜 로그인 성공 → `accessToken`·`refreshToken`을 **HttpOnly 쿠키**로 발급, fe 콜백으로 리다이렉트.
- 리프레시 토큰은 Redis 보관(재사용 감지). 로그아웃 시 삭제.
- **CSRF 켜져 있음** — 변경 요청(POST/PATCH/PUT/DELETE)엔 `X-XSRF-TOKEN` 헤더가 필요하다.
  쿠키 `XSRF-TOKEN`은 아무 GET 요청에서나 받아올 수 있다(`CookieCsrfTokenRepository.withHttpOnlyFalse`).
- 인증 없이 되는 곳: `/`, `/login/**`, `/oauth2/**`, `/auth/reissue`, `/weather/**`, `/places/**`, `/message`.

```
POST   /api/auth/reissue                access 재발급
POST   /api/auth/logout
GET    /api/members/me
PATCH  /api/members/me/nickname         온보딩 — 닉네임 + 이메일
GET    /api/members/search/{nickname}   친구 추가용 검색
```

> 카카오는 이메일을 안 내려준다(비즈 앱 검수 전). 그래서 온보딩에서 직접 받고,
> 소셜에서 받아온 값이 있으면 그걸 채운 뒤 비어 있을 때만 입력받는다.

`GET /api/` · `GET /api/me`는 **로그인·JWT 확인용 디버그 엔드포인트**다(`MainController`).
fe가 쓸 것은 `GET /api/members/me`.

### 할 일 · 라벨 · 루틴

```
GET    /api/todos?date={yyyy-MM-dd}&labelId={uuid}   내 것 + 수락한 공유 할 일
POST   /api/todos                                    source=MANUAL|AI만 클라가 지정 가능
PATCH  /api/todos/{id}                               날짜 이동 포함(과거 금지)
DELETE /api/todos/{id}                               주인=삭제 / 공유받은 사람=내 참여만 해제

POST   /api/todos/{id}/share                         친구에게만 공유 가능
POST   /api/todos/{id}/accept | /reject | /leave
GET    /api/todos/invitations | /shared
GET    /api/todos/{id}/participants
PATCH  /api/todos/{id}/participants/{memberId}       권한 변경 (owner만)
DELETE /api/todos/{id}/participants/{memberId}

GET|POST|PATCH|DELETE  /api/labels[/{id}]
GET|POST|PATCH|DELETE  /api/routines[/{id}]
POST   /api/routines/apply                           기간 지정 일괄 전개
```

- 권한: `OWNER > EDITOR > VIEWER`. EDITOR는 내용 수정까지, 참여자·권한 변경과 삭제는 OWNER만.
- 라벨 필터는 **내 소유 할 일만** 거른다 — 공유받은 할 일의 라벨은 남의 라벨이라 이름이 샌다.

### 약속

```
POST   /api/meetings                                 후보 날짜 + 시간대 → 시간 칸 생성
GET    /api/meetings | /{shareToken}
PATCH  /api/meetings/{shareToken}                    방장, 응답 들어오기 전까지만
DELETE /api/meetings/{shareToken}                    방장, 딸린 행·파생 할 일·알림까지 정리
POST   /api/meetings/{shareToken}/invitations        친구 초대 (PENDING 행으로 영속)
POST   /api/meetings/{shareToken}/participants       합류 = 초대 수락
POST   /api/meetings/{shareToken}/reject             초대 거절 (거절해도 재초대 가능)
GET    /api/meetings/invitations                     내가 받은 대기 초대
PUT    /api/meetings/{shareToken}/responses          가능 시간 제출 (full-replace)
GET    /api/meetings/{shareToken}/best-times | /heatmap | /status
PATCH  /api/meetings/{shareToken}/confirm            방장 확정 → 공유 할 일로 전환
```

- **집계 공개 조건**: 수락 참여자가 **전원 응답**했거나 **마감(기본 2시간)이 지났을 때**.
  그전엔 중간 결과를 최종 추천처럼 보여주지 않는다(`revealed=false` + 진행 상황만).
- 초대는 알림뿐 아니라 `meeting_participant` PENDING 행으로도 남는다 — 알림이 유실돼도
  초대함에서 다시 찾을 수 있다. PENDING은 참여 인원·응답 권한에서 빠진다.
- 수정을 응답 전까지로 잠그는 이유: 시간 칸을 다시 깔면 남이 골라둔 가능 시간이 통째로
  사라지는데 고른 사람은 알 방법이 없다.

### 알림

```
GET    /api/notifications?type={}&cursor={}&size={}          커서 페이지네이션(size 1~100)
GET    /api/notifications/unread-count
POST   /api/notifications/{id}/read | /read-all
DELETE /api/notifications/{id}
GET    /api/notifications/stream                             SSE 실시간 푸시
```

발행 타입 11종. 이벤트 기반 8종 + 배치 3종.

| 분류 | 타입 | 시점 |
|---|---|---|
| 약속 | `MEETING_INVITE` · `MEETING_INVITE_RESPONSE` · `MEETING_CONFIRMED` | 초대·응답·확정 |
| 공유 | `SHARE_INVITE` · `SHARE_INVITE_RESPONSE` · `TODO_SHARED_UPDATED` | 초대·응답·수정 |
| 친구 | `FRIEND_REQUEST` · `FRIEND_RESPONSE` | 요청·수락/거절 |
| 배치 | `TODO_REMINDER` · `MEETING_REMINDER` | 시작 10분 전 (5분 주기) |
| 배치 | `TODO_WEATHER_ALERT` | 매일 07:00, 좌표 있는 오늘 일정이 비·눈 예보일 때 |

- 목록 필터는 **타입 단건**(`type=MEETING_INVITE`)만 된다. 카테고리(INVITE/TODO/FRIEND) 단위
  묶음 필터는 아직 없다 — fe가 탭으로 나눌 거면 be에 추가가 필요하다.
- **자기 행동으로 생긴 알림은 자신에게 안 보낸다**.
- **중복 방지**는 유니크 제약 `(member_id, type, ref_id)`. 배치는 "창에 걸린 것"만 훑고
  어디까지 보냈는지 기록하지 않는다 — 실행이 밀려도 다음 주기가 주워 담고, 이미 보낸 건 걸러진다.
- 실시간 푸시: 알림 저장 → Redis pub/sub 발행 → 각 인스턴스가 자기가 들고 있는 SSE 연결로 전달.
  Redis가 죽어도 **저장은 유지**된다(푸시는 부가 기능).
- 메일: Resend **HTTP API**(SMTP 587이 개발 환경에서 막힘). 템플릿은 `resources/mail/`,
  원본·디자인 규칙은 `docs/email/`. 비동기 + 실패 흡수라 메일이 실패해도 본 트랜잭션에 영향 없다.
  배치 메일은 **알림이 새로 만들어졌을 때만** 보낸다 — 아니면 5분마다 같은 메일이 나간다.

### 친구

```
POST   /api/friends/requests | /{id}/accept | /{id}/reject
GET    /api/friends | /requests
DELETE /api/friends/{id}
```

상대가 이미 나에게 요청을 보낸 상태면 새로 만들지 않고 **바로 수락**으로 처리한다(양방향 중복 방지).

### 날씨

```
GET /api/weather?lat={위도}&lng={경도}&date={yyyy-MM-dd}
```

`date` 미지정 시 오늘. 예보 범위(오늘 ~ +10일) 밖이면 오늘로 대체.

```json
{
  "cond": "sunny", "condKo": "맑음",
  "temp": 26, "hi": 27, "lo": 19, "feels": 27,
  "pop": 5, "humidity": 45, "windMs": 3.2,
  "uvIndex": 7, "pm10": 33, "pm25": 21,
  "hourly": [ { "time": "15:00", "temp": 27, "pop": 5 } ]
}
```

| 범위 | 소스 | 제공 필드 |
|---|---|---|
| 오늘 ~ +3일 | 단기예보 `getVilageFcst` (격자 5km) | 전 필드 + `hourly` |
| +4 ~ +10일 | 중기예보 `getMidLandFcst` + `fct_afs_wc.php` | `cond/temp/hi/lo/pop`. 나머지 `null`, `hourly` `[]` |

- `cond`: `sunny` \| `cloudy` \| `rainy` \| `snowy`.
  비/눈 혼재(PTY 2·6)는 기온 ≤1℃면 `snowy`.
- `uvIndex`·`pm10`·`pm25`는 **fail-soft** — 실패해도 해당 필드만 `null`, 본체는 200.
  단기/중기 본체가 실패할 때만 502 → fe 폴백.
- 미세먼지는 사용자 좌표 **최근접 측정소** 실측값. 결측이면 다음 측정소로(최대 10곳),
  측정소 좌표를 못 받으면 시도 평균 폴백.

be가 처리하는 것: 인증키 보관, **위경도 → 격자(nx,ny)**, **→ 중기 지역코드**, **→ 최근접 측정소** 변환,
cond 4분류 매핑, 발표시각 자동 계산(늦은 밤 이전 발표 폴백), 기동 시 커넥션·캐시 워밍업.

### 장소 · AI 추천

```
GET  /api/places/search?query={}&lat={}&lng={}&radiusM={}&size={}
GET  /api/places/geocode?query={}
POST /api/message                    nlp 게이트웨이 (인증 시 개인화 보강)
POST /api/recommend/feedback/choice  선택 신호 학습
GET|POST /api/preferences            온보딩 설문
```

- `size` 미지정이면 **반경 내 전부** 반환(페이지네이션). 지정하면 그 개수만.
- `/message`는 fe → be → nlp 대행 호출이다. be가 개인화 프로필(취향·가중치 top-K·최근 선택)과
  스케줄 맥락(다음 일정까지 `gapMinutes`)을 실어 보낸다. **미인증도 동작**한다(프로필 없이).
- 개인화 학습: 추천 카테고리를 보여주고 고른 것 `+2`, 안 고른 것 `-0.05`.
  맥락축은 **날씨(비/맑음) × 시간대**라 같은 사람도 상황별로 다른 취향이 쌓인다.
  주 1회 `×0.9` 감쇠.

## Redis

| 용도 | 키 | TTL |
|---|---|---|
| 리프레시 토큰 | `refreshToken:{memberId}` | 토큰 수명 |
| 알림 실시간 발행 | pub/sub 채널 (저장 X) | — |
| 날씨 응답 | `weather:v1:*` | 10분(오늘)·30분 |
| 기상청 원본 | `kma:short|nowcast|ultra|mid|uv:v1:*` | 90분 ~ 13시간 |
| 대기오염 | `air:v1:*` | 측정값 60분 · 측정소 24시간 |

- 캐시 실패는 전부 흡수한다 — Redis가 죽어도 상류를 직접 호출해 응답은 나간다(`RedisCacheStore`).
- 에어코리아는 실패 시 **5분 음성 캐시**를 둔다(장애 중 재시도 폭주 방지).

## 배치

| 주기 | 하는 일 |
|---|---|
| 5분 | 다가온 할 일·약속 리마인더 (`haeyaji.notification.reminder-cron`) |
| 25초 | SSE 하트비트 (프록시가 유휴 연결을 끊지 않게) |
| 매일 00:00 | 루틴 → 그날치 할 일 전개 |
| 매일 07:00 | 궂은 날씨 알림 (`haeyaji.notification.weather-alert-cron`) |
| 매주 월 04:00 | 개인화 가중치 감쇠 |

## fe 연동

- `VITE_API_BASE` → `http://localhost:8090/api`
- axios `withCredentials: true` (쿠키 인증) + 변경 요청에 `X-XSRF-TOKEN` 헤더
- 허용 오리진은 `CORS_ALLOWED_ORIGINS` (기본 `localhost:5173`, `localhost:3000`)
- 로그인 콜백은 `app.frontend.callback-url` (기본 `http://localhost:5173/oauth/callback`)

## 키

`application-secret.yml`(gitignore) 또는 환경변수로 주입한다.

| 키 | 쓰는 곳 | 없으면 |
|---|---|---|
| `KMA_AUTH_KEY` | 기상청 단기·중기예보 | 날씨 502 |
| `DATA_GO_KR_SERVICE_KEY` | 자외선·미세먼지·측정소 (같은 키 하나로 3종) | 해당 필드 `null` |
| `KAKAO_REST_KEY` | 장소 검색·지오코딩 | 장소 기능 불가 |
| `KAKAO/NAVER/GOOGLE_CLIENT_SECRET` | 소셜 로그인 | 해당 소셜 로그인 불가 |
| `JWT_SECRET` | 토큰 서명 | 기동 실패 |
| `RESEND_API_KEY` | 알림 메일 | 발송 조용히 건너뜀 |

> 메일 발신 주소(`MAIL_FROM`)는 **Resend에 인증된 도메인**이어야 한다 — 미인증이면 403.

## 문서

| 파일 | 내용 |
|---|---|
| `data/init.sql` | 실행용 DDL (앱 스키마에서 생성 — 손편집 금지) |
| `docs/haeyaji-erd.dbml` · `haeyaji-schema.sql` | 설계 의도·컬럼 설명 (문서 전용) |
| `docs/haeyaji-be-requirements.csv` | 요구사항 목록 |
| `docs/email/` | 메일 템플릿 원본 + 클라이언트 호환 규칙 |
