#!/usr/bin/env bash
# =====================================================================
# 테스트용 access token 발급 — 서버 JwtTokenProvider와 동일한 HS512 서명
#   사용:   ./jwt.sh <member-uuid> [유효기간초=600]
#   시크릿: $JWT_SECRET 필수 — 앱(bootRun)에 준 값과 같아야 서명이 통과한다.
#           (application.yml이 ${JWT_SECRET}만 참조하도록 바뀌어 기본값 없음)
#   클레임: sub=memberId, role=ROLE_USER (서버 파싱 규칙과 동일)
# =====================================================================
set -euo pipefail

SECRET="${JWT_SECRET:?JWT_SECRET 환경변수가 필요합니다 (앱 기동 시 준 값과 동일하게)}"
MEMBER_ID=${1:?사용법: jwt.sh <member-uuid> [유효기간초]}
TTL=${2:-600}

b64url() { openssl base64 -A | tr '+/' '-_' | tr -d '='; }

now=$(date +%s)
header=$(printf '{"alg":"HS512"}' | b64url)
payload=$(printf '{"sub":"%s","role":"ROLE_USER","iat":%d,"exp":%d}' "$MEMBER_ID" "$now" $((now + TTL)) | b64url)
sig=$(printf '%s.%s' "$header" "$payload" | openssl dgst -binary -sha512 -hmac "$SECRET" | b64url)
printf '%s.%s.%s\n' "$header" "$payload" "$sig"
