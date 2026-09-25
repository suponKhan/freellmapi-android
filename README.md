# FreeLLMAPI Android port

Android app that runs FreeLLMAPI as a foreground service on mobile devices (32-bit + 64-bit support).

## Features
- Runs FreeLLMAPI router on Android via Node.js runtime (nodejs-mobile)
- Foreground service keeps router alive 24/7
- Auto-starts on boot
- Dashboard accessible via browser at `http://localhost:3001`
- Both ARM 32-bit (`armeabi-v7a`) and 64-bit (`arm64-v8a`) APKs
- Works on mobile data, WiFi, or local network

## Build Requirements
- Android Studio with JDK 17+
- Android SDK 35
- Android NDK 26+
- CMake 3.22+

## Build Steps

### 1. Download nodejs-mobile binaries
```bash
# Download from https://github.com/nodejs-mobile/nodejs-mobile/releases
curl -L -o nodejs-mobile.zip "https://github.com/nodejs-mobile/nodejs-mobile/releases/download/v18.20.4/nodejs-mobile-v18.20.4-android.zip"
unzip -q nodejs-mobile.zip -d nodejs-mobile-tmp
mkdir -p android/app/libnode/bin/arm64-v8a
mkdir -p android/app/libnode/bin/armeabi-v7a
mkdir -p android/app/libnode/bin/x86_64
cp nodejs-mobile-tmp/bin/arm64-v8a/libnode.so android/app/libnode/bin/arm64-v8a/
cp nodejs-mobile-tmp/bin/armeabi-v7a/libnode.so android/app/libnode/bin/armeabi-v7a/
cp nodejs-mobile-tmp/bin/x86_64/libnode.so android/app/libnode/bin/x86_64/
cp -r nodejs-mobile-tmp/include android/app/libnode/
rm -rf nodejs-mobile.zip nodejs-mobile-tmp
```

### 2. Download nodejs-mobile Java library
```bash
# The jar file should be in the download
cp nodejs-mobile-tmp/jars/android-arm64-v8a/nodejs-mobile.jar android/app/libs/
# Or use the standalone jar if available
```

### 3. Configure Android SDK
```bash
echo "sdk.dir=/path/to/android-sdk" > local.properties
```

### 4. Build APKs
```bash
# Debug builds
./gradlew assembleDebug

# Release builds (requires signing config)
./gradlew assembleRelease
```

## CI/CD
Automated builds run on GitHub Actions for pushes to `main` and tagged releases.

## Usage
1. Install APK on Android device
2. Open app and grant notification permission
3. Tap "Start Router"
4. Other apps can use: `base_url=http://localhost:3001/v1`, `api_key=freellmapi`

## GitHub Endpoints
- Chat: `http://localhost:3001/v1/chat/completions`
- Models: `http://localhost:3001/v1/models`
- Dashboard: `http://localhost:3001/`

## License
MIT - same as upstream FreeLLMAPI