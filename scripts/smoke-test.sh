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

pass() { PASS=$((PASS + 1)); echo "  PASS: $1"; }
fail() { FAIL=$((FAIL + 1)); echo "  FAIL: $1 — $2"; }

echo "=== myCQRS Smoke Tests ==="
echo "Target: $BASE_URL"
echo ""

# ----------------------------------------------------------
# 1. App is alive
# ----------------------------------------------------------
echo "[1/7] App starts without errors"
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/books/nonexistent" 2>/dev/null || echo "000")
if [ "$HTTP_CODE" != "000" ]; then
  pass "App responding (HTTP $HTTP_CODE)"
else
  fail "App not responding" "Is spring-boot:run running?"
  echo ""
  echo "RESULT: $PASS passed, $FAIL failed"
  exit 1
fi

# ----------------------------------------------------------
# 2. POST /books — create
# ----------------------------------------------------------
echo "[2/7] POST /books creates a book"
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/books" \
  -H "Content-Type: application/json" \
  -d "{\"title\":\"$UNIQUE_TITLE\",\"author\":\"Smoke Author\"}")
if [ "$HTTP_CODE" = "200" ]; then
  pass "POST /books -> $HTTP_CODE"
else
  fail "POST /books -> $HTTP_CODE" "expected 200"
fi

# ----------------------------------------------------------
# 3. GET /books/{title} — read
# ----------------------------------------------------------
echo "[3/7] GET /books/{title} returns created book"
ENCODED_TITLE=$(printf '%s' "$UNIQUE_TITLE" | sed 's/ /%20/g')
BODY=$(curl -s "$BASE_URL/books/$ENCODED_TITLE")
if echo "$BODY" | grep -q "$UNIQUE_TITLE"; then
  pass "GET /books/$UNIQUE_TITLE returned book"
else
  fail "GET /books/$UNIQUE_TITLE" "body: $BODY"
fi

# ----------------------------------------------------------
# Get the book ID from the database for remaining tests
# ----------------------------------------------------------
BOOK_ID=$(docker exec mycqrs-postgres psql -U postgres -d mycqrsdb -t -A \
  -c "SELECT id FROM book_entity WHERE title='$UNIQUE_TITLE' LIMIT 1;" 2>/dev/null | tr -d '[:space:]')

if [ -z "$BOOK_ID" ]; then
  fail "Could not retrieve book ID from database" "skipping PUT/DELETE tests"
  echo ""
  echo "RESULT: $PASS passed, $FAIL failed"
  exit 1
fi
echo "       (book id=$BOOK_ID)"

# ----------------------------------------------------------
# 4. PUT /books/{id} — update
# ----------------------------------------------------------
echo "[4/7] PUT /books/{id} updates book on write-side"
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X PUT "$BASE_URL/books/$BOOK_ID" \
  -H "Content-Type: application/json" \
  -d "{\"title\":\"${UNIQUE_TITLE}-Updated\",\"author\":\"Smoke Author Updated\"}")
if [ "$HTTP_CODE" = "200" ]; then
  pass "PUT /books/$BOOK_ID -> $HTTP_CODE"
else
  fail "PUT /books/$BOOK_ID -> $HTTP_CODE" "expected 200"
fi

# ----------------------------------------------------------
# 5. DELETE /books/{id} — soft delete
# ----------------------------------------------------------
echo "[5/7] DELETE /books/{id} sets deleted=true"
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X DELETE "$BASE_URL/books/$BOOK_ID")
if [ "$HTTP_CODE" = "204" ]; then
  # Verify deleted=true in DB
  DELETED=$(docker exec mycqrs-postgres psql -U postgres -d mycqrsdb -t -A \
    -c "SELECT deleted FROM book_entity WHERE id=$BOOK_ID;" 2>/dev/null | tr -d '[:space:]')
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
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X PUT "$BASE_URL/books/$BOOK_ID" \
  -H "Content-Type: application/json" \
  -d '{"title":"Should Fail","author":"No"}')
if [ "$HTTP_CODE" = "500" ] || [ "$HTTP_CODE" = "409" ] || [ "$HTTP_CODE" = "400" ]; then
  pass "PUT after DELETE /books/$BOOK_ID -> $HTTP_CODE (business error)"
else
  fail "PUT after DELETE /books/$BOOK_ID -> $HTTP_CODE" "expected 4xx/5xx error"
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
