#!/usr/bin/env bash
set -euo pipefail

wait_for_endpoint() {
  local endpoint="$1"
  local host="${endpoint%%:*}"
  local port="${endpoint##*:}"

  echo "Waiting for ${host}:${port} ..."
  until (echo >"/dev/tcp/${host}/${port}") >/dev/null 2>&1; do
    sleep 2
  done
}

if [[ -n "${WAIT_FOR_HOSTS:-}" ]]; then
  IFS=',' read -ra endpoints <<< "${WAIT_FOR_HOSTS}"
  for endpoint in "${endpoints[@]}"; do
    wait_for_endpoint "${endpoint}"
  done
fi

exec java ${JAVA_OPTS:-} -jar /app/app.jar
