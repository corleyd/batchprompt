#!/bin/bash

PROJECT_ROOT=$(dirname $(dirname $(readlink -f $0)))
echo "Starting all Spring Boot services from $PROJECT_ROOT"

# Create logs directory if it doesn't exist
mkdir -p "$PROJECT_ROOT/logs"


for service in prompts:prompts-api jobs:jobs-api files:files-api jobs:jobs-output-worker jobs:jobs-task-worker jobs:jobs-validation-worker users:users-api notifications:notifications-service waitlist:waitlist-api 
  

  service_name=$(echo $service | cut -d':' -f2)

  # Check if the service is already running
  if pgrep -f "$service:bootRun" > /dev/null; then
    echo "$service service is already running. Skipping..."
    continue
  fi

  

    # Check if the service's PID file exists and is a valid PID
    PID_FILE="$PROJECT_ROOT/logs/$service_name.pid"
    if [ -f "$PID_FILE" ]; then
        PID=$(cat "$PID_FILE")
        if ps -p $PID > /dev/null; then
            echo "$service service is already running with PID $PID. Skipping..."
            continue
        else
            echo "Removing stale PID file for $service service."
            rm "$PID_FILE"
        fi
    fi

    # Start the service
    echo "Starting $service service..."
    cd "$PROJECT_ROOT"
    nohup ./gradlew "$service:bootRun" > "$PROJECT_ROOT/logs/$service_name.log" 2>&1 &
    SERVICE_PID=$!
    echo "$service service started with PID: $SERVICE_PID"
    echo $SERVICE_PID > "$PROJECT_ROOT/logs/$service_name.pid"
    echo "Service $service started with PID: $SERVICE_PID"
    echo "Logs for $service service can be found at $PROJECT_ROOT/logs/$service_name.log"
    echo "PID for $service service can be found at $PROJECT_ROOT/logs/$service_name.pid"
    echo "Service $service started successfully!"
done

