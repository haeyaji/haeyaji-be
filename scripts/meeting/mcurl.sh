#!/usr/bin/env bash
# =====================================================================
# 인증(JWT)·CSRF(더블서브밋) 처리된 meeting API 호출 래퍼
#   사용: ./mcurl.sh <member-uuid|-> <METHOD> <PATH> [JSON_BODY]
#         '-' 는 토큰 없이 호출(비인증 케이스 검증용)
#   환경: BASE_URL(기본 http://localhost:8090/api)
#         WITH_CODE=1 이면 마지막 줄에 http status code 추가 출력
#   CSRF: CookieCsrfTokenRepository 더블서브밋 방식이라 임의 값을
#         XSRF-TOKEN 쿠키 + X-XSRF-TOKEN 헤더에 동일하게 실으면 통과한다.
# =====================================================================
set -euo pipefail

HERE=$(cd "$(dirname "$0")" && pwd)
BASE_URL="${BASE_URL:-http://localhost:8090/api}"

MEMBER=${1:?사용법: mcurl.sh <member-uuid|-> <METHOD> <PATH> [JSON_BODY]}
METHOD=${2:?METHOD 필요}
REQ_PATH=${3:?PATH 필요}
DATA=${4:-}

args=(-s)
if [[ "${WITH_CODE:-}" == 1 ]]; then
  args+=(-w $'\n%{http_code}')
fi
if [[ "$MEMBER" != "-" ]]; then
  args+=(-H "Authorization: Bearer $("$HERE/jwt.sh" "$MEMBER")")
fi
case "$METHOD" in
  POST|PUT|PATCH|DELETE)
    xsrf="meeting-scripts-csrf"
    args+=(-H "Cookie: XSRF-TOKEN=$xsrf" -H "X-XSRF-TOKEN: $xsrf")
    ;;
esac
if [[ -n "$DATA" ]]; then
  args+=(-H 'Content-Type: application/json' -d "$DATA")
fi

curl "${args[@]}" -X "$METHOD" "$BASE_URL$REQ_PATH"
