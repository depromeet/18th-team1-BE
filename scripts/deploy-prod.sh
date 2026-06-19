#!/bin/bash
set -Eeuo pipefail

APP_DIR="/opt/firstpenguin/app"
CONFIG_DIR="$APP_DIR/config"
COMPOSE_FILE="$APP_DIR/docker-compose-prod.yml"

for required_file in "$APP_DIR/.env" "$COMPOSE_FILE" "$CONFIG_DIR/application-prod.yml"; do
  if [ ! -f "$required_file" ]; then
    echo "필수 파일이 없습니다: $required_file"
    exit 1
  fi
done

echo "=== 설정 파일 업데이트 ==="
git -C "$CONFIG_DIR" pull --ff-only origin main

cd "$APP_DIR"

echo "=== Compose 설정 검증 ==="
docker compose -f "$COMPOSE_FILE" config --quiet

echo "=== 최신 이미지 pull ==="
docker compose -f "$COMPOSE_FILE" pull app

echo "=== DB 실행 ==="
docker compose -f "$COMPOSE_FILE" up -d db

echo "=== 앱 재시작 ==="
docker compose -f "$COMPOSE_FILE" up -d --no-deps --force-recreate app

echo "=== 헬스체크 ==="
for attempt in $(seq 1 12); do
  if curl -sSf --connect-timeout 2 --max-time 3 \
    http://localhost:8080/api/actuator/health >/dev/null; then
    echo "헬스체크 성공"
    docker image prune -f
    docker ps --filter "name=firstpenguin-prod"
    exit 0
  fi

  echo "대기 중... ($attempt/12)"
  sleep 5
done

echo "헬스체크 실패: 앱이 60초 내에 준비되지 않았습니다."
docker compose -f "$COMPOSE_FILE" logs --tail=100 app
exit 1
