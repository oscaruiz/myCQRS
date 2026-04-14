#!/usr/bin/env bash
set -euo pipefail

FAILED=0

check_imports() {
    local rule_name="$1"
    local source_dir="$2"
    local forbidden_regex="$3"

    if [ ! -d "$source_dir" ]; then
        echo "SKIP: $rule_name (directory not found: $source_dir)"
        return 0
    fi

    local violations
    violations=$(find "$source_dir" -type f -name '*.java' -exec grep -HEn "^[[:space:]]*import[[:space:]]+${forbidden_regex}" {} + 2>/dev/null || true)

    if [ -z "$violations" ]; then
        echo "PASS: $rule_name"
        return 0
    fi

    echo "FAIL: $rule_name"
    while IFS=: read -r file lineno rest; do
        echo "  ${file}:${lineno}"
        echo "    ${rest}"
    done <<< "$violations"
    FAILED=$((FAILED + 1))
}

echo "Running package dependency checks..."
echo ""

check_imports "contracts-no-infra-or-spring" \
    "src/core/src/main/java/com/oscaruiz/mycqrs/core/contracts/" \
    "com\.oscaruiz\.mycqrs\.core\.(infrastructure|spring)"

check_imports "infra-no-spring" \
    "src/core/src/main/java/com/oscaruiz/mycqrs/core/infrastructure/" \
    "com\.oscaruiz\.mycqrs\.core\.spring"

check_imports "ddd-no-infra-or-spring" \
    "src/core/src/main/java/com/oscaruiz/mycqrs/core/ddd/" \
    "com\.oscaruiz\.mycqrs\.core\.(infrastructure|spring)"

echo ""
echo "Result: ${FAILED} failure(s)"

if [ "$FAILED" -gt 0 ]; then
    exit 1
fi
