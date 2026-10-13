#!/usr/bin/env bash
# Builds a static preview of the public website into docs/ for GitHub Pages.
# The app must be running locally first (http://localhost:8080).
# Booking buttons become WhatsApp links, because online booking needs the server.
set -euo pipefail

BASE="${BASE_URL:-http://localhost:8080}"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
OUT="$ROOT/docs"
WHATSAPP="https://wa.me/918374661394?text=Hi%2C%20I%20would%20like%20to%20book%20a%20room%20at%20iRose%20Olive%20Business%20Hotel."

rm -rf "$OUT"
mkdir -p "$OUT/css"
cp -r "$ROOT/src/main/resources/static/images" "$OUT/images"
rm -f "$OUT/images/README.txt"
touch "$OUT/.nojekyll"

# Stylesheet: image paths become relative to docs/css/.
sed 's#url("/images/#url("../images/#g' "$ROOT/src/main/resources/static/css/style.css" > "$OUT/css/style.css"

fetch() {
  local path="$1" file="$2"
  curl -fsS "$BASE$path" \
    | sed -E \
        -e "s#href=\"/book[^\"]*\"#href=\"$WHATSAPP\" target=\"_blank\" rel=\"noopener\"#g" \
        -e '/href="\/login"/d' \
        -e '/[0-9]+ available<\/span>/d' \
        -e 's#href="/css/#href="css/#g' \
        -e 's#src="/images/#src="images/#g' \
        -e "s#'/images/#'images/#g" \
        -e 's#href="/\#([a-z]+)"#href="index.html\#\1"#g' \
        -e 's#href="/"#href="index.html"#g' \
        -e 's#(href|action)="/rooms"#\1="rooms.html"#g' \
    > "$OUT/$file"
}

fetch "/" index.html
fetch "/rooms" rooms.html

# Rooms page: drop the live date-search form (it needs the server).
sed -i '/<form class="row g-2 align-items-end"/,/<\/form>/d' "$OUT/rooms.html"

# Fail loudly if any server-only link survived.
if grep -nE '(href|action|src)="/[a-z]' "$OUT"/*.html; then
  echo "ERROR: server-relative links remain (listed above)" >&2
  exit 1
fi
echo "Static preview written to $OUT"
