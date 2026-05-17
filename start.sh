#!/bin/bash
set -e

echo "Building JARs..."
mvn clean package -DskipTests

echo "Starting Docker Compose..."
docker compose up --build -d

echo "Waiting for services to start..."
sleep 15

echo "Service status:"
docker compose ps
