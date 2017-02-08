Compile tox dependencies from source (including libsodiumjni)
=

**Travis:** [![Build Status](https://travis-ci.org/zoff99/Antox.png?branch=wiiam%2Fantox_0.25)](https://travis-ci.org/zoff99/Antox)
**CircleCI:** [![CircleCI](https://circleci.com/gh/zoff99/Antox/tree/wiiam%2Fantox_0.25.png?style=badge)](https://circleci.com/gh/zoff99/Antox)

=====

This commit APK can be downloaded from CircleCI, [here](https://circleci.com/api/v1/project/zoff99/Antox/latest/artifacts/0/$CIRCLE_ARTIFACTS/Antox.apk?filter=successful&branch=wiiam%2Fantox_0.25)

###Compiling Antox From Source using Gradle
- Download and install the SDK tools http://developer.android.com/sdk/index.html#Other
- Download the latest supplemental binaries by running the download-dependencies script `./fullsource/download-dependencies.sh`
- Run the gradle wrapper `./gradlew build -x lint --parallel` (`gradlew.bat build -x lint --parallel` on Windows). This will download gradle to your project files to ensure you have the correct version of gradle for building
- If errors occur during the first build, run the command again
- The apk will then be available in `app/build/outputs/apk/`
- To install the app via ADB, run `adb install <apk file>` 


###Compiling Antox From Source with Android Studio
- Download https://developer.android.com/sdk/installing/studio.html
- In Android Studio, go to Help>Check For Updates. As of writing, the latest version of AS is 2.2.3
- In Android Studio again, go to Tools>Android>SDK Manager. Make sure you're using the latest SDK tools and SDK Build tools.
- Clone the Antox repo
- To import the project, go to File>Import Project. Select the build.gradle file in the root of the Antox folder
- Download the latest supplemental binaries by running the download-dependencies script `./fullsource/download-dependencies.sh`
- Install the Scala plugin in IntelliJ, restart, and wait for IntelliJ to set itself up
- Connect your phone in developer mode and click Run in Android Studio. It will install Antox on to your phone and run it automatically.


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
 "toktok" % "libtoxav" % "0.1.0", # unused!!
 "toktok" % "libtoxcore" % "0.1.0", # unused!!
 "jedisct1" % "libsodium" % "1.0.7",
 ```
 + buildscripts/dependencies.pl
 ```
 "https://github.com/jedisct1", "libsodium", "1.0.11", @common,
 "https://github.com/TokTok", "c-toxcore", "v0.1.2", @common,
 ```
- in https://github.com/TokTok/jvm-toxcore-api
 + no versions to configure here



