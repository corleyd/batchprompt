#!/bin/bash 
cd "$(dirname "$0")"

DUMPFILE="/tmp/batchprompt_postgresql.$$.dump"

echo "Copying production data to local environment..."
ssh batchprompt.ai docker exec `ssh batchprompt.ai docker ps | grep batchprompt-postgres | awk '{print $1}'` pg_dump -U batchprompt -d batchprompt > $DUMPFILE

if [ $? -ne 0 ]; then
    echo "Failed to dump the production database."
    exit 1
fi

echo "Dump file created at $DUMPFILE"

echo "Stopping and removing existing Docker containers..."
docker compose down
docker volume rm -f `docker volume ls | grep batchprompt | awk '{print $2}'`


echo "Restarting Docker containers..."

docker compose up -d
if [ $? -ne 0 ]; then
    echo "Failed to start the Docker containers."
    exit 1
fi

echo "Waiting for PostgreSQL to start..."
sleep 60

echo "Restoring the database from the dump file..."

cat $DUMPFILE | docker exec -i `docker ps | grep batchprompt-postgres | awk '{print $1}'` psql -U batchprompt -d batchprompt

if [ $? -ne 0 ]; then
    echo "Failed to restore the database."
    exit 1
fi

echo "Copying MinIO data from production to local..."

./mc alias set prod-minio http://batchprompt.ai:9000 batchprompt batchprompt
./mc alias set localhost-minio http://localhost:9000 batchprompt batchprompt
./mc mb localhost-minio/batchprompt 
./mc mirror prod-minio/batchprompt localhost-minio/batchprompt

if [ $? -ne 0 ]; then
    echo "Failed to mirror the MinIO bucket."
    exit 1
fi

./mc ls localhost-minio/batchprompt
