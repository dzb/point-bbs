#!/usr/bin/env bash
# End-to-end API smoke test against a running point-bbs instance.
# Usage: bash tools/smoke-api.sh [base-url]   (default http://localhost:8082)
set -euo pipefail

BASE="${1:-http://localhost:8082}"
PASS=0; FAIL=0

check() { # check <desc> <expected-code> <actual-code>
  if [ "$2" = "$3" ]; then PASS=$((PASS+1)); echo "ok   $1"
  else FAIL=$((FAIL+1)); echo "FAIL $1 (expected $2, got $3)"; fi
}

echo "== smoke: $BASE =="
curl -sf -o /dev/null "$BASE/healthz" && check "healthz" 200 200 || { echo "FAIL server not reachable"; exit 1; }

CODE=$(curl -s -o /dev/null -w "%{http_code}" "$BASE/api/topics?page=1")
check "GET /api/topics" 200 "$CODE"

CODE=$(curl -s -o /dev/null -w "%{http_code}" "$BASE/api/topics/moments?page=1")
check "GET /api/topics/moments" 200 "$CODE"

CODE=$(curl -s -o /dev/null -w "%{http_code}" "$BASE/api/topics/search?q=%E4%B9%A6")
check "GET /api/topics/search" 200 "$CODE"

CODE=$(curl -s -o /dev/null -w "%{http_code}" "$BASE/api/topics/1")
check "GET /api/topics/1" 200 "$CODE"

CODE=$(curl -s -o /dev/null -w "%{http_code}" "$BASE/api/articles")
check "GET /api/articles" 200 "$CODE"

CODE=$(curl -s -o /dev/null -w "%{http_code}" "$BASE/api/categories")
check "GET /api/categories" 200 "$CODE"

CODE=$(curl -s -o /dev/null -w "%{http_code}" "$BASE/api/users/1")
check "GET /api/users/1" 200 "$CODE"

# auth: signin must work and carry a token
SIGNIN=$(curl -s -X POST "$BASE/api/auth/signin" -H "Content-Type: application/json" \
  -d '{"loginName":"moke","password":"123456"}')
TOKEN=$(echo "$SIGNIN" | python3 -c "import sys,json;d=json.load(sys.stdin);print(d['data']['token'] if d['code']==0 else '')" 2>/dev/null || true)
if [ -n "$TOKEN" ]; then
  PASS=$((PASS+1)); echo "ok   signin (token issued)"
else
  FAIL=$((FAIL+1)); echo "FAIL signin"
fi

CODE=$(curl -s -o /dev/null -w "%{http_code}" "$BASE/api/users/current" -H "Authorization: Bearer $TOKEN")
check "GET /api/users/current (auth)" 200 "$CODE"

CODE=$(curl -s -o /dev/null -w "%{http_code}" "$BASE/api/topics/following" -H "Authorization: Bearer $TOKEN")
check "GET /api/topics/following (auth)" 200 "$CODE"

# auth required: no token → 401 (current-user endpoint is 200+null by design)
CODE=$(curl -s -o /dev/null -w "%{http_code}" "$BASE/api/topics/following")
check "GET /api/topics/following (anon → 401)" 401 "$CODE"

# current user without token → 200 with null payload
BODY=$(curl -s "$BASE/api/users/current")
echo "$BODY" | grep -q '"data":null' && { PASS=$((PASS+1)); echo "ok   current user anon → null"; } || { FAIL=$((FAIL+1)); echo "FAIL current user anon (got $BODY)"; }

# admin RBAC: anon/moke-without-admin-role → 403 (moke IS admin in dev seed, use a fresh user)
CODE=$(curl -s -o /dev/null -w "%{http_code}" "$BASE/api/admin/topic")
check "GET /api/admin/topic (anon → 403)" 403 "$CODE"

# security headers present
HDR=$(curl -s -D - -o /dev/null "$BASE/" | grep -ci "content-security-policy" || true)
[ "$HDR" -ge 1 ] && { PASS=$((PASS+1)); echo "ok   CSP header"; } || { FAIL=$((FAIL+1)); echo "FAIL CSP header"; }

# XSS payload must not survive markdown grid rendering is frontend-side; here verify upload rejects SVG
if [ -n "$TOKEN" ]; then
  CODE=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE/api/upload" \
    -H "Authorization: Bearer $TOKEN" -H "Content-Type: text/plain" \
    -d 'data:image/svg+xml;base64,PHN2Zy8+')
  check "POST /api/upload (svg rejected → 400)" 400 "$CODE"
fi

echo
echo "== result: $PASS passed, $FAIL failed =="
[ "$FAIL" -eq 0 ]
