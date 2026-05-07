#!/bin/bash
set -e

IMAGE="ghcr.io/depromeet/18th-team1-be/api"
APP_DIR="/opt/firstpenguin/app"
CONFIG_DIR="/opt/firstpenguin/app/config"

echo "=== 설정 파일 업데이트 ==="
cd "$CONFIG_DIR"
git pull origin main

echo "=== 최신 이미지 pull ==="
docker pull "$IMAGE:dev"

echo "=== 앱 재시작 ==="
cd "$APP_DIR"
docker compose -f docker-compose-dev.yml up -d --force-recreate app

echo "=== 헬스체크 ==="
for i in $(seq 1 6); do
  curl -sf http://localhost:8080/api/actuator/health && break
  echo "대기 중... ($i/6)"
  sleep 5
  [ "$i" -eq 6 ] && echo "헬스체크 실패: 앱이 30초 내에 뜨지 않았습니다." && exit 1
done

echo "=== 이전 이미지 정리 ==="
docker image prune -f

echo "=== 배포 완료 ==="
docker ps --filter "name=firstpenguin"
