#!/usr/bin/env bash
# Starts every Tiny Leaps backend service locally (dev). Logs: ./logs/<service>.log
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
BACKEND="$ROOT/backend"
export TL_LOG_DIR="$ROOT/logs"
mkdir -p "$TL_LOG_DIR" "$ROOT/.pids"

if [[ "${1:-}" != "--no-build" ]]; then
  (cd "$BACKEND" && mvn -q -DskipTests package)
fi

start() {
  local name="$1" jar="$2" port="$3"
  if lsof -iTCP:"$port" -sTCP:LISTEN -n -P >/dev/null 2>&1; then
    echo "  ! port $port already in use - skipping $name"; return
  fi
  nohup java -jar "$jar" > "$TL_LOG_DIR/$name.stdout" 2>&1 &
  echo $! > "$ROOT/.pids/$name.pid"
  echo "  started $name on :$port (pid $!)"
}

echo "Starting Tiny Leaps backend..."
start auth-service          "$BACKEND/auth-service/target/auth-service-1.0.0.jar"                         8083
start catalogue-service     "$BACKEND/catalogue-service/target/catalogue-service-1.0.0.jar"               8084
start puzzle-game-service   "$BACKEND/games/puzzle-game-service/target/puzzle-game-service-1.0.0.jar"     8091
start painting-game-service "$BACKEND/games/painting-game-service/target/painting-game-service-1.0.0.jar" 8092
start gateway               "$BACKEND/gateway/target/gateway-1.0.0.jar"                                   8080

echo -n "Waiting for gateway health"
for _ in $(seq 1 60); do
  if curl -fs http://localhost:8080/actuator/health >/dev/null 2>&1; then echo " - up: http://localhost:8080"; exit 0; fi
  echo -n "."; sleep 1
done
echo; echo "Gateway did not become healthy; check $TL_LOG_DIR"; exit 1
