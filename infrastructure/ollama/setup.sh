#!/bin/bash

# Setup script for self-hosted Ollama with DeepSeek
# This provides FREE AI translation after initial setup

set -e

echo "🚀 Setting up Ollama for LinguaLink..."

# Check if Docker is installed
if ! command -v docker &> /dev/null; then
    echo "❌ Docker is not installed. Please install Docker first."
    exit 1
fi

# Start Ollama container
echo "📦 Starting Ollama container..."
docker-compose up -d

# Wait for Ollama to be ready
echo "⏳ Waiting for Ollama to start..."
until curl -s http://localhost:11434/api/tags > /dev/null 2>&1; do
    sleep 2
done

echo "✅ Ollama is running!"

# Pull the DeepSeek model
echo "📥 Downloading DeepSeek model (this may take a while)..."
docker exec -it lingualink-ollama ollama pull deepseek-r1:8b

echo ""
echo "✅ Setup complete!"
echo ""
echo "Available models for translation:"
echo "  - deepseek-r1:8b (recommended, ~5GB, good quality)"
echo "  - deepseek-r1:14b (better quality, ~9GB)"
echo "  - qwen2.5:7b (alternative, ~4GB)"
echo ""
echo "To pull additional models:"
echo "  docker exec -it lingualink-ollama ollama pull MODEL_NAME"
echo ""
echo "Ollama API available at: http://localhost:11434"
echo ""
echo "Set these environment variables in your serverless deployment:"
echo "  TRANSLATION_MODE=ollama"
echo "  OLLAMA_URL=http://YOUR_SERVER_IP:11434"



