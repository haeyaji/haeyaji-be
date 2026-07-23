-- =====================================================================
-- 해야지(haeyaji) 스키마 (MySQL 8)
-- =====================================================================

CREATE TABLE `member`
(
    `id`           binary(16) PRIMARY KEY COMMENT '사용자 고유 ID',
    `kakao_id`     varchar(50) UNIQUE           NOT NULL COMMENT '카카오 OIDC sub',
    `email`        varchar(255) COMMENT '이메일(카카오 미제공 가능)',
    `nickname`     varchar(50)                  NOT NULL,
    `friend_code`  varchar(10) UNIQUE           NOT NULL COMMENT '친구추가용 코드',
    `status`       ENUM ('ACTIVE', 'WITHDRAWN') NOT NULL DEFAULT 'ACTIVE',
    `created_at`   datetime                     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`   datetime                     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `withdrawn_at` datetime COMMENT '탈퇴 시각(soft delete)'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE `friend`
(
    `id`           binary(16) PRIMARY KEY,
    `requester_id` binary(16)                               NOT NULL COMMENT '요청자',
    `receiver_id`  binary(16)                               NOT NULL COMMENT '수신자',
    `status`       ENUM ('PENDING', 'ACCEPTED', 'REJECTED') NOT NULL DEFAULT 'PENDING',
    `created_at`   datetime                                 NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `accepted_at`  datetime
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE `todo`
(
    `id`            binary(16) PRIMARY KEY COMMENT '할 일 고유 ID (UUIDv7)',
    `member_id`     binary(16)                                  NOT NULL COMMENT '소유자(생성자). 공유는 todo_participant. (예정)인증 후',
    `title`         varchar(100)                                NOT NULL,
    `todo_date`     date                                        NOT NULL COMMENT '할 일 날짜(날짜별 조회 키)',
    `start_time`    time COMMENT '시작 시각(선택). gap 계산·시간추천',
    `place_name`    varchar(100) COMMENT '장소 표시명(카카오 추천). 구현 location→분리 요청 예정',
    `place_url`     varchar(300) COMMENT '장소 딥링크(지도 열기)',
    `lat`           double COMMENT '위도(지도 마커). (예정)구현엔 아직 없음',
    `lng`           double COMMENT '경도(지도 마커). (예정)구현엔 아직 없음',
    `category`      varchar(30) COMMENT 'nlp 6종 코드값(앱에서 강제). 자유문자열=nlp 정합',
    `status`        ENUM ('TODO', 'DONE')                       NOT NULL DEFAULT 'TODO' COMMENT 'TODO/DONE 2상태. DONE=완료',
    `pinned`        boolean                                     NOT NULL DEFAULT false COMMENT '최상단 고정(우선순위 대체). pinned끼리는 created_at 순',
    `sort_order`    int                                         NOT NULL DEFAULT 0 COMMENT '드래그 수동 순서. 기본=created_at 순',
    `ended_at`      datetime COMMENT '완료 누른 시각. DONE시 set, TODO복귀시 null',
    `source`        ENUM ('MANUAL', 'AI', 'ROUTINE', 'MEETING') NOT NULL DEFAULT 'MANUAL',
    `source_ref_id` binary(16) COMMENT '출처 원본ID(ROUTINE→routine, MEETING→meeting). 다형참조',
    `created_at`    datetime                                    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`    datetime                                    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE `todo_participant`
(
    `id`            binary(16) PRIMARY KEY,
    `todo_id`       binary(16)                               NOT NULL,
    `member_id`     binary(16)                               NOT NULL COMMENT '공유 대상',
    `role`          ENUM ('OWNER', 'EDITOR', 'VIEWER')       NOT NULL COMMENT 'OWNER>EDITOR>VIEWER. 소유자는 todo.member_id',
    `invite_status` ENUM ('PENDING', 'ACCEPTED', 'REJECTED') NOT NULL DEFAULT 'PENDING' COMMENT '공유 초대 수락 상태(PENDING/ACCEPTED/REJECTED)',
    `created_at`    datetime                                 NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE `routine`
(
    `id`         binary(16) PRIMARY KEY,
    `member_id`  binary(16)   NOT NULL,
    `title`      varchar(100) NOT NULL,
    `start_time` time,
    `is_active`  boolean      NOT NULL DEFAULT true,
    `created_at` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE `routine_day`
(
    `id`          binary(16) PRIMARY KEY,
    `routine_id`  binary(16)                                             NOT NULL,
    `day_of_week` ENUM ('MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT', 'SUN') NOT NULL COMMENT '반복 요일(프리셋은 요일 조합으로)'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE `meeting`
(
    `id`                 binary(16) PRIMARY KEY COMMENT '약속 고유 ID',
    `creator_id`         binary(16)                                  NOT NULL COMMENT '생성자=방장(owner). 소유권 위임은 이 값을 갱신',
    `title`              varchar(100)                                NOT NULL COMMENT '약속 제목',
    `type`               ENUM ('CASUAL', 'TEAM', 'REGULAR', 'ETC')   NOT NULL COMMENT '약속 종류',
    `time_start`         time                                        NOT NULL COMMENT '그리드 세로축 시작(후보 아님, 테두리). 예 09:00. 여기부터 슬롯 생성',
    `time_end`           time                                        NOT NULL COMMENT '그리드 세로축 끝(테두리). time_start보다 작으면 자정 넘김(익일)',
    `slot_unit_minutes`  int                                         NOT NULL DEFAULT 30 COMMENT '칸 하나 크기(분). 30 또는 60만 허용. time_start~end를 이 단위로 쪼갬',
    `deadline`           datetime COMMENT '응답 마감 시각. 지나면 만료 판정. null=무제한',
    `status`             ENUM ('COLLECTING', 'CONFIRMED', 'EXPIRED') NOT NULL DEFAULT 'COLLECTING' COMMENT 'COLLECTING(수집중)->CONFIRMED(확정)->EXPIRED(만료)',
    `confirmed_start_at` datetime COMMENT '방장이 확정한 최종 시작(프론트 confirmed.date+startH). 미확정 시 null',
    `confirmed_end_at`   datetime COMMENT '방장이 확정한 최종 끝(endH 배타). 미확정 시 null',
    `share_token`        varchar(64) UNIQUE                          NOT NULL COMMENT '초대 URL 토큰(추측불가). 이 URL로 로그인 후 참여',
    `created_at`         datetime                                    NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='약속 본체. time_start/end는 그리드 범위(후보 아님), 실제 후보 칸은 meeting_time_slot';

-- 후보 "날짜"들 — 범위(from~to) 아님. 개별 저장이라 월·수·금 같은 산발 선택 가능
CREATE TABLE `meeting_date`
(
    `id`             binary(16) PRIMARY KEY,
    `meeting_id`     binary(16) NOT NULL COMMENT '소속 약속',
    `candidate_date` date       NOT NULL COMMENT '후보 날짜 1개(개별 저장). 프론트 Meetup.dates[]의 원소 하나'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='후보 날짜(개별). 범위 아님';

-- 참여자 명단 — share_token URL로 로그인 합류하면 행 추가
CREATE TABLE `meeting_participant`
(
    `id`         binary(16) PRIMARY KEY,
    `meeting_id` binary(16) NOT NULL COMMENT '소속 약속',
    `member_id`  binary(16) NOT NULL COMMENT '참여자(로그인 필수). share_token URL로 합류하면 이 행 추가',
    `joined_at`  datetime   NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='약속 참여자 명단';

-- 투표 "칸"(그리드 셀) — be가 (후보날짜 x time_start~end / slot_unit)로 자동 생성
CREATE TABLE `meeting_time_slot`
(
    `id`            binary(16) PRIMARY KEY COMMENT '투표 칸 하나(그리드 셀)',
    `meeting_id`    binary(16) NOT NULL COMMENT '소속 약속',
    `slot_start_at` datetime   NOT NULL COMMENT '칸 시작 DATETIME. 예 2026-07-20 14:00. 프론트 myCells 키 date|hour 대응(새벽=날짜+1 정규화)'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='투표 칸. (후보날짜 x 시간범위 / 단위)로 생성';

-- 칸별 응답 — 한 사람이 한 칸에 되는/안되는 하나. 행 없으면 미응답
CREATE TABLE `meeting_response`
(
    `id`                   binary(16) PRIMARY KEY,
    `meeting_time_slot_id` binary(16)            NOT NULL COMMENT '어느 칸(slot)에 대한 응답인지',
    `member_id`            binary(16)            NOT NULL COMMENT '누가 응답했는지',
    `status`               ENUM ('FREE', 'BUSY') NOT NULL COMMENT 'FREE(되는)/BUSY(안되는). 행 없음=미응답. 히트맵 집계는 FREE만 카운트',
    `updated_at`           datetime              NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '재수정 가능(마지막 응답 시각)'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='칸별 free/busy 응답';

-- member_preference: member와 1:1 공유PK. id 값이 곧 member.id (@MapsId 패턴)
CREATE TABLE `member_preference`
(
    `id`                   binary(16) PRIMARY KEY COMMENT 'member와 1:1. 값=member.id',
    `preferred_categories` json COMMENT '설문 선호 카테고리 예 실내,맛집카페',
    `avoid`                json COMMENT '회피 요소 예 시끄러운 곳',
    `vibe`                 varchar(20) COMMENT '조용한/활기찬/감성적인/트렌디한/편안한',
    `intensity`            varchar(20) COMMENT '가볍게/적당히/적극적으로',
    `created_at`           datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`           datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

-- 복합 PK 테이블(단일 id 없음) — member_id는 FK 겸 PK 구성요소
CREATE TABLE `member_category_weight`
(
    `member_id`       binary(16)                                                                        NOT NULL,
    `ctx_weather`     ENUM ('RAINY', 'CLEAR')                                                           NOT NULL,
    `ctx_time_of_day` ENUM ('MORNING', 'AFTERNOON', 'EVENING', 'NIGHT')                                 NOT NULL,
    `category`        ENUM ('CAFE_DESSERT', 'RESTAURANT', 'NATURE_WALK', 'SPORTS_ACTIVITY', 'CULTURE_EXHIBIT', 'INDOOR_PLAY', 'REST_HEALING', 'STUDY_WORK', 'SOCIAL', 'SHOPPING') NOT NULL,
    `weight`          double                                                                            NOT NULL DEFAULT 0 COMMENT 'SEL+2/ADD+1/IGN-0.5(선택시점 학습, DONE 제외), 주1회 x0.9 decay',
    `updated_at`      datetime                                                                          NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`member_id`, `ctx_weather`, `ctx_time_of_day`, `category`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE `member_keyword_weight`
(
    `member_id`  binary(16)  NOT NULL,
    `keyword`    varchar(50) NOT NULL COMMENT 'nlp 자유 검색어(방탈출 등). enum 아님',
    `weight`     double      NOT NULL DEFAULT 0 COMMENT 'category_weight와 동일 공식·decay',
    `updated_at` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`member_id`, `keyword`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE `notification`
(
    `id`         binary(16) PRIMARY KEY,
    `member_id`  binary(16)                                                                                                                 NOT NULL COMMENT '수신자',
    `category`   ENUM ('INVITE', 'TODO')                                                                                                    NOT NULL COMMENT 'INVITE(초대류) / TODO(투두류) 대분류',
    `type`       ENUM ('MEETING_INVITE', 'SHARE_INVITE', 'MEETING_CONFIRMED', 'TODO_REMINDER', 'TODO_WEATHER_ALERT', 'TODO_SHARED_UPDATED') NOT NULL COMMENT '세부 유형',
    `title`      varchar(100)                                                                                                               NOT NULL,
    `body`       varchar(255),
    `ref_id`     binary(16) COMMENT '연관 대상 id(meeting/todo 등), 다형참조',
    `link_token` varchar(64) COMMENT '초대류면 meeting.share_token',
    `is_read`    boolean                                                                                                                    NOT NULL DEFAULT false,
    `created_at` datetime                                                                                                                   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `read_at`    datetime
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

-- ---------- 인덱스 ----------
CREATE UNIQUE INDEX `uk_friend_pair` ON `friend` (`requester_id`, `receiver_id`);
CREATE INDEX `idx_todo_member_date` ON `todo` (`member_id`, `todo_date`);
CREATE INDEX `idx_todo_member_source` ON `todo` (`member_id`, `source`, `created_at`);
CREATE UNIQUE INDEX `uk_todo_member` ON `todo_participant` (`todo_id`, `member_id`);
CREATE UNIQUE INDEX `uk_routine_day` ON `routine_day` (`routine_id`, `day_of_week`);
CREATE UNIQUE INDEX `uk_meeting_date` ON `meeting_date` (`meeting_id`, `candidate_date`);
CREATE UNIQUE INDEX `uk_meeting_member` ON `meeting_participant` (`meeting_id`, `member_id`);
CREATE UNIQUE INDEX `uk_meeting_slot` ON `meeting_time_slot` (`meeting_id`, `slot_start_at`);
CREATE UNIQUE INDEX `uk_slot_member` ON `meeting_response` (`meeting_time_slot_id`, `member_id`);
CREATE INDEX `idx_noti_inbox` ON `notification` (`member_id`, `is_read`, `created_at`);

-- ---------- 외래키 (참조 대상 PK = id) ----------
ALTER TABLE `friend`
    ADD FOREIGN KEY (`requester_id`) REFERENCES `member` (`id`);
ALTER TABLE `friend`
    ADD FOREIGN KEY (`receiver_id`) REFERENCES `member` (`id`);
ALTER TABLE `todo`
    ADD FOREIGN KEY (`member_id`) REFERENCES `member` (`id`);
ALTER TABLE `todo_participant`
    ADD FOREIGN KEY (`todo_id`) REFERENCES `todo` (`id`);
ALTER TABLE `todo_participant`
    ADD FOREIGN KEY (`member_id`) REFERENCES `member` (`id`);
ALTER TABLE `routine`
    ADD FOREIGN KEY (`member_id`) REFERENCES `member` (`id`);
ALTER TABLE `routine_day`
    ADD FOREIGN KEY (`routine_id`) REFERENCES `routine` (`id`);
ALTER TABLE `meeting`
    ADD FOREIGN KEY (`creator_id`) REFERENCES `member` (`id`);
ALTER TABLE `meeting_date`
    ADD FOREIGN KEY (`meeting_id`) REFERENCES `meeting` (`id`);
ALTER TABLE `meeting_participant`
    ADD FOREIGN KEY (`meeting_id`) REFERENCES `meeting` (`id`);
ALTER TABLE `meeting_participant`
    ADD FOREIGN KEY (`member_id`) REFERENCES `member` (`id`);
ALTER TABLE `meeting_time_slot`
    ADD FOREIGN KEY (`meeting_id`) REFERENCES `meeting` (`id`);
ALTER TABLE `meeting_response`
    ADD FOREIGN KEY (`meeting_time_slot_id`) REFERENCES `meeting_time_slot` (`id`);
ALTER TABLE `meeting_response`
    ADD FOREIGN KEY (`member_id`) REFERENCES `member` (`id`);
-- FK 방향 정정: member_preference(자식) → member(부모), 공유PK
ALTER TABLE `member_preference`
    ADD FOREIGN KEY (`id`) REFERENCES `member` (`id`);
ALTER TABLE `member_category_weight`
    ADD FOREIGN KEY (`member_id`) REFERENCES `member` (`id`);
ALTER TABLE `member_keyword_weight`
    ADD FOREIGN KEY (`member_id`) REFERENCES `member` (`id`);
ALTER TABLE `notification`
    ADD FOREIGN KEY (`member_id`) REFERENCES `member` (`id`);

