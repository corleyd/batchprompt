#!/bin/bash

echo "Stopping notification service..."
if [ -f ./logs/notifications-service.pid ]; then
  kill -15 $(cat ./logs/notifications-service.pid) || true
  rm ./logs/notifications-service.pid
  echo "Notification service stopped"
else
  echo "No PID file found for notification service"
fi
