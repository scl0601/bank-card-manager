#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ENV_ID="${CLOUDBASE_ENV_ID:-dev-4g1sv3870175b971}"
CLOUD_RUN_SERVICE="${CLOUDBASE_CLOUD_RUN_SERVICE:-bank-admin-backend}"
CLOUD_RUN_PORT="${CLOUDBASE_CLOUD_RUN_PORT:-7878}"
FRONTEND_DIR="$PROJECT_ROOT/frontend"
BACKEND_DIR="$PROJECT_ROOT/backend"
FRONTEND_DIST="$FRONTEND_DIR/dist"
BACKEND_API_DOCS_URL="${BACKEND_API_DOCS_URL:-https://bank-admin-backend-239413-10-1411764939.sh.run.tcloudbase.com/v3/api-docs}"
FRONTEND_URLS=(
  "https://bankaiscl.top/"
  "https://dev-4g1sv3870175b971-1411764939.tcloudbaseapp.com/"
)
REQUIRED_BACKEND_FIELDS=(
  "topUserId"
  "topUserName"
  "creditCardCount"
  "debitCardCount"
)

step() {
  printf '\n==> %s\n' "$1"
}

require_command() {
  if ! command -v "$1" >/dev/null 2>&1; then
    printf 'Required command not found: %s\n' "$1" >&2
    exit 1
  fi
}

java_major_version() {
  java -version 2>&1 | awk -F '"' '/version/ {
    split($2, parts, ".")
    if (parts[1] == "1") print parts[2]; else print parts[1]
    exit
  }'
}

get_frontend_index_asset() {
  local url="$1"
  local cache_bust
  local html

  if [[ "$url" == *"?"* ]]; then
    cache_bust="${url}&v=$(date +%s)"
  else
    cache_bust="${url}?v=$(date +%s)"
  fi

  html="$(curl -fsSL -H 'Cache-Control: no-cache' -H 'Pragma: no-cache' "$cache_bust")"
  printf '%s' "$html" | grep -Eo 'assets/index-[A-Za-z0-9_-]+\.js' | head -n 1
}

test_backend_schema() {
  local url="${BACKEND_API_DOCS_URL}?ts=$(date +%s)000"
  local schema

  schema="$(curl -fsSL -H 'Cache-Control: no-cache' -H 'Pragma: no-cache' "$url")"
  for field in "${REQUIRED_BACKEND_FIELDS[@]}"; do
    if ! printf '%s' "$schema" | grep -q "\"$field\""; then
      return 1
    fi
  done
}

step "Checking project and tools"
[[ -d "$FRONTEND_DIR" ]] || { printf 'Frontend dir not found: %s\n' "$FRONTEND_DIR" >&2; exit 1; }
[[ -d "$BACKEND_DIR" ]] || { printf 'Backend dir not found: %s\n' "$BACKEND_DIR" >&2; exit 1; }
require_command node
require_command npm
require_command java
require_command mvn
require_command cloudbase
require_command curl
require_command grep

JAVA_MAJOR="$(java_major_version)"
if [[ -z "$JAVA_MAJOR" || "$JAVA_MAJOR" -lt 21 ]]; then
  printf 'Java 21+ is required. Current java version:\n' >&2
  java -version >&2
  exit 1
fi

printf 'Project: %s\n' "$PROJECT_ROOT"
printf 'CloudBase env: %s\n' "$ENV_ID"
printf 'CloudRun service: %s\n' "$CLOUD_RUN_SERVICE"
printf 'Node: %s\n' "$(command -v node)"
printf 'NPM: %s\n' "$(command -v npm)"
printf 'Maven: %s\n' "$(command -v mvn)"
printf 'Java: %s\n' "$(java -version 2>&1 | tr '\n' ' ' | sed 's/[[:space:]]\+/ /g')"

step "Building frontend"
(cd "$FRONTEND_DIR" && npm run build)

if [[ ! -f "$FRONTEND_DIST/index.html" ]]; then
  printf 'Frontend build output missing: %s/index.html\n' "$FRONTEND_DIST" >&2
  exit 1
fi

LOCAL_INDEX_ASSET="$(grep -Eo 'assets/index-[A-Za-z0-9_-]+\.js' "$FRONTEND_DIST/index.html" | head -n 1)"
if [[ -z "$LOCAL_INDEX_ASSET" ]]; then
  printf 'Could not find local frontend index asset after build.\n' >&2
  exit 1
fi
printf 'Local frontend index asset: %s\n' "$LOCAL_INDEX_ASSET"

step "Building backend"
(cd "$BACKEND_DIR" && mvn clean package -DskipTests)

if [[ ! -f "$BACKEND_DIR/target/bank-admin-1.0.0.jar" ]]; then
  printf 'Backend jar was not created.\n' >&2
  exit 1
fi

step "Deploying backend to CloudRun"
(cd "$BACKEND_DIR" && printf '\nY\n' | cloudbase cloudrun deploy -s "$CLOUD_RUN_SERVICE" --source . --port "$CLOUD_RUN_PORT" --force -e "$ENV_ID")

step "Refreshing static hosting assets"
(cd "$FRONTEND_DIR" && cloudbase hosting delete assets --dir -e "$ENV_ID" || true)

step "Deploying frontend static files"
(cd "$FRONTEND_DIR" && cloudbase hosting deploy dist / -e "$ENV_ID")

step "Verifying frontend domains"
for url in "${FRONTEND_URLS[@]}"; do
  ONLINE_INDEX_ASSET="$(get_frontend_index_asset "$url")"
  printf '%s -> %s\n' "$url" "$ONLINE_INDEX_ASSET"
  if [[ "$ONLINE_INDEX_ASSET" != "$LOCAL_INDEX_ASSET" ]]; then
    printf 'Frontend index asset mismatch for %s. Expected %s, got %s\n' "$url" "$LOCAL_INDEX_ASSET" "$ONLINE_INDEX_ASSET" >&2
    exit 1
  fi
done

step "Waiting for backend schema"
DEADLINE=$((SECONDS + 480))
SCHEMA_READY=0
while (( SECONDS < DEADLINE )); do
  if test_backend_schema; then
    SCHEMA_READY=1
    break
  fi
  printf '%s backend schema not updated yet; waiting...\n' "$(date +%H:%M:%S)"
  sleep 20
done

if [[ "$SCHEMA_READY" -ne 1 ]]; then
  printf 'Backend schema did not expose required fields within timeout: %s\n' "${REQUIRED_BACKEND_FIELDS[*]}" >&2
  exit 1
fi

step "Checking CloudRun status"
(cd "$BACKEND_DIR" && cloudbase cloudrun list -e "$ENV_ID" --serviceName "$CLOUD_RUN_SERVICE")

step "Deployment complete"
printf 'Frontend index asset: %s\n' "$LOCAL_INDEX_ASSET"
printf 'Backend schema contains: %s\n' "${REQUIRED_BACKEND_FIELDS[*]}"
printf 'Done.\n'
