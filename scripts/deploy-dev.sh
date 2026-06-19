#!/bin/bash
set -Eeuo pipefail

IMAGE="ghcr.io/depromeet/18th-team1-be/api"
APP_DIR="/opt/firstpenguin/app"
CONFIG_DIR="/opt/firstpenguin/app/config"
HEALTHCHECK_MAX_ATTEMPTS=24
HEALTHCHECK_INTERVAL_SECONDS=5

echo "=== 설정 파일 업데이트 ==="
cd "$CONFIG_DIR"
git pull origin main

echo "=== 최신 이미지 pull ==="
docker pull "$IMAGE:dev"

echo "=== 앱 재시작 ==="
cd "$APP_DIR"
docker compose -f docker-compose-dev.yml up -d --force-recreate app

echo "=== 헬스체크 ==="
healthcheck_succeeded=false
for attempt in $(seq 1 "$HEALTHCHECK_MAX_ATTEMPTS"); do
  if curl -sSf --connect-timeout 2 --max-time 3 \
    http://localhost:8080/api/actuator/health >/dev/null; then
    echo "헬스체크 성공"
    healthcheck_succeeded=true
    break
  fi

  echo "대기 중... ($attempt/$HEALTHCHECK_MAX_ATTEMPTS)"
  if [ "$attempt" -lt "$HEALTHCHECK_MAX_ATTEMPTS" ]; then
    sleep "$HEALTHCHECK_INTERVAL_SECONDS"
  fi
done

if [ "$healthcheck_succeeded" != true ]; then
  echo "헬스체크 실패: 제한 시간 내에 앱이 준비되지 않았습니다."
  docker compose -f docker-compose-dev.yml logs --tail=100 app
  exit 1
fi

echo "=== 이전 이미지 정리 ==="
docker image prune -f

echo "=== 배포 완료 ==="
docker ps --filter "name=firstpenguin"
