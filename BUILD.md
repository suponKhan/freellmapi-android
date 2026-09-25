# FreeLLMAPI Android Port - Build Instructions

## Repository
https://github.com/suponKhan/freellmapi-android

## Quick Start

### Prerequisites
- Android Studio installed
- JDK 17 or higher
- Android SDK 34
- Android NDK 26+

### Step 1: Clone Repository
```bash
git clone https://github.com/suponKhan/freellmapi-android.git
cd freellmapi-android
```

### Step 2: Download nodejs-mobile Binaries
```bash
curl -L -o nodejs-mobile.zip "https://github.com/nodejs-mobile/nodejs-mobile/releases/download/v18.20.4/nodejs-mobile-v18.20.4-android.zip"
unzip -q nodejs-mobile.zip -d nodejs-mobile-tmp
mkdir -p android/app/libnode/bin/arm64-v8a
mkdir -p android/app/libnode/bin/armeabi-v7a
cp nodejs-mobile-tmp/bin/arm64-v8a/libnode.so android/app/libnode/bin/arm64-v8a/
cp nodejs-mobile-tmp/bin/armeabi-v7a/libnode.so android/app/libnode/bin/armeabi-v7a/
cp -r nodejs-mobile-tmp/include android/app/libnode/
rm -rf nodejs-mobile.zip nodejs-mobile-tmp
```

### Step 3: Configure Android SDK
```bash
echo "sdk.dir=/path/to/your/android-sdk" > local.properties
```

### Step 4: Build APK
```bash
# Debug build
./gradlew assembleDebug

# Release build
./gradlew assembleRelease
```

### Step 5: Install on Device
```bash
# Debug APK
adb install android/app/build/outputs/apk/debug/app-debug.apk

# Or release APK
adb install android/app/build/outputs/apk/release/app-release.apk
```

## Usage

### Start the Router
1. Open the app
2. Grant notification permission
3. Tap "Start Router"
4. The server will start on port 3001

### Use in Your Apps
```python
from openai import OpenAI

client = OpenAI(
    base_url="http://<PHONE_IP>:3001/v1",
    api_key="freellmapi"
)

response = client.chat.completions.create(
    model="auto",
    messages=[{"role": "user", "content": "Hello!"}]
)
```

### Access Dashboard
Open browser and navigate to:
```
http://<PHONE_IP>:3001/
```

## Available Providers

### Keyless (No API Key Required)
- **Pollinations.ai**: GPT-OSS 20B, GPT-OSS Protection
- **ApiAirforce**: Grok 4.1 Mini, Step 3.5 Flash

### With API Keys
Set environment variables in the app or via adb:
```bash
adb shell setprop persist.sys.freellmapi.groq_api_key YOUR_KEY
adb shell setprop persist.sys.freellmapi.cerebras_api_key YOUR_KEY
adb shell setprop persist.sys.freellmapi.gemini_api_key YOUR_KEY
```

## GitHub Actions

### Build Workflow
Triggered on push to `main`:
- Downloads dependencies
- Builds debug APK
- Uploads as artifact

### Release Workflow
Triggered on `v*` tags:
- Builds release APK
- Creates GitHub Release
- Uploads APK to release

### Sync Workflow
Runs every 6 hours:
- Syncs with upstream freellmapi
- Merges latest changes

## Troubleshooting

### CI Build Fails
The CI workflow may fail due to Android SDK configuration. To fix:
1. Use `android-actions/setup-android@v3` action
2. Or install Android SDK via apt-get
3. Ensure `ANDROID_HOME` is set correctly

### App Won't Start
1. Check that notification permission is granted
2. Verify foreground service is allowed in battery settings
3. Check that port 3001 is not blocked by firewall

### No Response from Server
1. Verify the server is running (check notification)
2. Check phone IP address (settings → about phone)
3. Ensure phone and client are on same network

## License
MIT