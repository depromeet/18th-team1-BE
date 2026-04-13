#!/bin/bash
set -e

IMAGE="ghcr.io/depromeet/18th-team1-be/api"
APP_DIR="/opt/firstpenguin/app"
CONFIG_DIR="/opt/firstpenguin/app/config"

echo "=== 설정 파일 업데이트 ==="
cd "$CONFIG_DIR"
git pull origin main

echo "=== 최신 이미지 pull ==="
docker pull "$IMAGE:latest"

echo "=== 앱 재시작 ==="
cd "$APP_DIR"
docker compose -f docker-compose-dev.yml up -d --force-recreate app

echo "=== 이전 이미지 정리 ==="
docker image prune -f

echo "=== 배포 완료 ==="
docker ps --filter "name=firstpenguin"
