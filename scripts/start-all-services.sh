#!/bin/bash

PROJECT_ROOT=$(dirname $(dirname $(readlink -f $0)))
echo "Starting all Spring Boot services from $PROJECT_ROOT"

# Create logs directory if it doesn't exist
mkdir -p "$PROJECT_ROOT/logs"

# Start the prompts service
echo "Starting prompts service..."
cd "$PROJECT_ROOT"
nohup ./gradlew :services:prompts:bootRun > "$PROJECT_ROOT/logs/prompts.log" 2>&1 &
PROMPTS_PID=$!
echo "Prompts service started with PID: $PROMPTS_PID"
echo $PROMPTS_PID > "$PROJECT_ROOT/logs/prompts.pid"

# Start the jobs service
echo "Starting jobs service..."
cd "$PROJECT_ROOT"
nohup ./gradlew :services:jobs:bootRun > "$PROJECT_ROOT/logs/jobs.log" 2>&1 &
JOBS_PID=$!
echo "Jobs service started with PID: $JOBS_PID"
echo $JOBS_PID > "$PROJECT_ROOT/logs/jobs.pid"

# Start the files service
echo "Starting files service..."
cd "$PROJECT_ROOT"
nohup ./gradlew :services:files:bootRun > "$PROJECT_ROOT/logs/files.log" 2>&1 &
FILES_PID=$!
echo "Files service started with PID: $FILES_PID"
echo $FILES_PID > "$PROJECT_ROOT/logs/files.pid"

echo "All services started successfully!"
echo "To stop all services, run stop-all-services.sh"

# List all running services
echo "Running services:"
echo "- Prompts service (PID: $PROMPTS_PID)"
echo "- Jobs service (PID: $JOBS_PID)"
echo "- Files service (PID: $FILES_PID)"