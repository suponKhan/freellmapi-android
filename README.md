# FreeLLMAPI Android

[![Build Android APKs](https://github.com/suponKhan/freellmapi-android/actions/workflows/build.yml/badge.svg)](https://github.com/suponKhan/freellmapi-android/actions/workflows/build.yml)
[![Release APKs](https://github.com/suponKhan/freellmapi-android/actions/workflows/release.yml/badge.svg)](https://github.com/suponKhan/freellmapi-android/actions/workflows/release.yml)
[![Latest Release](https://img.shields.io/github/v/release/suponKhan/freellmapi-android)](https://github.com/suponKhan/freellmapi-android/releases/latest)

**FreeLLMAPI ported to Android.** Runs a full OpenAI-compatible LLM router on your mobile device as a 24/7 background foreground service.

---

## 📱 Download APKs

Directly download prebuilt APKs from **[GitHub Releases](https://github.com/suponKhan/freellmapi-android/releases/latest)**:

| Architecture | Device Type | APK Download |
| :--- | :--- | :--- |
| **ARM 64-bit (`arm64-v8a`)** | Modern Android devices | [Download `FreeLLMAPI-arm64-v8a.apk`](https://github.com/suponKhan/freellmapi-android/releases/latest) |
| **ARM 32-bit (`armeabi-v7a`)** | Older Android devices | [Download `FreeLLMAPI-armeabi-v7a.apk`](https://github.com/suponKhan/freellmapi-android/releases/latest) |
| **Universal (`universal`)** | All devices (all ABIs bundled) | [Download `FreeLLMAPI-universal.apk`](https://github.com/suponKhan/freellmapi-android/releases/latest) |
| **x86_64 (`x86_64`)** | Emulators / Chromebooks | [Download `FreeLLMAPI-x86_64.apk`](https://github.com/suponKhan/freellmapi-android/releases/latest) |

---

## ✨ Features

- **24/7 Keep-Alive Background Service**: Uses Android Foreground Service with Partial WakeLock and battery optimization whitelisting so the proxy router never dies in sleep mode.
- **Auto-Start on Boot**: Automatically restores the router service after device restart.
- **Network-Wide Accessibility**: Works locally on `http://127.0.0.1:3001` or across your LAN, WiFi, or Mobile Hotspot on `http://<phone-ip>:3001`.
- **Zero-Key Keyless Inference**: Works immediately with Pollinations.ai & ApiAirforce without any API keys.
- **Optional Custom Provider Keys**: Set keys for Groq, Cerebras, Google Gemini, Ollama Cloud.
- **In-App Dashboard & WebView**: Live dashboard and metrics directly on the phone or in your browser.
- **Full Automation via GitHub Actions**: Builds and releases are built 100% in GitHub CI with zero device overhead.
- **Automatic Fork Sync**: Checks upstream `freellmapi` on schedule and builds updated releases automatically.

---

## 🔌 Using the API from Other Apps

Any app on your phone, local network, or hotspot can use FreeLLMAPI as a backend:

### Endpoint Addresses
- **Base URL**: `http://localhost:3001/v1` (or `http://127.0.0.1:3001/v1`)
- **LAN URL**: `http://<your-phone-ip>:3001/v1`
- **Default API Key**: `freellmapi`

### Python Example
```python
from openai import OpenAI

client = OpenAI(
    base_url="http://localhost:3001/v1",
    api_key="freellmapi"
)

response = client.chat.completions.create(
    model="auto",
    messages=[{"role": "user", "content": "Explain quantum tunneling in one sentence."}]
)
print(response.choices[0].message.content)
```

### cURL
```bash
curl http://localhost:3001/v1/chat/completions \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer freellmapi" \
  -d '{
    "model": "auto",
    "messages": [{"role": "user", "content": "Hello!"}]
  }'
```

---

## 🛠️ GitHub Actions Automation

- **`build.yml`**: Compiles debug APKs on every push to `main` with native Node.js and C++ JNI bridge.
- **`release.yml`**: Triggers on `v*` tags (e.g. `v1.0.0`) and publishes standalone 32-bit & 64-bit APK assets to GitHub Releases.
- **`sync.yml`**: Periodically syncs upstream updates from `suponKhan/freellmapi` and merges updates into Android assets.

---

## 📄 License
MIT License. FreeLLMAPI Android Port.