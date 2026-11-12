#!/usr/bin/env bash
# Stops services started by start-backend.sh
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
for f in "$ROOT"/.pids/*.pid; do
  [[ -e "$f" ]] || continue
  pid=$(cat "$f"); name=$(basename "$f" .pid)
  if kill "$pid" 2>/dev/null; then echo "stopped $name ($pid)"; fi
  rm -f "$f"
done
