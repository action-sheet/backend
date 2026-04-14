# Deploy Action Sheet Backend to Render.com

## 🎯 What You'll Get

- ✅ **Permanent URL**: `https://actionsheet-backend.onrender.com` (never changes!)
- ✅ **FREE Tier**: 750 hours/month (enough for 24/7 operation)
- ✅ **No CORS Issues**: Full control over CORS headers
- ✅ **Auto-Deploy**: Push to GitHub → Automatic deployment
- ✅ **No Credit Card**: Completely free, no payment required

---

## 📋 Prerequisites

1. GitHub account (to store your code)
2. Render.com account (free signup)

---

## 🚀 Step-by-Step Deployment

### Step 1: Push Code to GitHub (5 minutes)

#### 1.1 Create GitHub Repository

1. Go to https://github.com/new
2. Repository name: `actionsheet-backend`
3. Description: `Al-Ahlia Action Sheet Management System - Backend`
4. Visibility: **Private** (recommended) or Public
5. Click "Create repository"

#### 1.2 Initialize Git and Push

Open PowerShell in your backend folder (`E:\backend`) and run:

```powershell
# Initialize git (if not already done)
git init

# Add all files
git add .

# Commit
git commit -m "Initial commit - Action Sheet Backend"

# Add remote (replace YOUR_USERNAME with your GitHub username)
git remote add origin https://github.com/YOUR_USERNAME/actionsheet-backend.git

# Push to GitHub
git branch -M main
git push -u origin main
```

**Note**: If you get authentication errors, you may need to:
- Use GitHub Desktop app (easier)
- Or create a Personal Access Token: https://github.com/settings/tokens

---

### Step 2: Sign Up for Render.com (2 minutes)

1. Go to https://render.com/
2. Click "Get Started for Free"
3. Sign up with GitHub (recommended - easier integration)
4. Authorize Render to access your GitHub account

---

### Step 3: Create Web Service on Render (3 minutes)

#### 3.1 Create New Web Service

1. In Render Dashboard, click "New +" button
2. Select "Web Service"
3. Click "Connect a repository"
4. Find and select `actionsheet-backend`
5. Click "Connect"

#### 3.2 Configure Service

Fill in the following settings:

**Basic Settings:**
- **Name**: `actionsheet-backend`
- **Region**: Choose closest to Kuwait (e.g., Frankfurt or Singapore)
- **Branch**: `main`
- **Root Directory**: (leave empty)

**Build Settings:**
- **Runtime**: `Docker`
- **Build Command**: (auto-detected from Dockerfile)
- **Start Command**: (auto-detected from Dockerfile)

**Instance Type:**
- Select **Free** (750 hours/month)

**Environment Variables:**
Click "Add Environment Variable" and add these:

| Key | Value |
|-----|-------|
| `JAVA_OPTS` | `-Xmx512m -Xms256m` |
| `SPRING_PROFILES_ACTIVE` | `prod` |
| `EMAIL_USERNAME` | `gemis6292@gmail.com` |
| `EMAIL_PASSWORD` | `utqk kwym ikem ebbc` |
| `EMAIL_FROM` | `gemis6292@gmail.com` |

**Advanced Settings:**
- **Auto-Deploy**: Yes (enabled by default)
- **Health Check Path**: `/actuator/health`

#### 3.3 Deploy

1. Click "Create Web Service"
2. Render will start building and deploying
3. Wait 5-10 minutes for first deployment

---

### Step 4: Get Your Permanent URL

Once deployed, you'll see:

```
Your service is live at https://actionsheet-backend.onrender.com
```

**This URL never changes!** ✅

---

### Step 5: Update Frontend

Update your Vercel frontend to use the new URL:

```javascript
const API_BASE_URL = 'https://actionsheet-backend.onrender.com';
const WS_URL = 'wss://actionsheet-backend.onrender.com/ws-direct';
```

Deploy the frontend update to Vercel.

---

### Step 6: Test the Deployment

Open browser and test:

```
https://actionsheet-backend.onrender.com/api/test/cors
```

You should see:
```json
{
  "message": "CORS is working!",
  "origin": "...",
  "timestamp": 1234567890
}
```

---

## 🔧 Troubleshooting

### Build Failed

**Check logs** in Render dashboard:
- Click on your service
- Go to "Logs" tab
- Look for error messages

**Common issues:**
- Missing Dockerfile → Make sure `Dockerfile` is in root directory
- Maven build errors → Check `pom.xml` is correct
- Out of memory → Increase `JAVA_OPTS` memory settings

### Service Won't Start

**Check environment variables:**
- Make sure all required variables are set
- Check for typos in variable names

**Check health endpoint:**
- Go to `https://your-service.onrender.com/actuator/health`
- Should return `{"status":"UP"}`

### CORS Still Not Working

**Check CORS configuration:**
- Make sure `SecurityConfig.java` has Vercel domain whitelisted
- Check `CorsFilter.java` is active
- Look at response headers in browser DevTools

---

## 💡 Pro Tips

### 1. Custom Domain (Optional)

You can add your own domain:
1. Go to service settings
2. Click "Custom Domains"
3. Add your domain (e.g., `api.alahlia.com`)
4. Follow DNS configuration instructions

### 2. Persistent Storage

Render free tier has **ephemeral storage** (resets on restart).

For persistent data:
- Use Render PostgreSQL (free tier available)
- Or use external storage (AWS S3, Cloudinary)

**Current setup**: H2 database will reset on restart. To fix:
- Migrate to PostgreSQL (I can help with this)
- Or use Render Disk (paid feature)

### 3. Keep Service Awake

Free tier services sleep after 15 minutes of inactivity.

**Solution**: Use a cron job to ping your service:
- Use cron-job.org (free)
- Ping `https://actionsheet-backend.onrender.com/actuator/health` every 10 minutes

### 4. Monitor Logs

View real-time logs:
1. Go to Render dashboard
2. Click on your service
3. Go to "Logs" tab
4. See live application logs

### 5. Auto-Deploy on Git Push

Already enabled! Every time you push to GitHub:
```bash
git add .
git commit -m "Update feature"
git push
```

Render automatically rebuilds and deploys. ✅

---

## 📊 Free Tier Limits

| Resource | Limit | Notes |
|----------|-------|-------|
| **Hours** | 750/month | Enough for 24/7 |
| **Memory** | 512 MB | Sufficient for Spring Boot |
| **CPU** | Shared | Good performance |
| **Bandwidth** | Unlimited | No data transfer limits |
| **Build Time** | 15 min/build | Usually takes 5-8 minutes |
| **Storage** | Ephemeral | Resets on restart |

---

## 🎉 Benefits Over Tunnels

| Feature | Render | Ngrok Free | Cloudflare Tunnel |
|---------|--------|------------|-------------------|
| **Permanent URL** | ✅ Yes | ❌ No | ❌ No |
| **Data Limits** | ✅ None | ❌ 1GB/month | ✅ None |
| **CORS Support** | ✅ Full | ⚠️ Issues | ⚠️ Issues |
| **Uptime** | ✅ 24/7 | ✅ 24/7 | ✅ 24/7 |
| **Custom Domain** | ✅ Yes | ❌ No (free) | ✅ Yes |
| **SSL/HTTPS** | ✅ Auto | ✅ Yes | ✅ Yes |
| **Cost** | ✅ FREE | ✅ FREE | ✅ FREE |

---

## 🔄 Updating Your Deployment

### Method 1: Git Push (Recommended)

```bash
# Make changes to code
# Commit and push
git add .
git commit -m "Your update message"
git push

# Render auto-deploys!
```

### Method 2: Manual Deploy

1. Go to Render dashboard
2. Click on your service
3. Click "Manual Deploy" → "Deploy latest commit"

---

## 📝 Next Steps After Deployment

1. ✅ Update frontend with new URL
2. ✅ Test all API endpoints
3. ✅ Test WebSocket connection
4. ✅ Set up cron job to keep service awake
5. ✅ Consider migrating to PostgreSQL for persistent data

---

## 🆘 Need Help?

If you encounter any issues:

1. **Check Render logs** - Most errors are visible there
2. **Check GitHub** - Make sure code pushed successfully
3. **Check environment variables** - Verify all are set correctly
4. **Test locally first** - Make sure app runs on your machine

---

## 📞 Support

- Render Docs: https://render.com/docs
- Render Community: https://community.render.com/
- GitHub Issues: Create issue in your repo

---

**Estimated Total Time**: 10-15 minutes

**Result**: Permanent, reliable backend URL with full CORS support! 🚀
