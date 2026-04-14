# CORS Issue Fix Guide

## Problem
Frontend (Vercel) cannot connect to backend (ngrok) due to CORS errors:
```
Access to XMLHttpRequest has been blocked by CORS policy: 
No 'Access-Control-Allow-Origin' header is present
```

## Root Cause
Ngrok's free tier shows a warning page that blocks CORS preflight requests.

---

## Solution Applied

### 1. Updated CorsFilter.java
- Explicitly allow Vercel origin
- Added `ngrok-skip-browser-warning` header
- Improved OPTIONS request handling
- Added proper header exposure

### 2. Updated SecurityConfig.java
- Added `ngrok-skip-browser-warning` to allowed headers
- Exposed more headers in CORS configuration
- Ensured Vercel domain is whitelisted

### 3. Added Test Endpoint
- `/api/test/cors` - Test CORS GET
- `/api/test/headers` - View request headers

---

## How to Fix

### Step 1: Restart Backend
```bash
# Stop current server (Ctrl+C)
# Restart with new CORS configuration
.\setup-and-run-server.bat
```

### Step 2: Test CORS
Open browser console on your Vercel frontend and run:
```javascript
fetch('https://childcare-scarce-cauterize.ngrok-free.dev/api/test/cors', {
  headers: {
    'ngrok-skip-browser-warning': 'true'
  }
})
.then(r => r.json())
.then(d => console.log('CORS working:', d))
.catch(e => console.error('CORS failed:', e));
```

### Step 3: Update Frontend API Client
Make sure your frontend sends the bypass header with EVERY request:

```javascript
// In your API client (axios/fetch)
const headers = {
  'Content-Type': 'application/json',
  'ngrok-skip-browser-warning': 'true'  // ← ADD THIS
};

// Example with fetch
fetch(url, {
  method: 'GET',
  headers: headers
});

// Example with axios
axios.get(url, {
  headers: headers
});
```

---

## Frontend Code Fix

### If using Axios:
```javascript
// Create axios instance with default headers
import axios from 'axios';

const api = axios.create({
  baseURL: 'https://childcare-scarce-cauterize.ngrok-free.dev',
  headers: {
    'ngrok-skip-browser-warning': 'true'
  }
});

export default api;
```

### If using Fetch:
```javascript
// Wrapper function
const apiFetch = (url, options = {}) => {
  return fetch(url, {
    ...options,
    headers: {
      'ngrok-skip-browser-warning': 'true',
      ...options.headers
    }
  });
};

// Usage
apiFetch('/api/projects').then(r => r.json());
```

### If using SWR or React Query:
```javascript
const fetcher = (url) => fetch(url, {
  headers: { 'ngrok-skip-browser-warning': 'true' }
}).then(r => r.json());

// In component
const { data } = useSWR('/api/projects', fetcher);
```

---

## WebSocket Fix

For WebSocket connections, add the header to the connection:

```javascript
const ws = new WebSocket('wss://childcare-scarce-cauterize.ngrok-free.dev/ws-direct', {
  headers: {
    'ngrok-skip-browser-warning': 'true'
  }
});
```

Or if using SockJS:
```javascript
const socket = new SockJS('https://childcare-scarce-cauterize.ngrok-free.dev/ws', {
  transports: ['websocket'],
  headers: {
    'ngrok-skip-browser-warning': 'true'
  }
});
```

---

## Verification Steps

### 1. Check Backend Logs
After restart, you should see:
```
CORS test request from origin: https://frontend-rho-peach-62.vercel.app
```

### 2. Check Browser Network Tab
- Open DevTools → Network
- Look for OPTIONS requests (preflight)
- Should return 200 OK with CORS headers:
  ```
  Access-Control-Allow-Origin: https://frontend-rho-peach-62.vercel.app
  Access-Control-Allow-Credentials: true
  Access-Control-Allow-Methods: GET, POST, PUT, DELETE, PATCH, OPTIONS
  ```

### 3. Test API Endpoints
```bash
# Test from command line
curl -X OPTIONS https://childcare-scarce-cauterize.ngrok-free.dev/api/projects \
  -H "Origin: https://frontend-rho-peach-62.vercel.app" \
  -H "Access-Control-Request-Method: GET" \
  -v
```

Should return 200 OK with CORS headers.

---

## Alternative: Use Ngrok Paid Plan

If CORS issues persist, consider upgrading ngrok:
- **Ngrok Pro**: $8/month
- **Benefits**:
  - No warning page (eliminates CORS issues)
  - Custom domains
  - More concurrent tunnels
  - Better performance

---

## Permanent Solution: Deploy Backend

Instead of ngrok, deploy backend to:
- **Render.com** (free tier available)
- **Railway.app** (free tier available)
- **Fly.io** (free tier available)

This eliminates ngrok entirely and provides a stable URL.

---

## Quick Checklist

- [ ] Backend restarted with new CORS config
- [ ] Frontend sends `ngrok-skip-browser-warning` header
- [ ] Test endpoint returns success
- [ ] WebSocket connection works
- [ ] All API calls succeed

---

## Still Not Working?

If CORS still fails after these steps:

1. **Check ngrok URL** - Make sure it matches in frontend
2. **Clear browser cache** - Hard refresh (Ctrl+Shift+R)
3. **Check frontend code** - Ensure header is sent with ALL requests
4. **Try different browser** - Test in incognito mode
5. **Check ngrok status** - Visit ngrok URL directly in browser

Send me the browser console errors and I'll help debug further.
