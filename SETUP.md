# 🚀 LinguaLink Setup Guide (Ubuntu VM on Proxmox)

This guide will walk you through setting up LinguaLink on an Ubuntu VM running on Proxmox. Perfect for a home lab setup!

---

## 📋 What You'll Need

- ✅ Proxmox server running
- ✅ Ability to create a new VM
- ✅ A phone with Expo Go installed (for testing the mobile app)
- ✅ About 30-60 minutes

---

## 🖥️ Part 1: Create the Ubuntu VM in Proxmox

### Step 1.1: Download Ubuntu Server ISO

1. **On your computer**, go to: https://ubuntu.com/download/server
2. **Download** "Ubuntu Server 22.04 LTS" (about 2GB)
3. **Upload** the ISO to your Proxmox server:
   - Open Proxmox web UI (usually `https://YOUR_PROXMOX_IP:8006`)
   - Click on your storage (e.g., `local`)
   - Click **ISO Images** → **Upload**
   - Select the Ubuntu ISO you downloaded

### Step 1.2: Create the VM

1. **Click** "Create VM" button (top right in Proxmox)

2. **General tab:**
   - Node: (your node)
   - VM ID: `100` (or any available number)
   - Name: `lingualink`
   - Click **Next**

3. **OS tab:**
   - ISO image: Select the Ubuntu ISO you uploaded
   - Type: `Linux`
   - Version: `6.x - 2.6 Kernel`
   - Click **Next**

4. **System tab:**
   - Leave defaults, click **Next**

5. **Disks tab:**
   - Disk size: `32` GB (minimum, 50GB recommended)
   - Click **Next**

6. **CPU tab:**
   - Sockets: `1` (leave as default)
   - Cores: `2` (minimum, 4 recommended)
   - Type: `Default (kvm64)` or `host` for better performance
   - Click **Next**

7. **Memory tab:**
   - Memory: `4096` MB (4GB minimum, 8GB recommended)
   - Click **Next**

8. **Network tab:**
   - Bridge: `vmbr0` (or your network bridge)
   - Click **Next**

9. **Confirm tab:**
   - Check "Start after created"
   - Click **Finish**

### Step 1.3: Install Ubuntu Server

1. **Click** on your new VM (`lingualink`)
2. **Click** "Console" to open the VM screen
3. **Follow** the Ubuntu installer:

   - Select language: **English**
   - Select **Ubuntu Server** (not minimized)
   - Network: Usually auto-configured (note the IP address!)
   - Proxy: Leave blank
   - Mirror: Leave default
   - Storage: Use entire disk (default)
   - Confirm destructive action: **Continue**
   - Your name: `admin`
   - Server name: `lingualink`
   - Username: `admin`
   - Password: Choose something you'll remember!
   - Skip Ubuntu Pro
   - **Install OpenSSH server: YES** ✅ (important!)
   - Featured snaps: Skip (press Tab, then Enter)

4. **Wait** for installation to complete (5-10 minutes)
5. **Click** "Reboot Now" when prompted
6. **Remove** the ISO (in Proxmox: Hardware → CD/DVD → Remove)

### Step 1.4: Find Your VM's IP Address

After the VM reboots and you log in:

```bash
ip addr show
```

Look for something like `192.168.1.XXX` or `10.0.0.XXX` - this is your VM's IP address.

**Write this down! You'll need it later.** 📝

Example: `192.168.1.100`

---

## 🔧 Part 2: Set Up the VM

Now let's install everything on your Ubuntu VM. You can either:
- Use the Proxmox console, OR
- SSH from your computer: `ssh admin@YOUR_VM_IP`

### Step 2.1: Update Ubuntu

```bash
# Update package lists
sudo apt update

# Upgrade all packages
sudo apt upgrade -y
```

### Step 2.2: Install Node.js

```bash
# Install Node.js 20 (LTS)
curl -fsSL https://deb.nodesource.com/setup_20.x | sudo -E bash -
sudo apt install -y nodejs

# Verify installation
node --version   # Should show v20.x.x
npm --version    # Should show 10.x.x
```

### Step 2.3: Install Docker

```bash
# Install Docker
curl -fsSL https://get.docker.com | sudo sh

# Add your user to docker group (so you don't need sudo)
sudo usermod -aG docker $USER

# Log out and back in for group changes to take effect
exit
```

**Log back in:**
```bash
ssh admin@YOUR_VM_IP
```

Verify Docker works:
```bash
docker --version   # Should show Docker version
docker ps          # Should show empty table (no error)
```

### Step 2.4: Install Git

```bash
sudo apt install -y git
```

---

## 🎯 Part 3: Get Your FREE DeepSeek API Key

DeepSeek is the AI that translates messages. It's **FREE** to use!

1. **On your computer**, go to: https://platform.deepseek.com
2. **Click** "Sign Up" (top right)
3. **Create an account** with your email
4. **Verify your email** (check your inbox)
5. **Log in** to DeepSeek
6. **Click** on "API Keys" in the left menu
7. **Click** "Create new API key"
8. **Copy the key** - it looks like: `sk-abc123xyz789...`
9. **Save it somewhere safe!**

> ⚠️ **Important:** Don't share your API key with anyone!

---

## 📦 Part 4: Download and Configure LinguaLink

### Step 4.1: Clone the Repository

```bash
# Go to home directory
cd ~

# Clone the code
git clone https://github.com/YOUR_USERNAME/messaging-app.git

# Go into the folder
cd messaging-app

# Install dependencies
npm install
```

### Step 4.2: Start the Database

We'll use Docker to run PostgreSQL and Redis. This step is where we **set the database username and password**.

```bash
# Create docker-compose file for databases
cat > docker-compose.yml << 'EOF'
version: '3.8'
services:
  postgres:
    image: postgres:15
    container_name: lingualink-db
    restart: unless-stopped
    environment:
      # ======================================
      # 🔐 POSTGRESQL CREDENTIALS - SET HERE!
      # ======================================
      # These values will be used in your .env file later
      # You can change these if you want different credentials
      POSTGRES_USER: postgres        # Database username
      POSTGRES_PASSWORD: password123 # Database password (change for production!)
      POSTGRES_DB: lingualink        # Database name
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data

  redis:
    image: redis:7
    container_name: lingualink-redis
    restart: unless-stopped
    ports:
      - "6379:6379"
    # Note: Redis has no password by default (fine for local/home use)

volumes:
  postgres_data:
EOF

# Start the databases
docker compose up -d

# Verify they're running
docker ps
```

You should see 2 containers running:
```
CONTAINER ID   IMAGE         STATUS         PORTS
abc123         postgres:15   Up 10 seconds  0.0.0.0:5432->5432
def456         redis:7       Up 10 seconds  0.0.0.0:6379->6379
```

### 📝 Understanding the Database Credentials

The credentials you just set in `docker-compose.yml` will be used in Step 4.3:

| Set in docker-compose.yml | Used in .env DATABASE_URL |
|---------------------------|---------------------------|
| `POSTGRES_USER: postgres` | `postgresql://`**postgres**`:password123@...` |
| `POSTGRES_PASSWORD: password123` | `postgresql://postgres:`**password123**`@...` |
| `POSTGRES_DB: lingualink` | `postgresql://...@localhost:5432/`**lingualink** |

**The full DATABASE_URL format:**
```
postgresql://USERNAME:PASSWORD@HOST:PORT/DATABASE
            └───┬───┘ └───┬───┘ └─┬─┘└┬─┘ └──┬───┘
                │         │       │   │      │
           postgres  password123  │  5432  lingualink
                               localhost
                          (same VM = localhost)
```

### 🔒 Want Different Credentials? (Optional)

If you want to use different credentials, change them in **BOTH** places:

**1. In docker-compose.yml (before running `docker compose up`):**
```yaml
environment:
  POSTGRES_USER: myuser           # Your custom username
  POSTGRES_PASSWORD: MySecureP@ss # Your custom password
  POSTGRES_DB: lingualink
```

**2. In .env (Step 4.3):**
```bash
DATABASE_URL="postgresql://myuser:MySecureP@ss@localhost:5432/lingualink"
```

**For this tutorial, we'll use the defaults:** `postgres` / `password123`

### Step 4.3: Create the Environment File

First, let me explain what each variable is and where the values come from:

| Variable | What It Is | Where It Comes From | Do You Change It? |
|----------|------------|---------------------|-------------------|
| `DATABASE_URL` | PostgreSQL connection | We set user/pass in docker-compose (Step 4.2) | ❌ No - leave as is |
| `REDIS_URL` | Redis connection | Redis has no password by default | ❌ No - leave as is |
| `DEEPSEEK_API_KEY` | AI translation key | You got this in Part 3 | ✅ **YES - paste your key!** |
| `JWT_SECRET` | Security key for logins | Any random string you make up | ⚠️ Optional (defaults work for testing) |
| `JWT_REFRESH_SECRET` | Security key for sessions | Any random string you make up | ⚠️ Optional (defaults work for testing) |
| `PORT` | Server port | Standard setting | ❌ No - leave as 3001 |

**Why `localhost`?** Because PostgreSQL and Redis are running on the **same VM** as your server (in Docker containers). They talk to each other locally.

```bash
# Go to server folder
cd ~/messaging-app/server

# Create .env file
cat > .env << 'EOF'
# ===========================================
# DATABASE (Don't change - matches docker-compose.yml)
# ===========================================
# Format: postgresql://USERNAME:PASSWORD@HOST:PORT/DATABASE
# - Username: postgres (set in docker-compose)
# - Password: password123 (set in docker-compose)
# - Host: localhost (database is on same VM)
# - Port: 5432 (PostgreSQL default)
# - Database: lingualink (set in docker-compose)
DATABASE_URL="postgresql://postgres:password123@localhost:5432/lingualink"

# ===========================================
# REDIS (Don't change - no password needed for local)
# ===========================================
REDIS_URL="redis://localhost:6379"

# ===========================================
# DEEPSEEK API KEY - ⚠️ YOU MUST CHANGE THIS! ⚠️
# ===========================================
# Paste the API key you got from Part 3
# It looks like: sk-abc123def456...
DEEPSEEK_API_KEY="sk-PASTE-YOUR-REAL-KEY-HERE"

# ===========================================
# JWT SECRETS (Can leave defaults for testing)
# ===========================================
# These are just random strings for security
# For production, generate random ones with: openssl rand -base64 32
# No special format - just make them long and random
JWT_SECRET="lingualink-jwt-secret-change-this-in-production-abc123"
JWT_REFRESH_SECRET="lingualink-refresh-secret-change-this-xyz789"

# ===========================================
# SERVER SETTINGS (Don't change)
# ===========================================
PORT=3001
CORS_ORIGIN="*"
NODE_ENV="production"
EOF
```

### ⚠️ YOU ONLY NEED TO CHANGE ONE THING: Your DeepSeek API Key!

```bash
# Edit the .env file
nano .env
```

1. **Find this line:**
   ```
   DEEPSEEK_API_KEY="sk-PASTE-YOUR-REAL-KEY-HERE"
   ```

2. **Replace** `sk-PASTE-YOUR-REAL-KEY-HERE` **with your actual API key** from Part 3
   
   Example - change FROM:
   ```
   DEEPSEEK_API_KEY="sk-PASTE-YOUR-REAL-KEY-HERE"
   ```
   
   TO (using your real key):
   ```
   DEEPSEEK_API_KEY="sk-a1b2c3d4e5f6g7h8i9j0..."
   ```

3. **Save the file:** Press `Ctrl + X`, then `Y`, then `Enter`

### 💡 Quick Answers to Your Questions:

**Q: Do I change `localhost` to my VM IP?**
> ❌ No! Keep it as `localhost`. The database runs on the same VM as the server, so they communicate locally.

**Q: Where do the PostgreSQL username/password come from?**
> From the `docker-compose.yml` we created in Step 4.2. We set:
> - Username: `postgres`
> - Password: `password123`
> - Database: `lingualink`

**Q: Where does the Redis URL come from?**
> Redis runs without a password by default on port 6379. Since it's on the same VM, we just use `localhost:6379`.

**Q: Do JWT secrets need a specific format?**
> No! They're just random strings. The longer and more random, the more secure. For testing, the defaults work fine. For production, generate random ones:
> ```bash
> openssl rand -base64 32
> ```

### Step 4.4: Set Up the Database Tables

```bash
# Generate Prisma client
npx prisma generate

# Create database tables
npx prisma migrate deploy
```

### Step 4.5: Add Demo Users

```bash
# Seed the database with test users
npx tsx prisma/seed.ts
```

This creates 4 demo users:
- 📧 `alice@example.com` / `password123` (English 🇺🇸)
- 📧 `carlos@example.com` / `password123` (Spanish 🇪🇸)
- 📧 `marie@example.com` / `password123` (French 🇫🇷)
- 📧 `yuki@example.com` / `password123` (Japanese 🇯🇵)

---

## 🚀 Part 5: Start the Server

### Step 5.1: Install PM2 (Keeps Server Running)

PM2 will keep your server running even after you close the terminal:

```bash
# Install PM2 globally
sudo npm install -g pm2

# Go to project root
cd ~/messaging-app

# Start the server with PM2
pm2 start npm --name "lingualink" -- run dev:server

# Check it's running
pm2 status
```

You should see:
```
┌─────┬──────────────┬─────────┬──────┬───────────┐
│ id  │ name         │ status  │ cpu  │ memory    │
├─────┼──────────────┼─────────┼──────┼───────────┤
│ 0   │ lingualink   │ online  │ 0%   │ 50mb      │
└─────┴──────────────┴─────────┴──────┴───────────┘
```

### Step 5.2: View Server Logs

```bash
# See the logs
pm2 logs lingualink
```

You should see:
```
🚀 LinguaLink Server is running!
📡 HTTP:      http://localhost:3001
🔌 WebSocket: ws://localhost:3001
```

Press `Ctrl + C` to exit logs (server keeps running).

### Step 5.3: Make PM2 Start on Boot

```bash
# Generate startup script
pm2 startup

# Copy and run the command it shows you (looks like: sudo env PATH=...)
# Then save the current processes
pm2 save
```

Now your server will automatically start when the VM reboots!

---

## 🔥 Part 6: Configure Firewall

Allow traffic to reach your server:

```bash
# Allow SSH (already open, but just in case)
sudo ufw allow 22

# Allow the API server
sudo ufw allow 3001

# Enable firewall
sudo ufw enable

# Check status
sudo ufw status
```

---

## 📱 Part 7: Set Up the Mobile App

Now we'll set up the mobile app **on your computer** (not the VM).

### Step 7.1: On Your Computer

```bash
# Clone the repo (if you haven't already)
git clone https://github.com/YOUR_USERNAME/messaging-app.git
cd messaging-app/mobile

# Install dependencies
npm install
```

### Step 7.2: Configure Mobile to Connect to Your VM

**Replace `YOUR_VM_IP` with your actual VM IP address (from Part 1.4):**

```bash
# Create .env file with your VM's IP
cat > .env << EOF
EXPO_PUBLIC_API_URL=http://YOUR_VM_IP:3001
EXPO_PUBLIC_WS_URL=ws://YOUR_VM_IP:3001
EOF
```

**Example** (if your VM IP is 192.168.1.100):
```bash
cat > .env << EOF
EXPO_PUBLIC_API_URL=http://192.168.1.100:3001
EXPO_PUBLIC_WS_URL=ws://192.168.1.100:3001
EOF
```

### Step 7.3: Install Expo Go on Your Phone

1. **iPhone:** Search "Expo Go" in App Store → Install
2. **Android:** Search "Expo Go" in Google Play → Install

### Step 7.4: Start the Mobile App

```bash
# Make sure you're in the mobile folder
cd mobile

# Start Expo
npx expo start
```

A QR code will appear in your terminal!

### Step 7.5: Open on Your Phone

1. Make sure your phone is on the **same network** as your Proxmox server
2. **iPhone:** Open Camera app → Scan QR code
3. **Android:** Open Expo Go app → Scan QR code

The app will load on your phone! 🎉

---

## 🧪 Part 8: Test the App!

1. **Log in** with: `alice@example.com` / `password123`
2. **Tap** the ✏️ icon to start a new chat
3. **Search** for "carlos" and tap to start a conversation
4. **Send a message** in English like "Hello, how are you?"
5. **On another phone/device**, log in as `carlos@example.com`
6. **Carlos sees** your message translated to Spanish! 🪄

---

## 📊 Part 9: Monitor Your Server

### Check Server Status
```bash
ssh admin@YOUR_VM_IP
pm2 status
```

### View Live Logs
```bash
pm2 logs lingualink
```

### Restart Server
```bash
pm2 restart lingualink
```

### Stop Server
```bash
pm2 stop lingualink
```

### Check Database
```bash
docker ps   # Should show postgres and redis running
```

---

## 🔄 Quick Reference Commands

Save these for later!

```bash
# ===== ON YOUR VM =====

# SSH into your VM
ssh admin@YOUR_VM_IP

# Start everything (after VM reboot, should auto-start)
cd ~/messaging-app
docker compose up -d
pm2 start lingualink

# View logs
pm2 logs lingualink

# Restart server
pm2 restart lingualink

# Check what's running
pm2 status
docker ps


# ===== ON YOUR COMPUTER =====

# Start mobile app
cd messaging-app/mobile
npx expo start
```

---

## ❓ Troubleshooting

### Can't connect to VM via SSH
```bash
# Make sure SSH is installed on the VM
sudo apt install openssh-server
sudo systemctl enable ssh
sudo systemctl start ssh
```

### "Connection refused" when accessing API
1. Check the server is running: `pm2 status`
2. Check firewall: `sudo ufw status` (port 3001 should be allowed)
3. Check you're using the right IP address

### Database not starting
```bash
# Check Docker is running
sudo systemctl status docker

# If not running:
sudo systemctl start docker

# Restart databases
cd ~/messaging-app
docker compose down
docker compose up -d
```

### Mobile app can't connect
1. Make sure phone is on same network as Proxmox server
2. Check VM IP address is correct in `mobile/.env`
3. Try accessing `http://YOUR_VM_IP:3001/health` in phone browser

### DeepSeek translation not working
1. Check your API key is correct in `server/.env`
2. View logs for errors: `pm2 logs lingualink`

### Server crashed
```bash
# Check logs for error
pm2 logs lingualink --lines 50

# Restart
pm2 restart lingualink
```

---

## 🎉 Congratulations!

You now have LinguaLink running on your Proxmox server!

**Your setup:**
```
┌─────────────────────────────────────────────────────────┐
│                    Proxmox Server                        │
│  ┌─────────────────────────────────────────────────┐    │
│  │              Ubuntu VM (lingualink)              │    │
│  │                                                   │    │
│  │  ┌──────────┐  ┌───────┐  ┌─────────────────┐   │    │
│  │  │ Postgres │  │ Redis │  │ LinguaLink API  │   │    │
│  │  │   :5432  │  │ :6379 │  │     :3001       │   │    │
│  │  └──────────┘  └───────┘  └─────────────────┘   │    │
│  │                                                   │    │
│  └─────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────┘
                          │
                          │ Network
                          ▼
              ┌─────────────────────┐
              │    Your Phone       │
              │   (Expo Go App)     │
              └─────────────────────┘
```

---

## 📱 Next Steps

Ready to put your app on the App Store? See `DEPLOYMENT.md` for:
- Deploying to AWS (serverless, very cheap)
- Using your own domain name
- Submitting to Apple App Store
- Submitting to Google Play Store
