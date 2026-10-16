#!/bin/bash

# MatchMind AI Deployment Script

set -e

echo "=== MatchMind AI Deployment ==="

# Configuration
BACKEND_PORT=${BACKEND_PORT:-8080}
FRONTEND_PORT=${FRONTEND_PORT:-5173}
AI_MODE=${AI_MODE:-DEMO}

echo "Backend Port: $BACKEND_PORT"
echo "Frontend Port: $FRONTEND_PORT"
echo "AI Mode: $AI_MODE"

# Check prerequisites
echo "Checking prerequisites..."
command -v java >/dev/null 2>&1 || { echo "Java is required but not installed. Aborting." >&2; exit 1; }
command -v node >/dev/null 2>&1 || { echo "Node.js is required but not installed. Aborting." >&2; exit 1; }
command -v mvn >/dev/null 2>&1 || { echo "Maven is required but not installed. Aborting." >&2; exit 1; }

# Build backend
echo "Building backend..."
cd backend
mvn clean package -DskipTests
cd ..

# Build frontend
echo "Building frontend..."
cd frontend
npm run build
cd ..

echo "Build complete!"
echo ""
echo "To run the application:"
echo "1. Backend: cd backend && java -jar target/matchmind-backend-1.0.0.jar"
echo "2. Frontend: Serve the frontend/dist directory with a web server"
echo ""
echo "Or use Docker:"
echo "docker-compose -f deploy/docker-compose.yml up -d"
