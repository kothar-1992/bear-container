# Bear Container

An Android application with KeyAuth API integration and advanced obfuscation features.

## Package Structure

- **Package Name:** `org.bearmod.container`
- **Application ID:** `org.bearmod.container`
- **Version:** 3.6.0 (Build 100)

## Features

- 🔐 **KeyAuth API Integration** - Secure license validation
- 🛡️ **Advanced Obfuscation** - BlackObfuscator integration
- 🎮 **Game Overlay System** - Floating UI components
- 📱 **Multi-language Support** - Localized interface
- 🔧 **Native Library Support** - NDK integration

## Build Requirements

- **Android Studio:** Arctic Fox or newer
- **Gradle:** 8.12+
- **Android Gradle Plugin:** 8.10.1
- **Kotlin:** 2.1.0
- **NDK:** 27.1.12297006
- **Min SDK:** 21
- **Target SDK:** 34
- **Compile SDK:** 34

## KeyAuth Configuration

The app uses KeyAuth API for license validation:

- **Owner ID:** `yLoA9zcOEF`
- **App Name:** `org.bearmod.container`
- **API Version:** 1.3
- **Endpoint:** `https://keyauth.win/api/1.3/`

## Building the Project

### Debug Build
```bash
./gradlew assembleDebug
```

### Release Build
```bash
./gradlew assembleRelease
```

### Clean Build
```bash
./gradlew clean
```

## Project Structure

```
app/
├── src/main/java/org/bearmod/container/
│   ├── activity/          # Main activities
│   ├── floating/          # Overlay services
│   ├── Component/         # Utility components
│   ├── utils/            # Helper utilities
│   ├── libhelper/        # Native library helpers
│   └── KeyAuthAPIManager.java  # License validation
├── src/main/jni/         # Native C++ code
├── src/main/res/         # Resources
└── build.gradle          # Module build configuration
```

## Obfuscation

The project uses BlackObfuscator for advanced code protection:

- **Enabled:** Release builds only
- **Depth:** 2 levels
- **Protected Classes:**
  - `org.bearmod.container.activity`
  - `org.bearmod.container.Component`
  - `org.bearmod.container.floating.*`

## License

This project uses KeyAuth for license validation. A valid license key is required to use the application.

## Security Features

- ✅ Release builds are not debuggable
- ✅ Code obfuscation enabled
- ✅ Resource shrinking enabled
- ✅ ProGuard optimization
- ✅ Network security configuration
- ✅ Crash handling system

## Development Notes

- The project was migrated from `com.pubgall` to `org.bearmod.container`
- Native authentication was replaced with KeyAuth API
- All deprecated Android Gradle Plugin APIs have been updated
- Build configuration follows current Android development best practices

## Build Configuration Issues Resolved

### BlackObfuscator Plugin Compatibility
- **Issue**: Configuration cache incompatibility with Gradle 8.12
- **Solution**: Disabled configuration cache due to plugin limitations
- **Impact**: Slightly slower builds but maintains obfuscation functionality

### Java Module Access
- **Issue**: "module java.base does not 'opens java.lang.ref' to unnamed module"
- **Solution**: Added JVM arguments: `--add-opens=java.base/java.lang.ref=ALL-UNNAMED`
- **Result**: Resolved reflection access restrictions

### Task Configuration
- **Issue**: DexMergingTask serialization errors in configuration cache
- **Solution**: Added task-specific configuration cache exclusions
- **Code**: `notCompatibleWithConfigurationCache("BlackObfuscator modifies Dex merging dynamically")`

## Known Limitations

- Configuration cache is disabled due to BlackObfuscator plugin incompatibility
- Warning about deprecated `variant.getMappingFile()` API (from BlackObfuscator plugin)
- These limitations do not affect build functionality or app performance
