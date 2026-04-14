# Ngrok Alternatives - Quick Setup Guide

## ⚠️ Problem
Ngrok free tier data limit exceeded. Need immediate alternative.

---

## 🚀 Solution 1: Cloudflare Tunnel (RECOMMENDED)

### Why Cloudflare?
- ✅ **100% FREE** - No data limits
- ✅ **No account required** for quick tunnels
- ✅ **Fast and reliable**
- ✅ **No CORS issues**
- ✅ **Better than ngrok free tier**

### Quick Setup (2 minutes)

**Step 1: Run setup script**
```bash
.\setup-cloudflare-tunnel.bat
```

**Step 2: Start tunnel**
```bash
.\start-cloudflare-tunnel.bat
```

**Step 3: Copy the URL**
You'll see something like:
```
Your quick Tunnel has been created! Visit it at:
https://random-name-123.trycloudflare.com
```

**Step 4: Update frontend**
Replace ngrok URL with the Cloudflare URL in your frontend.

---

## 🚀 Solution 2: LocalTunnel (EASIEST)

### Why LocalTunnel?
- ✅ **FREE** - No limits
- ✅ **Simple npm package**
- ✅ **Custom subdomain support**
- ✅ **No account needed**

### Quick Setup (1 minute)

**Step 1: Install and start**
```bash
.\start-localtunnel.bat
```

**Step 2: Get URL**
You'll see:
```
your url is: https://actionsheet.loca.lt
```

**Step 3: Update frontend**
Use the URL shown in your frontend.

**Note**: First visit will show a warning page (like ngrok), but you can bypass it.

---

## 🚀 Solution 3: Serveo (NO INSTALLATION)

### Why Serveo?
- ✅ **FREE** - No limits
- ✅ **No installation** - Uses SSH
- ✅ **Works immediately**

### Quick Setup (30 seconds)

**Step 1: Run**
```bash
.\start-serveo.bat
```

**Step 2: Get URL**
You'll see:
```
Forwarding HTTP traffic from https://serveo.net
```

**Step 3: Update frontend**
Use the serveo.net URL.

---

## 📊 Comparison Table

| Feature | Cloudflare | LocalTunnel | Serveo | Ngrok Free |
|---------|-----------|-------------|--------|------------|
| **Cost** | FREE | FREE | FREE | FREE |
| **Data Limit** | ❌ None | ❌ None | ❌ None | ✅ 1GB/month |
| **Setup Time** | 2 min | 1 min | 30 sec | 1 min |
| **Installation** | Download | npm | None | Download |
| **CORS Issues** | ❌ No | ⚠️ Maybe | ⚠️ Maybe | ✅ Yes |
| **Reliability** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| **Custom Domain** | ❌ No | ✅ Yes | ❌ No | ❌ No |
| **Speed** | Fast | Fast | Medium | Fast |

---

## 🎯 Recommended Choice

### For Production/Demo: **Cloudflare Tunnel**
- Most reliable
- No data limits
- Professional URLs
- No CORS issues

### For Quick Testing: **LocalTunnel**
- Fastest setup
- Custom subdomain
- Good for development

### For Emergency: **Serveo**
- No installation
- Works immediately
- Good backup option

---

## 🔧 Step-by-Step: Cloudflare Tunnel (RECOMMENDED)

### 1. Download and Setup
```bash
.\setup-cloudflare-tunnel.bat
```

This will download `cloudflared.exe` (about 30MB).

### 2. Start Backend
Make sure your Spring Boot backend is running:
```bash
.\setup-and-run-server.bat
```

### 3. Start Tunnel
```bash
.\start-cloudflare-tunnel.bat
```

### 4. Copy the URL
You'll see output like:
```
2026-04-14T10:30:45Z INF +--------------------------------------------------------------------------------------------+
2026-04-14T10:30:45Z INF |  Your quick Tunnel has been created! Visit it at (it may take some time to be reachable): |
2026-04-14T10:30:45Z INF |  https://random-words-1234.trycloudflare.com                                               |
2026-04-14T10:30:45Z INF +--------------------------------------------------------------------------------------------+
```

### 5. Update Frontend
In your Vercel frontend, update the API base URL to:
```
https://random-words-1234.trycloudflare.com
```

### 6. Test
Open your frontend and verify it connects!

---

## 💡 Pro Tips

### Cloudflare Tunnel
- URL changes each time you restart (like ngrok free)
- For permanent URL, create a Cloudflare account (still free)
- No data transfer limits ever
- Better performance than ngrok free

### LocalTunnel
- Can request custom subdomain: `lt --port 8080 --subdomain actionsheet`
- First visit shows warning page (click "Continue")
- Add `ngrok-skip-browser-warning` header to bypass

### Serveo
- Sometimes has downtime (community-run)
- Good as backup option
- No configuration needed

---

## 🆘 Troubleshooting

### Cloudflare: "Failed to download"
- Download manually: https://github.com/cloudflare/cloudflared/releases/latest
- Get `cloudflared-windows-amd64.exe`
- Rename to `cloudflared.exe`
- Place in backend folder

### LocalTunnel: "npm not found"
- Install Node.js: https://nodejs.org/
- Restart PowerShell
- Run script again

### Serveo: "Connection refused"
- Check if SSH is enabled on Windows
- Try Cloudflare or LocalTunnel instead

---

## 🎉 Quick Start Commands

**Cloudflare (Recommended):**
```bash
.\setup-cloudflare-tunnel.bat
.\start-cloudflare-tunnel.bat
```

**LocalTunnel (Fastest):**
```bash
.\start-localtunnel.bat
```

**Serveo (No Install):**
```bash
.\start-serveo.bat
```

---

## 💰 Paid Options (If Needed)

If you need a permanent solution:

1. **Ngrok Pro** - $8/month
   - No data limits
   - Custom domains
   - No warning page

2. **Cloudflare Tunnel (with account)** - FREE
   - Permanent custom domain
   - Better control
   - Still completely free

3. **Deploy Backend** - FREE
   - Render.com (free tier)
   - Railway.app (free tier)
   - Fly.io (free tier)
   - No tunnel needed!

---

## 📝 Next Steps

1. Choose a tunnel solution (Cloudflare recommended)
2. Run the setup script
3. Start the tunnel
4. Copy the public URL
5. Update frontend with new URL
6. Test the connection

**Estimated time: 2-5 minutes**

---

## Need Help?

If any solution doesn't work, let me know and I'll help troubleshoot or suggest another option!
