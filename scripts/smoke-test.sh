#!/usr/bin/env bash
# ============================================================
# Smoke test suite for myCQRS demo app
# Usage: ./scripts/smoke-test.sh [BASE_URL]
# Default: http://localhost:8080
# ============================================================
set -euo pipefail

BASE_URL="${1:-http://localhost:8080}"
PASS=0
FAIL=0
UNIQUE_TITLE="SmokeTest-$(date +%s)"

gen_uuid() {
  if command -v uuidgen >/dev/null 2>&1; then
    uuidgen | tr '[:upper:]' '[:lower:]'
  elif [ -r /proc/sys/kernel/random/uuid ]; then
    cat /proc/sys/kernel/random/uuid
  else
    python3 -c 'import uuid; print(uuid.uuid4())'
  fi
}

BOOK_ID=$(gen_uuid)

pass() { PASS=$((PASS + 1)); echo "  PASS: $1"; }
fail() { FAIL=$((FAIL + 1)); echo "  FAIL: $1 — $2"; }

echo "=== myCQRS Smoke Tests ==="
echo "Target:  $BASE_URL"
echo "Book ID: $BOOK_ID"
echo ""

# ----------------------------------------------------------
# 1. App is alive
# ----------------------------------------------------------
echo "[1/7] App starts without errors"
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/books/$(gen_uuid)" 2>/dev/null || echo "000")
if [ "$HTTP_CODE" != "000" ]; then
  pass "App responding (HTTP $HTTP_CODE)"
else
  fail "App not responding" "Is spring-boot:run running?"
  echo ""
  echo "RESULT: $PASS passed, $FAIL failed"
  exit 1
fi

# ----------------------------------------------------------
# 2. PUT /books/{id} — create
# ----------------------------------------------------------
echo "[2/7] PUT /books/{id} creates a book"
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X PUT "$BASE_URL/books/$BOOK_ID" \
  -H "Content-Type: application/json" \
  -d "{\"title\":\"$UNIQUE_TITLE\",\"author\":\"Smoke Author\"}")
if [ "$HTTP_CODE" = "201" ]; then
  pass "PUT /books/$BOOK_ID -> $HTTP_CODE"
else
  fail "PUT /books/$BOOK_ID -> $HTTP_CODE" "expected 201"
fi

# ----------------------------------------------------------
# 3. GET /books?title= — read
# ----------------------------------------------------------
echo "[3/7] GET /books?title= returns created book"
ENCODED_TITLE=$(printf '%s' "$UNIQUE_TITLE" | sed 's/ /%20/g')
BODY=$(curl -s "$BASE_URL/books?title=$ENCODED_TITLE")
if echo "$BODY" | grep -q "$UNIQUE_TITLE"; then
  pass "GET /books?title=$UNIQUE_TITLE returned book"
else
  fail "GET /books?title=$UNIQUE_TITLE" "body: $BODY"
fi

# ----------------------------------------------------------
# 4. PATCH /books/{id} — update
# ----------------------------------------------------------
echo "[4/7] PATCH /books/{id} updates book on write-side"
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X PATCH "$BASE_URL/books/$BOOK_ID" \
  -H "Content-Type: application/json" \
  -d "{\"title\":\"${UNIQUE_TITLE}-Updated\",\"author\":\"Smoke Author Updated\"}")
if [ "$HTTP_CODE" = "200" ]; then
  pass "PATCH /books/$BOOK_ID -> $HTTP_CODE"
else
  fail "PATCH /books/$BOOK_ID -> $HTTP_CODE" "expected 200"
fi

# ----------------------------------------------------------
# 5. DELETE /books/{id} — soft delete
# ----------------------------------------------------------
echo "[5/7] DELETE /books/{id} sets deleted=true"
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X DELETE "$BASE_URL/books/$BOOK_ID")
if [ "$HTTP_CODE" = "204" ]; then
  DELETED=$(docker exec mycqrs-postgres psql -U postgres -d mycqrsdb -t -A \
    -c "SELECT deleted FROM book_entity WHERE id='$BOOK_ID';" 2>/dev/null | tr -d '[:space:]')
  if [ "$DELETED" = "t" ]; then
    pass "DELETE /books/$BOOK_ID -> 204, deleted=true in DB"
  else
    fail "DELETE /books/$BOOK_ID -> 204 but deleted=$DELETED" "expected deleted=true"
  fi
else
  fail "DELETE /books/$BOOK_ID -> $HTTP_CODE" "expected 204"
fi

# ----------------------------------------------------------
# 6. Double delete — should fail
# ----------------------------------------------------------
echo "[6/7] Double delete fails with business error"
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X DELETE "$BASE_URL/books/$BOOK_ID")
if [ "$HTTP_CODE" = "500" ] || [ "$HTTP_CODE" = "409" ] || [ "$HTTP_CODE" = "400" ]; then
  pass "Double DELETE /books/$BOOK_ID -> $HTTP_CODE (business error)"
else
  fail "Double DELETE /books/$BOOK_ID -> $HTTP_CODE" "expected 4xx/5xx error"
fi

# ----------------------------------------------------------
# 7. Update after delete — should fail
# ----------------------------------------------------------
echo "[7/7] Update after delete fails with business error"
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X PATCH "$BASE_URL/books/$BOOK_ID" \
  -H "Content-Type: application/json" \
  -d '{"title":"Should Fail","author":"No"}')
if [ "$HTTP_CODE" = "500" ] || [ "$HTTP_CODE" = "409" ] || [ "$HTTP_CODE" = "400" ]; then
  pass "PATCH after DELETE /books/$BOOK_ID -> $HTTP_CODE (business error)"
else
  fail "PATCH after DELETE /books/$BOOK_ID -> $HTTP_CODE" "expected 4xx/5xx error"
fi

# ----------------------------------------------------------
# Cleanup: remove test data
# ----------------------------------------------------------
docker exec mycqrs-postgres psql -U postgres -d mycqrsdb -q \
  -c "DELETE FROM book_entity WHERE title LIKE 'SmokeTest-%';" 2>/dev/null

# ----------------------------------------------------------
# Summary
# ----------------------------------------------------------
echo ""
echo "==============================="
echo "RESULT: $PASS passed, $FAIL failed"
echo "==============================="

[ "$FAIL" -eq 0 ] && exit 0 || exit 1
