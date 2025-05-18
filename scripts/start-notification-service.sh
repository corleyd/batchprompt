#!/bin/bash

echo "Starting notification service..."
cd "$(dirname "$0")/.."
./gradlew :notifications:notifications-service:bootRun --args='--spring.profiles.active=dev' &
echo $! > ./logs/notifications-service.pid
echo "Notification service started with PID $(cat ./logs/notifications-service.pid)"
