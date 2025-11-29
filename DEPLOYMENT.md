# LinguaLink Deployment Guide

## Cost Comparison

### AWS Serverless (Pay-per-use)

| Service | Free Tier | After Free Tier |
|---------|-----------|-----------------|
| Lambda | 1M requests/month | $0.20 per 1M requests |
| API Gateway (HTTP) | 1M requests/month | $1.00 per 1M requests |
| API Gateway (WebSocket) | 1M messages/month | $1.00 per 1M messages |
| DynamoDB | 25GB storage, 25 WCU/RCU | $1.25 per 1M writes |

**Estimated Monthly Cost (1,000 users):** ~$5-15/month

### Translation Costs

| Option | Cost | Notes |
|--------|------|-------|
| DeepSeek API | ~$0.14/1M input tokens | Cheapest cloud option |
| Self-hosted Ollama | $20-50/month VM | Free after VM cost |
| AWS Bedrock | ~$0.75/1M tokens | More expensive |
| OpenAI | ~$0.50/1M tokens | Most expensive |

**Recommendation:**
- **< 10K messages/day:** Use DeepSeek API (~$1-5/month)
- **> 10K messages/day:** Self-host Ollama (~$20-50/month fixed)

---

## Option 1: Serverless (AWS Lambda) - Cheapest for Low Volume

### Prerequisites
- AWS Account
- AWS CLI installed and configured
- AWS SAM CLI installed (`pip install aws-sam-cli`)
- DeepSeek API key (free at https://platform.deepseek.com/)

### Deploy

```bash
cd server-serverless

# Install dependencies
npm install

# Build Lambda functions
npm run build

# Deploy to AWS (first time - guided)
sam deploy --guided

# Or deploy with config
sam deploy --config-env dev
```

### Configuration

1. **Get your API endpoints** from CloudFormation outputs:
   - `HttpApiUrl` - REST API for auth/conversations
   - `WebSocketUrl` - Real-time messaging

2. **Update mobile app** `.env`:
```env
EXPO_PUBLIC_API_URL=https://xxxxx.execute-api.us-east-1.amazonaws.com/prod
EXPO_PUBLIC_WS_URL=wss://xxxxx.execute-api.us-east-1.amazonaws.com/prod
```

---

## Option 2: Self-Hosted Ollama (Free Translation)

### Why Self-Host?
- **Zero API costs** for translation
- **Privacy** - your data never leaves your server
- **No rate limits**

### Setup on AWS EC2 / GCP Compute

1. **Launch a VM:**
   - **CPU Only:** t3.medium (2 vCPU, 4GB) - ~$30/month
   - **With GPU:** g4dn.xlarge (T4 GPU) - ~$150/month (5x faster)

2. **Install Ollama:**
```bash
# On your VM
curl -fsSL https://ollama.com/install.sh | sh

# Pull DeepSeek model
ollama pull deepseek-r1:8b

# Start server (runs on port 11434)
ollama serve
```

3. **Or use Docker:**
```bash
cd infrastructure/ollama
docker-compose up -d
./setup.sh
```

4. **Update serverless config:**
```bash
sam deploy --parameter-overrides \
  "TranslationMode=ollama" \
  "OllamaUrl=http://YOUR_VM_IP:11434"
```

### Ollama Model Options

| Model | Size | RAM Needed | Quality |
|-------|------|------------|---------|
| deepseek-r1:8b | 5GB | 8GB | Good |
| deepseek-r1:14b | 9GB | 16GB | Better |
| qwen2.5:7b | 4GB | 8GB | Good |
| llama3.2:3b | 2GB | 4GB | Basic |

---

## Option 3: Hybrid (Recommended for Production)

Use **DeepSeek API** for development and low volume, switch to **Ollama** when you scale:

```yaml
# template.yaml
Parameters:
  TranslationMode:
    Type: String
    Default: api  # Start with API
    AllowedValues:
      - api       # DeepSeek API (cheap)
      - ollama    # Self-hosted (free)
```

---

## Mobile App Deployment

### Build for App Stores

```bash
cd mobile

# Install EAS CLI
npm install -g eas-cli

# Login to Expo
eas login

# Build for iOS
eas build --platform ios --profile production

# Build for Android  
eas build --platform android --profile production

# Submit to stores
eas submit --platform ios
eas submit --platform android
```

### App Store Requirements

| Store | Account Cost | Review Time |
|-------|--------------|-------------|
| Apple App Store | $99/year | 1-3 days |
| Google Play | $25 one-time | 1-7 days |

---

## Complete Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     Mobile App (Expo)                        │
│                   iOS / Android / Web                        │
└─────────────────────────┬───────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│                    AWS API Gateway                           │
│         HTTP API (REST)    │    WebSocket API               │
└─────────────────┬──────────┴────────────┬───────────────────┘
                  │                       │
                  ▼                       ▼
┌─────────────────────────────────────────────────────────────┐
│                    AWS Lambda Functions                      │
│   auth.ts │ conversations.ts │ messages.ts │ websocket.ts   │
└─────────────────────────────┬───────────────────────────────┘
                              │
              ┌───────────────┼───────────────┐
              ▼               ▼               ▼
┌─────────────────┐  ┌─────────────┐  ┌─────────────────────┐
│   DynamoDB      │  │  DeepSeek   │  │  Ollama (Optional)  │
│ Users/Messages  │  │     API     │  │   Self-hosted AI    │
│   Connections   │  │   (Cloud)   │  │      (Free)         │
└─────────────────┘  └─────────────┘  └─────────────────────┘
```

---

## Estimated Monthly Costs

### Scenario: 1,000 Active Users

| Component | API Translation | Self-Hosted |
|-----------|-----------------|-------------|
| Lambda | $2 | $2 |
| API Gateway | $3 | $3 |
| DynamoDB | $5 | $5 |
| Translation | $5 (DeepSeek) | $30 (VM) |
| **Total** | **~$15/month** | **~$40/month** |

### Scenario: 10,000 Active Users

| Component | API Translation | Self-Hosted |
|-----------|-----------------|-------------|
| Lambda | $10 | $10 |
| API Gateway | $15 | $15 |
| DynamoDB | $25 | $25 |
| Translation | $50 (DeepSeek) | $50 (GPU VM) |
| **Total** | **~$100/month** | **~$100/month** |

**Break-even point:** ~5,000-10,000 messages/day

