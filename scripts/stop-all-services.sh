#!/bin/bash

PROJECT_ROOT=$(dirname $(dirname $(readlink -f $0)))
echo "Stopping all Spring Boot services..."

# Define a function to stop services
stop_service() {
  local service_name=$1
  local pid_file="$PROJECT_ROOT/logs/${service_name}.pid"
  
  if [ -f "$pid_file" ]; then
    local pid=$(cat "$pid_file")
    if ps -p $pid > /dev/null; then
      echo "Stopping $service_name service (PID: $pid)..."
      kill $pid
      echo "$service_name service stopped"
    else
      echo "$service_name service is not running (PID: $pid not found)"
    fi
    rm "$pid_file"
  else
    echo "$service_name service PID file not found. Looking for running process..."
    # Try to find the process by looking at the Java processes
    local found_pid=$(ps -ef | grep "services/${service_name}" | grep -v grep | awk '{print $2}')
    if [ ! -z "$found_pid" ]; then
      echo "Found $service_name service running with PID: $found_pid"
      kill $found_pid
      echo "$service_name service stopped"
    else
      echo "No running $service_name service found"
    fi
  fi
}

# Stop all services
stop_service "prompts"
stop_service "jobs"
stop_service "files"

# Final check for any remaining Gradle processes related to our services
echo "Checking for any remaining Gradle processes..."
for process in $(ps -ef | grep "gradle" | grep "services/" | grep -v grep | awk '{print $2}'); do
  echo "Stopping extra Gradle process with PID: $process"
  kill $process
done

echo "All Spring Boot services stopped"