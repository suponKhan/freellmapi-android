# FreeLLMAPI Android Port

OpenAI-compatible LLM router for Android devices.

## Repository
https://github.com/suponKhan/freellmapi-android

## Features
- Runs FreeLLMAPI server on Android via Node.js
- OpenAI-compatible API endpoints
- Works on mobile data, WiFi, or local network
- 32-bit and 64-bit APK support
- Auto-start on boot
- Foreground service keeps router alive

## Endpoints
- Chat: `http://localhost:3001/v1/chat/completions`
- Models: `http://localhost:3001/v1/models`
- Dashboard: `http://localhost:3001/`

## Usage
```python
from openai import OpenAI

client = OpenAI(
    base_url="http://localhost:3001/v1",
    api_key="freellmapi"
)

response = client.chat.completions.create(
    model="auto",
    messages=[{"role": "user", "content": "Hello!"}]
)
```

## Build from Source

### Prerequisites
- Android Studio with JDK 17
- Android SDK 34
- Android NDK 26+
- CMake 3.22+

### Steps
1. Clone the repository
2. Download nodejs-mobile binaries:
   ```bash
   curl -L -o nodejs-mobile.zip "https://github.com/nodejs-mobile/nodejs-mobile/releases/download/v18.20.4/nodejs-mobile-v18.20.4-android.zip"
   unzip -q nodejs-mobile.zip -d nodejs-mobile-tmp
   mkdir -p android/app/libnode/bin/arm64-v8a
   mkdir -p android/app/libnode/bin/armeabi-v7a
   cp nodejs-mobile-tmp/bin/arm64-v8a/libnode.so android/app/libnode/bin/arm64-v8a/
   cp nodejs-mobile-tmp/bin/armeabi-v7a/libnode.so android/app/libnode/bin/armeabi-v7a/
   rm -rf nodejs-mobile.zip nodejs-mobile-tmp
   ```
3. Configure Android SDK:
   ```bash
   echo "sdk.dir=/path/to/android-sdk" > local.properties
   ```
4. Build:
   ```bash
   ./gradlew assembleDebug
   ```
5. Install:
   ```bash
   adb install android/app/build/outputs/apk/debug/app-debug.apk
   ```

## Available Providers

### Keyless (No API Key)
- Pollinations.ai: GPT-OSS 20B
- ApiAirforce: Grok 4.1 Mini, Step 3.5 Flash

### With API Keys
- Groq: Llama 3.3 70B, Qwen3 30B
- Cerebras: Llama 3.3 70B, Qwen3 235B
- Google Gemini: Gemini 2.5 Flash
- Ollama Cloud: GPT-OSS 120B

## License
MIT