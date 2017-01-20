<img src="http://i.imgur.com/fFhygVw.png" width="400" height="185" />

**Travis:** [![Build Status](https://travis-ci.org/zoff99/Antox.png?branch=zoff99%2FAntox_v0.25)](https://travis-ci.org/zoff99/Antox)
**CircleCI:** [![CircleCI](https://circleci.com/gh/zoff99/Antox/tree/zoff99%2FAntox_v0.25.png?style=badge)](https://circleci.com/gh/zoff99/Antox)

=====

Antox is an Android 4+ client for Tox created by [Mark Winter](https://github.com/Astonex). It aims to bring the full multimedia support Tox offers to your device, although it's still currently in heavy development. Antox is currently available for alpha testing on Google Play and F-Droid.

###Screenshots

<img src="http://i.imgur.com/PvY7zCQ.jpg" width="230px" height="400px"/> <img src="http://i.imgur.com/Hmnjpv3.png" width="230px" height="400px"/> <img src="http://i.imgur.com/jApGiZQ.png" width="230px" height="400px"/>

###Current development

- Android 6 (Marshmallow) permission support
- Bugfixing A/V

###Getting Antox

Antox can be downloaded from [Google Play](https://play.google.com/store/apps/details?id=chat.tox.antox).

To install on F-Droid, add `https://pkg.tox.chat/fdroid/repo` and search for "Antox".

The APK can be downloaded from CircleCI, [here](https://circleci.com/api/v1/project/zoff99/Antox/latest/artifacts/0/$CIRCLE_ARTIFACTS/Antox.apk?filter=successful&branch=zoff99%2FAntox_v0.25)

###What Is Currently Working
- One to one messaging
- File transfers
- Avatars
- Partial A/V support

###Known Issues

###Translating Antox
- You can localize the application via pull request or using [Transifex](https://www.transifex.com/antox/antox/).

###Compiling Antox From Source with Android Studio
- Download https://developer.android.com/sdk/installing/studio.html
- In Android Studio, go to Help>Check For Updates. As of writing, the latest version of AS is 2.2.3
- In Android Studio again, go to Tools>Android>SDK Manager. Make sure you're using the latest SDK tools and SDK Build tools.
- Clone the Antox repo
- To import the project, go to File>Import Project. Select the build.gradle file in the root of the Antox folder
- Download the latest supplemental binaries by running the download-dependencies script (`./download-dependencies.sh` on Linux/Mac or `download-dependencies.bat` on Windows)
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

###Changing Tox Dependencies (and Sodium) for CI Build
- in circle.yml
```
cd libsodium-jni/ ; git checkout f21eb1c83da8be42efebda01b68474abef958285
cd jvm-toxcore-api ; git checkout c6d05b3d200de9ea53b19e0101d08abd16a751cd
cd jvm-toxcore-c/ ; git checkout 137be841050860b71d75c115aa0b046fec127ae5
```
- in https://github.com/TokTok/jvm-toxcore-c
 + build.sbt
 ```
 "org.toktok" %% "tox4j-api" % "0.1.2",
 "toktok" % "libtoxav" % "0.1.0",
 "toktok" % "libtoxcore" % "0.1.0",
 "jedisct1" % "libsodium" % "1.0.7",
 ```
 + buildscripts/dependencies.pl
```
"https://github.com/jedisct1", "libsodium", "1.0.11", @common,
"https://github.com/TokTok", "c-toxcore", "v0.1.2", @common,
```
- in https://github.com/TokTok/jvm-toxcore-api
 + no versions to configure here


###Compiling Antox From Source using Gradle
- Download and install the SDK tools http://developer.android.com/sdk/index.html#Other
- Run `./download-dependencies.sh` (`download-dependencies.bat` on Windows)
- Run the gradle wrapper `./gradlew build -x lint --parallel` (`gradlew.bat build -x lint --parallel` on Windows). This will download gradle to your project files to ensure you have the correct version of gradle for building
- If errors occur during the first build, run the command again
- The apk will then be available in `app/build/outputs/apk/`
- To install the app via ADB, run `adb install <apk file>` 

###Remarks

*The Android robot is reproduced or modified from work created and shared by Google and used according to terms described in the Creative Commons 3.0 Attribution License.*

*This repository resembles but is legally distinct from [astonex/Antox](https://github.com/Astonex/Antox) and the Lollipop Guild.*


