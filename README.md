# FreeLLMAPI Android

Android port of FreeLLMAPI - OpenAI-compatible LLM router running on Android devices.

## Features

- **Runs on Android**: Uses nodejs-mobile to run Node.js on Android devices
- **Foreground Service**: Keeps the router alive 24/7 even when app is in background
- **Auto-start on Boot**: Starts automatically when device boots
- **32-bit + 64-bit Support**: APKs for both ARM 32-bit and 64-bit devices
- **Keyless Providers**: Works out of the box with free providers (Pollinations.ai, ApiAirforce)
- **Optional API Keys**: Add keys for Groq, Cerebras, Gemini, etc.
- **OpenAI-Compatible**: Any app that works with OpenAI API will work with FreeLLMAPI
- **Dashboard**: Built-in web dashboard to monitor status and usage

## Endpoints

| Endpoint | Description |
|----------|-------------|
| `http://localhost:3001/v1/chat/completions` | Chat completions |
| `http://localhost:3001/v1/models` | List available models |
| `http://localhost:3001/` | Web dashboard |

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
print(response.choices[0].message.content)
```

## Installation

### From GitHub Releases
Download the APK from the [Releases](https://github.com/suponKhan/freellmapi-android/releases) page.

### Build from Source
```bash
# Download nodejs-mobile binaries
curl -L -o nodejs-mobile.zip "https://github.com/nodejs-mobile/nodejs-mobile/releases/download/v18.20.4/nodejs-mobile-v18.20.4-android.zip"
unzip -q nodejs-mobile.zip -d nodejs-mobile-tmp
mkdir -p android/app/libnode/bin/arm64-v8a android/app/libnode/bin/armeabi-v7a
cp nodejs-mobile-tmp/bin/arm64-v8a/libnode.so android/app/libnode/bin/arm64-v8a/
cp nodejs-mobile-tmp/bin/armeabi-v7a/libnode.so android/app/libnode/bin/armeabi-v7a/
rm -rf nodejs-mobile.zip nodejs-mobile-tmp

# Configure Android SDK
echo "sdk.dir=/path/to/android-sdk" > local.properties

# Build
./gradlew assembleDebug
```

## CI/CD

- **Build Workflow**: Runs on every push to main, builds debug APKs
- **Release Workflow**: Runs on tagged commits (v*), creates GitHub Release with APKs
- **Sync Workflow**: Runs every 6 hours to sync with upstream freellmapi

## Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `FREELLMAPI_PORT` | 3001 | Port to listen on |
| `FREELLMAPI_API_KEY` | freellmapi | API key for auth |
| `GROQ_API_KEY` | - | Groq API key (optional) |
| `CEREBRAS_API_KEY` | - | Cerebras API key (optional) |
| `GEMINI_API_KEY` | - | Google Gemini API key (optional) |

## Available Providers

### Keyless (No API Key Required)
- **Pollinations.ai**: GPT-OSS 20B, GPT-OSS Protection, GPT-OSS Default
- **ApiAirforce**: Grok 4.1 Mini, Step 3.5 Flash, Gemma3 270M

### With API Keys
- **Groq**: Llama 3.3 70B, Llama 3.1 8B, Qwen3 30B, GPT-OSS 120B
- **Cerebras**: Llama 3.3 70B, Llama 3.1 8B, Qwen3 235B
- **Google Gemini**: Gemini 2.5 Flash, Gemini 2.5 Pro
- **Ollama Cloud**: GPT-OSS 120B, Qwen3 32B

## License

MIT - Same as upstream FreeLLMAPI

## Links

- **Repository**: https://github.com/suponKhan/freellmapi-android
- **Upstream**: https://github.com/suponKhan/freellmapi
- **CI Builds**: https://github.com/suponKhan/freellmapi-android/actions