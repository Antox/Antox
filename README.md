<img src="http://i.imgur.com/fFhygVw.png" width="400" height="185" />

**Jenkins:** [![Build Status](https://build.tox.chat/job/antox_build_android_arm_release/badge/icon)](https://build.tox.chat/job/antox_build_android_arm_release/)
**CircleCI:** [![CircleCI](https://circleci.com/gh/Antox/Antox.svg?style=shield)](https://circleci.com/gh/Antox/Antox)

---

Antox is an Android 4+ client for Tox created by [Mark Winter](https://github.com/Astonex). It aims to bring the full multimedia support Tox offers to your device, although it's still currently in heavy development. Antox is currently available for alpha testing on Google Play and F-Droid.

### Screenshots

<img src="http://i.imgur.com/PvY7zCQ.jpg" width="230px" height="400px"/> <img src="http://i.imgur.com/Hmnjpv3.png" width="230px" height="400px"/> <img src="http://i.imgur.com/jApGiZQ.png" width="230px" height="400px"/>

### Current development

- Android 6 (Marshmallow) permission support
- Bugfixing A/V

### Getting Antox

Antox can be downloaded from [Google Play](https://play.google.com/store/apps/details?id=chat.tox.antox).

To install on F-Droid, add `https://pkg.tox.chat/fdroid/repo` and search for "Antox".

The APK can be downloaded [here](https://pkg.tox.chat/fdroid/repo/antox.apk)

### What Is Currently Working
- One to one messaging
- File transfers
- Avatars
- Partial A/V support

### Known Issues
- Currently crashing on x86_64 devices, due to the tox4j library not currently supporting this arch. To find out if your device is running x86_64 Android, look for your device on [GSMArena](http://www.gsmarena.com/) and check your device's chipset. The most common x86_64 Android chipset is the Intel Atom.

### Translating Antox
- You can localize the application via pull request or using [Transifex](https://www.transifex.com/antox/antox/).

### Compiling Antox From Source with Android Studio
- Download https://developer.android.com/sdk/installing/studio.html
- In Android Studio, go to Help>Check For Updates. As of writing, the latest version of AS is 2.2.3
- In Android Studio again, go to Tools>Android>SDK Manager. Make sure you're using the latest SDK tools and SDK Build tools.
- Clone the Antox repo
- To import the project, go to File>Import Project. Select the build.gradle file in the root of the Antox folder
- Download the latest tox4j binaries by running the download-dependencies script (`./download-dependencies.sh` on Linux/Mac or `download-dependencies.bat` on Windows)
- Install the Scala plugin in IntelliJ, restart, and wait for IntelliJ to set itself up
- Connect your phone in developer mode and click Run in Android Studio. It will install Antox on to your phone and run it automatically.

You may get an error when using the latest version of Android Studio:

```
Unsupported method: AndroidProject.getPluginGeneration().
The version of Gradle you connect to does not support that method.
To resolve the problem you can change/upgrade the target version of Gradle you connect to.
Alternatively, you can ignore this exception and read other information from the model.
```

Currently there is no fix for this, but there is a workaround by disabling instant run. This can be done by going to `File > Settings > Build, Execution, Deployment > Instant Run` and unchecking enable.


### Compiling Antox From Source using Gradle
- Download and install the SDK tools http://developer.android.com/sdk/index.html#Other
- Run `./download-dependencies.sh` (`download-dependencies.bat` on Windows)
- Run the gradle wrapper `./gradlew build -x lint --parallel` (`gradlew.bat build -x lint --parallel` on Windows). This will download gradle to your project files to ensure you have the correct version of gradle for building
- If errors occur during the first build, run the command again
- The apk will then be available in `app/build/outputs/apk/`
- To install the app via ADB, run `adb install <apk file>` 

### Remarks

*The Android robot is reproduced or modified from work created and shared by Google and used according to terms described in the Creative Commons 3.0 Attribution License.*

*This repository resembles but is legally distinct from [astonex/Antox](https://github.com/Astonex/Antox) and the Lollipop Guild.*


