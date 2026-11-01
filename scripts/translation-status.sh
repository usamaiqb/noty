#!/usr/bin/env bash
# Compute per-locale translation completion and update/check the README table.
#
# Usage:
#   bash scripts/translation-status.sh           # rewrite the table in README.md
#   bash scripts/translation-status.sh --check   # exit 1 if the table is stale
#
# Requires: bash, coreutils, grep, sed, awk, and a JDK 11+ on PATH (the one that builds
# the app). Language names come from scripts/LanguageNames.java, so a new values-<locale>
# folder needs no change here.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
RES_DIR="$REPO_ROOT/app/src/main/res"
SOURCE_XML="$RES_DIR/values/strings.xml"
README="$REPO_ROOT/README.md"

START_MARKER="<!-- translations:start -->"
END_MARKER="<!-- translations:end -->"

# Percentage -> 12-char Unicode block progress bar (e.g. ███████████░)
progress_bar() {
    awk -v p="$1" 'BEGIN {
        w = 12
        f = int(p * w / 100 + 0.5)
        bar = ""
        for (i = 0; i < f; i++) bar = bar "█"
        for (i = f; i < w; i++) bar = bar "░"
        print bar
    }'
}

# Sorted translatable key names from the source strings.xml
source_keys() {
    tr -d '\r' < "$SOURCE_XML" \
        | grep '<string name=' \
        | grep -v 'translatable="false"' \
        | grep -o 'name="[^"]*"' \
        | sort
}

# Sorted key names from a locale strings.xml (path as $1)
locale_keys() {
    tr -d '\r' < "$1" \
        | grep '<string name=' \
        | grep -o 'name="[^"]*"' \
        | sort
}

# Resource qualifiers of every translated locale (de, zh-rCN, ...). values-night and
# other non-locale folders carry no strings.xml, so they drop out here.
locale_qualifiers() {
    for f in "$RES_DIR"/values-*/strings.xml; do
        [[ -e "$f" ]] || continue
        basename "$(dirname "$f")" | sed 's/^values-//'
    done | sort
}

# The source language's own name, from res/resources.properties (en-US -> en -> English)
source_qualifier() {
    tr -d '\r' < "$RES_DIR/resources.properties" \
        | sed -n 's/^unqualifiedResLocale=\([a-z]*\).*/\1/p'
}

# Print the Markdown table to stdout
compute_table() {
    local src_keys total src names
    src_keys="$(source_keys)"
    total="$(echo "$src_keys" | wc -l | tr -d '[:space:]')"
    src="$(source_qualifier)"

    # One JVM start for every name rather than one per locale.
    # shellcheck disable=SC2046
    names="$(java "$SCRIPT_DIR/LanguageNames.java" "$src" $(locale_qualifiers) | tr -d '\r')"

    echo "| Language | Progress |"
    echo "| --- | --- |"
    echo "| $(echo "$names" | awk -F'\t' -v q="$src" '$1 == q { print $2 }') | $(progress_bar 100) 100% (source) |"

    local code matched pct name
    for code in $(locale_qualifiers); do
        matched="$(comm -12 <(echo "$src_keys") <(locale_keys "$RES_DIR/values-$code/strings.xml") \
            | wc -l | tr -d '[:space:]')"
        pct="$(awk -v m="$matched" -v t="$total" 'BEGIN { printf "%d", int(m * 100 / t + 0.5) }')"
        name="$(echo "$names" | awk -F'\t' -v q="$code" '$1 == q { print $2 }')"
        echo "| $name | $(progress_bar "$pct") ${pct}% |"
    done
}

generate_block() {
    echo "$START_MARKER"
    compute_table
    echo "$END_MARKER"
}

# ── main ──────────────────────────────────────────────────────────────────────

if ! grep -q "$START_MARKER" "$README"; then
    echo "ERROR: README.md is missing the $START_MARKER marker."
    exit 1
fi

new="$(generate_block)"
current="$(tr -d '\r' < "$README" | awk -v s="$START_MARKER" -v e="$END_MARKER" '$0 == s, $0 == e')"

if [[ "${1:-}" == "--check" ]]; then
    if [[ "$current" == "$new" ]]; then
        echo "Translation table is up to date."
        exit 0
    fi
    echo "ERROR: README translation table is out of date."
    echo "Run: bash scripts/translation-status.sh"
    echo ""
    diff <(echo "$current") <(echo "$new") || true
    exit 1
fi

if [[ "$current" == "$new" ]]; then
    echo "README.md: translation table already up to date."
    exit 0
fi

# Splice the new block between the markers, keeping the file's own line endings so a
# Windows checkout (CRLF) does not come back with every line changed.
eol=""
# Counted with tr rather than grep: Git Bash's grep strips the \r before matching.
if [[ "$(tr -dc '\r' < "$README" | wc -c)" -gt 0 ]]; then eol=$'\r'; fi
block_file="$(mktemp)"
echo "$new" > "$block_file"
tr -d '\r' < "$README" \
    | awk -v s="$START_MARKER" -v e="$END_MARKER" -v blk="$block_file" '
        $0 == s { while ((getline line < blk) > 0) print line; skip = 1; next }
        $0 == e { skip = 0; next }
        !skip { print }
    ' \
    | sed "s/\$/$eol/" > "$README.tmp"
mv "$README.tmp" "$README"
rm -f "$block_file"
echo "README.md: translation table updated."
