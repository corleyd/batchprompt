#!/bin/bash 
cd "$(dirname "$0")/.."
docker compose down
docker ps -a | grep 'batckprompt' | awk '{print $1}' | xargs -r docker rm -f
docker system prune -f
docker volume rm -f `docker volume ls | grep batchprompt | tail +2`