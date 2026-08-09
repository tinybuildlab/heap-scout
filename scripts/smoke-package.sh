#!/usr/bin/env bash

set -euo pipefail

if [[ $# -lt 2 || $# -gt 3 ]]; then
  echo "Usage: $0 <launcher> <legal-directory> [port]" >&2
  exit 2
fi

smoke_launcher="$1"
smoke_legal_directory="$2"
smoke_port="${3:-18911}"
smoke_process_id=""
smoke_directory=""
smoke_log=""

print_log() {
  if [[ -n "$smoke_log" && -f "$smoke_log" ]]; then
    echo "HeapScout package log:" >&2
    sed -n '1,240p' "$smoke_log" >&2
  fi
}

cleanup() {
  if [[ -n "$smoke_process_id" ]] && kill -0 "$smoke_process_id" 2>/dev/null; then
    kill "$smoke_process_id" 2>/dev/null || true
    wait "$smoke_process_id" 2>/dev/null || true
  fi
  if [[ -n "$smoke_directory" && -d "$smoke_directory" ]]; then
    rm -rf -- "$smoke_directory"
  fi
}

fail() {
  echo "Package smoke test failed: $1" >&2
  print_log
  exit 1
}

trap cleanup EXIT INT TERM

[[ -x "$smoke_launcher" ]] || fail "launcher is missing or not executable: $smoke_launcher"
[[ -f "$smoke_legal_directory/LICENSE" ]] || fail "LICENSE is missing from the application image"
[[ -f "$smoke_legal_directory/THIRD_PARTY_NOTICES.md" ]] || fail "THIRD_PARTY_NOTICES.md is missing from the application image"
[[ "$smoke_port" =~ ^[0-9]+$ ]] || fail "port must be numeric"
(( smoke_port >= 1024 && smoke_port <= 65535 )) || fail "port must be between 1024 and 65535"

smoke_directory="$(mktemp -d "${TMPDIR:-/tmp}/heapscout-package-smoke.XXXXXX")"
smoke_log="$smoke_directory/heapscout.log"
smoke_jobs_response="$smoke_directory/jobs.json"
smoke_ui_response="$smoke_directory/index.html"
smoke_base_url="http://127.0.0.1:$smoke_port"

if curl --silent --fail --max-time 1 "$smoke_base_url/api/dumps" >/dev/null 2>&1; then
  fail "port $smoke_port is already serving HTTP"
fi

"$smoke_launcher" \
  --heapscout.open-browser=false \
  --server.port="$smoke_port" \
  >"$smoke_log" 2>&1 &
smoke_process_id="$!"

smoke_ready=false
for ((smoke_attempt = 1; smoke_attempt <= 60; smoke_attempt++)); do
  if ! kill -0 "$smoke_process_id" 2>/dev/null; then
    wait "$smoke_process_id" 2>/dev/null || true
    fail "packaged process exited before the API became ready"
  fi

  smoke_status="$(
    curl \
      --silent \
      --output "$smoke_jobs_response" \
      --write-out '%{http_code}' \
      --max-time 2 \
      "$smoke_base_url/api/dumps" || true
  )"
  if [[ "$smoke_status" == "200" ]]; then
    smoke_ready=true
    break
  fi
  sleep 1
done

[[ "$smoke_ready" == "true" ]] || fail "API did not become ready within 60 seconds"
[[ "$(tr -d '[:space:]' < "$smoke_jobs_response")" == "[]" ]] || fail "fresh package returned an unexpected job list"

smoke_ui_status="$(
  curl \
    --silent \
    --output "$smoke_ui_response" \
    --write-out '%{http_code}' \
    --max-time 5 \
    "$smoke_base_url/" || true
)"
[[ "$smoke_ui_status" == "200" ]] || fail "bundled UI did not return HTTP 200"
grep -q '<title>HeapScout</title>' "$smoke_ui_response" || fail "bundled UI did not contain the HeapScout document title"

smoke_rebind_status="$(
  curl \
    --silent \
    --output /dev/null \
    --write-out '%{http_code}' \
    --header 'Host: attacker.example' \
    --max-time 5 \
    "$smoke_base_url/api/dumps" || true
)"
[[ "$smoke_rebind_status" == "421" ]] || fail "non-loopback Host was not rejected with HTTP 421"

kill "$smoke_process_id"
wait "$smoke_process_id" 2>/dev/null || true
smoke_process_id=""

echo "Package smoke test passed: $smoke_launcher"
