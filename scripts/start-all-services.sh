#!/bin/bash

PROJECT_ROOT=$(dirname $(dirname $(readlink -f $0)))
echo "Starting all Spring Boot services from $PROJECT_ROOT"

# Create logs directory if it doesn't exist
mkdir -p "$PROJECT_ROOT/logs"

for service in prompts:prompts-api jobs:jobs-api files:files-api jobs:jobs-output-worker jobs:jobs-task-worker jobs:jobs-validation-worker users:users-api notifications:notifications-api waitlist:waitlist-api 
do
  service_module=$(echo $service | cut -d':' -f1)
  service_name=$(echo $service | cut -d':' -f2)
  
  # Check if the service's PID file exists and is a valid PID
  PID_FILE="$PROJECT_ROOT/logs/$service_name.pid"
  if [ -f "$PID_FILE" ]; then
    PID=$(cat "$PID_FILE")
    if ps -p $PID > /dev/null; then
      echo "$service_name service is already running with PID $PID. Skipping..."
      continue
    else
      echo "Removing stale PID file for $service_name service."
      rm "$PID_FILE"
    fi
  fi

  # Check if the service is already running by looking for java processes
  if pgrep -f "java.*$service_name.*\.jar" > /dev/null; then
    echo "$service_name service is already running. Skipping..."
    continue
  fi

  # Find the JAR file
  JAR_FILE="$PROJECT_ROOT/$service_module/$service_name/build/libs/$service_name-0.0.1-SNAPSHOT.jar"
  
  if [ ! -f "$JAR_FILE" ]; then
    echo "JAR file not found for $service_name at $JAR_FILE. Please build the project first."
    continue
  fi

  # Start the service
  echo "Starting $service_name service..."
  cd "$PROJECT_ROOT"
  nohup java -jar "$JAR_FILE" > "$PROJECT_ROOT/logs/$service_name.log" 2>&1 &
  SERVICE_PID=$!
  echo "$service_name service started with PID: $SERVICE_PID"
  echo $SERVICE_PID > "$PROJECT_ROOT/logs/$service_name.pid"
  echo "Logs for $service_name service can be found at $PROJECT_ROOT/logs/$service_name.log"
  echo "PID for $service_name service can be found at $PROJECT_ROOT/logs/$service_name.pid"
  echo "Service $service_name started successfully!"
done

