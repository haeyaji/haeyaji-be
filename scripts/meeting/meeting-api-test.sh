#!/usr/bin/env bash
# =====================================================================
# meeting API 자동 검증 스크립트 (JWT 인증 + CSRF 더블서브밋 반영)
#   - 사전 조건: 앱 기동(make run), MySQL·Redis 기동(make db-up), member 시드(make seed)
#   - 사용:      make api-test  또는  ./meeting-api-test.sh
#   - 인증:      jwt.sh로 서버와 같은 시크릿(HS512)의 테스트 토큰을 직접 발급해 사용.
#                요청 회원은 이제 body가 아니라 토큰(sub)으로 식별된다.
#   - 종료 코드: 실패 케이스가 하나라도 있으면 1
# =====================================================================
set -uo pipefail

HERE=$(cd "$(dirname "$0")" && pwd)
BASE_URL="${BASE_URL:-http://localhost:8090/api}"
M1="${M1:-11111111-1111-7111-8111-111111111111}"   # 방장
M2="${M2:-22222222-2222-7222-8222-222222222222}"   # 참여자
M3="${M3:-33333333-3333-7333-8333-333333333333}"   # 미참여자 (시드 불필요 — 참여자 검사에서 403)
D1=$(date -d '+7 days' '+%Y-%m-%d')
D2=$(date -d '+9 days' '+%Y-%m-%d')
PAST=$(date -d '-1 day' '+%Y-%m-%d')
FAR=$(date -d '+3 months' '+%Y-%m-%d')   # 2개월 한도 초과

PASS=0; FAIL=0
BODY=""; CODE=""

# req MEMBER METHOD PATH [JSON_BODY] — mcurl.sh 경유, BODY/CODE 전역에 결과 저장. MEMBER '-'는 무인증
req() {
  local out
  out=$(WITH_CODE=1 "$HERE/mcurl.sh" "$1" "$2" "$3" "${4:-}")
  CODE=$(tail -1 <<<"$out")
  BODY=$(sed '$d' <<<"$out")
}

check() { # check "설명" 조건식...
  local desc=$1; shift
  if "$@"; then
    PASS=$((PASS+1)); printf '  \033[32mPASS\033[0m %s\n' "$desc"
  else
    FAIL=$((FAIL+1)); printf '  \033[31mFAIL\033[0m %s  (http=%s body=%.200s)\n' "$desc" "$CODE" "$BODY"
  fi
}

expect_status() { check "$1" test "$CODE" = "$2"; }
json() { jq -r "$1" <<<"$BODY"; }

command -v jq >/dev/null || { echo "jq가 필요합니다 (dnf install jq)"; exit 2; }

echo "== 0. 헬스/인증 체크 =="
req "$M1" GET /meetings
[[ "$CODE" == 200 ]] || { echo "앱이 응답하지 않거나 인증 실패: $BASE_URL (http=$CODE, make run 후 재시도)"; exit 2; }
echo "  OK ($BASE_URL)"
req - GET /meetings
expect_status "토큰 없이 → 401" 401
CODE=$(curl -s -o /dev/null -w '%{http_code}' -X POST "$BASE_URL/meetings" \
  -H "Authorization: Bearer $("$HERE/jwt.sh" "$M1")" \
  -H 'Content-Type: application/json' -d '{"title":"t"}')
BODY="(CSRF 검증용 직접 호출)"
check "CSRF 헤더 없이 POST → 403" test "$CODE" = 403

echo "== 1. 약속 생성 (MEET-1/2/13/14) =="
req "$M1" POST /meetings '{"title":"API 테스트","type":"TEAM","dates":["'$D1'","'$D2'"],"timeStart":"09:00","timeEnd":"12:00","slotUnitMinutes":30}'
expect_status "생성 201" 201
TOKEN=$(json '.data.shareToken')
check "creatorId = 토큰의 회원" test "$(json '.data.creatorId')" = "$M1"
check "슬롯 12개 생성 (2일 x 6칸)" test "$(json '.data.slots | length')" = 12
check "방장 자동 참여" test "$(json '.data.participants | length')" = 1
check "shareToken 발급 (43자)" test "${#TOKEN}" = 43
check "상태 COLLECTING" test "$(json '.data.status')" = COLLECTING
S1=$(json '.data.slots[0].id'); S2=$(json '.data.slots[1].id')
S3=$(json '.data.slots[2].id'); S4=$(json '.data.slots[3].id')

echo "== 2. 생성 검증 실패 케이스 =="
req "$M1" POST /meetings '{"title":"t","type":"CASUAL","dates":["'$D1'"],"timeStart":"09:00","timeEnd":"12:00","slotUnitMinutes":45}'
expect_status "슬롯 단위 45 → 400" 400
req "$M1" POST /meetings '{"title":"t","type":"CASUAL","dates":["'$D1'"],"timeStart":"09:10","timeEnd":"12:00","slotUnitMinutes":30}'
expect_status "미정렬 09:10 → 400" 400
req "$M1" POST /meetings '{"title":"t","type":"CASUAL","dates":["'$D1'"],"timeStart":"09:00","timeEnd":"09:00","slotUnitMinutes":30}'
expect_status "start==end → 400" 400
req "$M1" POST /meetings '{"title":"t","type":"CASUAL","dates":["'$D1'","'$D1'"],"timeStart":"09:00","timeEnd":"12:00","slotUnitMinutes":30}'
expect_status "중복 날짜 → 400" 400
req "$M1" POST /meetings '{"title":"t","type":"CASUAL","dates":["'$PAST'"],"timeStart":"09:00","timeEnd":"12:00","slotUnitMinutes":30}'
expect_status "과거 날짜 → 400" 400
req "$M1" POST /meetings '{"title":"t","type":"CASUAL","dates":["'$FAR'"],"timeStart":"09:00","timeEnd":"12:00","slotUnitMinutes":30}'
expect_status "2개월 초과 날짜 → 400" 400

echo "== 3. 자정 넘김 그리드 =="
req "$M1" POST /meetings '{"title":"자정","type":"ETC","dates":["'$D1'"],"timeStart":"23:00","timeEnd":"01:00","slotUnitMinutes":30}'
expect_status "23:00~01:00 생성 201" 201
check "익일 00:00 슬롯 존재" test "$(json '[.data.slots[].slotStartAt] | map(select(test("T00:00"))) | length')" = 1

echo "== 4. 참여 (MEET-4) =="
req "$M2" POST "/meetings/$TOKEN/participants"
expect_status "참여 201" 201
P_ID=$(json '.data.id')
check "memberId = 토큰의 회원" test "$(json '.data.memberId')" = "$M2"
req "$M2" POST "/meetings/$TOKEN/participants"
check "재참여 멱등 (같은 participant)" test "$(json '.data.id')" = "$P_ID"
req "$M1" GET "/meetings/does-not-exist"
expect_status "없는 토큰 → 404" 404

echo "== 4-1. 초대 (noti 연계) =="
req "$M1" POST "/meetings/$TOKEN/invitations" '{"memberIds":["'$M2'","'$M3'","'$M3'"]}'
expect_status "초대 201" 201
check "미참여자만 초대 (M3)" test "$(json '.data.invitedMemberIds | join(",")')" = "$M3"
check "기참여자는 스킵 (M2)" test "$(json '.data.skippedMemberIds | join(",")')" = "$M2"
req "$M3" POST "/meetings/$TOKEN/invitations" '{"memberIds":["'$M1'"]}'
expect_status "미참여자의 초대 → 403" 403
req "$M1" POST "/meetings/$TOKEN/invitations" '{"memberIds":[]}'
expect_status "빈 목록 → 400" 400

echo "== 5. 시간 응답 (MEET-5) =="
req "$M1" PUT "/meetings/$TOKEN/responses" '{"responses":[{"slotId":"'$S1'","status":"FREE"},{"slotId":"'$S2'","status":"FREE"},{"slotId":"'$S3'","status":"FREE"}]}'
expect_status "M1 제출 200" 200
req "$M2" PUT "/meetings/$TOKEN/responses" '{"responses":[{"slotId":"'$S2'","status":"FREE"},{"slotId":"'$S3'","status":"FREE"},{"slotId":"'$S4'","status":"BUSY"}]}'
expect_status "M2 제출 200" 200
req "$M3" PUT "/meetings/$TOKEN/responses" '{"responses":[]}'
expect_status "미참여자 → 403" 403
req "$M2" PUT "/meetings/$TOKEN/responses" '{"responses":[{"slotId":"99999999-9999-7999-8999-999999999999","status":"FREE"}]}'
expect_status "없는 슬롯 → 400" 400

echo "== 6. 집계 (MEET-6/7/8) =="
req "$M1" GET "/meetings/$TOKEN/heatmap"
expect_status "히트맵 200" 200
check "참여자 2명" test "$(json '.data.participantCount')" = 2
check "2번째 칸 freeCount=2" test "$(json '.data.cells[1].freeCount')" = 2
check "4번째 칸 BUSY 미집계 (0)" test "$(json '.data.cells[3].freeCount')" = 0
req "$M1" GET "/meetings/$TOKEN/best-times"
check "최적 구간 max=2" test "$(json '.data.maxFreeCount')" = 2
check "최적 구간 1개 (09:30~10:30)" test "$(json '.data.windows | length')" = 1
req "$M1" GET "/meetings/$TOKEN/status"
check "응답 현황 respondedCount=2" test "$(json '.data.respondedCount')" = 2

echo "== 7. 재수정 full-replace =="
req "$M2" PUT "/meetings/$TOKEN/responses" '{"responses":[{"slotId":"'$S3'","status":"FREE"}]}'
expect_status "M2 재제출 200" 200
req "$M1" GET "/meetings/$TOKEN/status"
check "M2 응답 1건으로 교체" test "$(json '.data.participants[] | select(.memberId=="'$M2'") | .responses | length')" = 1

echo "== 8. 확정 (MEET-9) =="
CONFIRM_S="${D1}T09:30:00"; CONFIRM_E="${D1}T10:30:00"
req "$M2" PATCH "/meetings/$TOKEN/confirm" '{"confirmedStartAt":"'$CONFIRM_S'","confirmedEndAt":"'$CONFIRM_E'"}'
expect_status "생성자 아님 → 403" 403
req "$M1" PATCH "/meetings/$TOKEN/confirm" '{"confirmedStartAt":"'${D1}'T09:45:00","confirmedEndAt":"'$CONFIRM_E'"}'
expect_status "미정렬 범위 → 400" 400
req "$M1" PATCH "/meetings/$TOKEN/confirm" '{"confirmedStartAt":"'${D1}'T13:00:00","confirmedEndAt":"'${D1}'T14:00:00"}'
expect_status "슬롯 밖 범위 → 400" 400
req "$M1" PATCH "/meetings/$TOKEN/confirm" '{"confirmedStartAt":"'$CONFIRM_S'","confirmedEndAt":"'$CONFIRM_E'"}'
expect_status "정상 확정 200" 200
check "상태 CONFIRMED" test "$(json '.data.status')" = CONFIRMED
req "$M1" PATCH "/meetings/$TOKEN/confirm" '{"confirmedStartAt":"'$CONFIRM_S'","confirmedEndAt":"'$CONFIRM_E'"}'
expect_status "재확정 → 409" 409
req "$M2" POST "/meetings/$TOKEN/participants"
expect_status "확정 후 참여 → 409" 409
req "$M1" POST "/meetings/$TOKEN/invitations" '{"memberIds":["'$M3'"]}'
expect_status "확정 후 초대 → 409" 409

echo "== 9. 목록 (MEET-11) =="
req "$M2" GET /meetings
expect_status "목록 200" 200
check "M2 참여 약속 포함" test "$(json '[.data[] | select(.shareToken=="'$TOKEN'")] | length')" = 1

echo "== 10. 만료 정책 (MEET-12) =="
DEADLINE=$(date -d '+3 seconds' '+%Y-%m-%dT%H:%M:%S')
req "$M1" POST /meetings '{"title":"만료","type":"CASUAL","dates":["'$D1'"],"timeStart":"10:00","timeEnd":"11:00","slotUnitMinutes":30,"deadline":"'$DEADLINE'"}'
expect_status "마감 있는 약속 생성 201" 201
EXP_TOKEN=$(json '.data.shareToken')
sleep 5
req "$M1" GET "/meetings/$EXP_TOKEN"
check "마감 후 조회 시 EXPIRED (lazy)" test "$(json '.data.status')" = EXPIRED
req "$M2" POST "/meetings/$EXP_TOKEN/participants"
expect_status "만료 후 참여 → 410" 410
req "$M1" POST "/meetings/$EXP_TOKEN/invitations" '{"memberIds":["'$M3'"]}'
expect_status "만료 후 초대 → 410" 410
req "$M1" PATCH "/meetings/$EXP_TOKEN/confirm" '{"confirmedStartAt":"'${D1}'T10:00:00","confirmedEndAt":"'${D1}'T10:30:00"}'
expect_status "만료 후 확정 → 410" 410

echo
echo "=================================="
echo " 결과: PASS=$PASS FAIL=$FAIL"
echo "=================================="
[[ $FAIL -eq 0 ]]
