# =====================================================================
# meeting 도메인 개발·API 테스트용 Makefile
#   위치: scripts/meeting/  →  레포 루트 어디서든:  make -C scripts/meeting <타깃>
#   make -C scripts/meeting help 로 전체 타깃 확인
# =====================================================================
SHELL := /bin/bash
ROOT  := $(abspath $(dir $(lastword $(MAKEFILE_LIST)))/../..)
HERE  := $(abspath $(dir $(lastword $(MAKEFILE_LIST))))

BASE_URL ?= http://localhost:8090/api
M1       ?= 11111111-1111-7111-8111-111111111111
M2       ?= 22222222-2222-7222-8222-222222222222
MYSQL     = docker exec haeyaji-mysql mysql -uhaeyaji -p'haeyaji12!@' haeyaji_db
GRADLE    = cd $(ROOT) && ./gradlew
COMPOSE   = docker compose -f $(ROOT)/docker-compose.yaml

MCURL     = BASE_URL=$(BASE_URL) $(HERE)/mcurl.sh

.DEFAULT_GOAL := help
.PHONY: help build test run db-up db-down db-reset seed api-test spec token \
        meeting-create meeting-get meeting-join meeting-invite meeting-heatmap \
        meeting-best-times meeting-status meeting-list

help: ## 타깃 목록 출력
	@grep -E '^[a-zA-Z_-]+:.*?## ' $(MAKEFILE_LIST) | awk -F':.*?## ' '{printf "  \033[36m%-18s\033[0m %s\n", $$1, $$2}'

# ---------- 빌드/실행 ----------
build: ## 전체 빌드 (단위 테스트 포함)
	$(GRADLE) build

test: ## 단위 테스트만 실행
	$(GRADLE) test

run: ## 앱 기동 (포그라운드, ctrl-c로 종료)
	$(GRADLE) bootRun

# ---------- DB ----------
db-up: ## MySQL 컨테이너 기동 + 준비 대기
	$(COMPOSE) up -d
	@for i in $$(seq 1 45); do \
		$(MYSQL) -e 'select 1' >/dev/null 2>&1 && { echo "MySQL ready"; exit 0; }; sleep 2; \
	done; echo "MySQL 준비 실패"; exit 1

db-down: ## MySQL 컨테이너 중지 (데이터 유지)
	$(COMPOSE) down

db-reset: ## [주의] 볼륨 삭제 후 init.sql로 재초기화 (데이터 전체 삭제)
	@read -p "DB 볼륨을 삭제합니다. 계속? [y/N] " a && [[ $$a == y ]] || exit 1
	$(COMPOSE) down -v
	$(MAKE) db-up

seed: ## API 테스트용 member 2행 시드 (멱등 — 구(init.sql)·신(Member 엔티티) NOT NULL 컬럼 모두 충족)
	@$(MYSQL) -e "insert ignore into member (id, kakao_id, email, nickname, friend_code, social_type, social_type_id, role, status) values \
	 (UUID_TO_BIN('$(M1)'), 'kakao-m1', 'm1@test.io', '방장',   'FRIEND01', 'KAKAO', 'kakao-m1', 'ROLE_USER', 'ACTIVE'), \
	 (UUID_TO_BIN('$(M2)'), 'kakao-m2', 'm2@test.io', '참여자', 'FRIEND02', 'KAKAO', 'kakao-m2', 'ROLE_USER', 'ACTIVE');" 2>/dev/null
	@echo "seeded: M1=$(M1), M2=$(M2)"

# ---------- API 테스트/스펙 ----------
api-test: seed ## meeting API 전체 자동 검증 (앱 기동 필요: make run)
	BASE_URL=$(BASE_URL) M1=$(M1) M2=$(M2) $(HERE)/meeting-api-test.sh

spec: ## API 명세(JSON) 출력 — 프론트 참고용
	@jq . $(HERE)/meeting-api-spec.json

token: ## 테스트용 access token 출력 ([MEMBER=uuid], 기본 M1)
	@$(HERE)/jwt.sh $(or $(MEMBER),$(M1))

# ---------- 개별 호출 (수동 확인용, 인증·CSRF는 mcurl.sh가 처리) ----------
meeting-create: ## 약속 생성 (M1, +7일/+9일, 09~12시 30분 단위)
	@$(MCURL) $(M1) POST /meetings '{ \
	  "title":"수동 테스트","type":"TEAM", \
	  "dates":["'$$(date -d '+7 days' +%F)'","'$$(date -d '+9 days' +%F)'"], \
	  "timeStart":"09:00","timeEnd":"12:00","slotUnitMinutes":30}' | jq .

meeting-get: ## 약속 상세 조회 (TOKEN=... 필수 [MEMBER=uuid], 기본 M1)
	@test -n "$(TOKEN)" || { echo "사용법: make meeting-get TOKEN=<shareToken>"; exit 1; }
	@$(MCURL) $(or $(MEMBER),$(M1)) GET /meetings/$(TOKEN) | jq .

meeting-join: ## 약속 참여 (TOKEN=... [MEMBER=uuid], 기본 M2 — 참여 회원은 토큰으로 식별)
	@test -n "$(TOKEN)" || { echo "사용법: make meeting-join TOKEN=<shareToken> [MEMBER=uuid]"; exit 1; }
	@$(MCURL) $(or $(MEMBER),$(M2)) POST /meetings/$(TOKEN)/participants | jq .

meeting-invite: ## 약속 초대 (TOKEN=... INVITEE=uuid[,uuid...] [MEMBER=uuid], 기본 초대자 M1)
	@test -n "$(TOKEN)" && test -n "$(INVITEE)" || { echo "사용법: make meeting-invite TOKEN=<shareToken> INVITEE=<uuid[,uuid...]>"; exit 1; }
	@$(MCURL) $(or $(MEMBER),$(M1)) POST /meetings/$(TOKEN)/invitations \
	  '{"memberIds":["'$$(echo '$(INVITEE)' | sed 's/,/","/g')'"]}' | jq .

meeting-heatmap: ## 히트맵 조회 (TOKEN=... 필수)
	@test -n "$(TOKEN)" || { echo "사용법: make meeting-heatmap TOKEN=<shareToken>"; exit 1; }
	@$(MCURL) $(M1) GET /meetings/$(TOKEN)/heatmap | jq .

meeting-best-times: ## 최적 시간 조회 (TOKEN=... 필수)
	@test -n "$(TOKEN)" || { echo "사용법: make meeting-best-times TOKEN=<shareToken>"; exit 1; }
	@$(MCURL) $(M1) GET /meetings/$(TOKEN)/best-times | jq .

meeting-status: ## 응답 현황 조회 (TOKEN=... 필수)
	@test -n "$(TOKEN)" || { echo "사용법: make meeting-status TOKEN=<shareToken>"; exit 1; }
	@$(MCURL) $(M1) GET /meetings/$(TOKEN)/status | jq .

meeting-list: ## 내 약속 목록 ([MEMBER=uuid], 기본 M1 — 회원은 토큰으로 식별)
	@$(MCURL) $(or $(MEMBER),$(M1)) GET /meetings | jq .
