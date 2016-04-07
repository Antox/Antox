Antox
=====


<img src="http://i.imgur.com/PvY7zCQ.jpg" width="230px" height="400px"/><img src="http://i.imgur.com/Hmnjpv3.png" width="230px" height="400px"/><img src="http://i.imgur.com/jApGiZQ.png" width="230px" height="400px"/>

Antox is an Android 4+ client for Tox created by [Mark Winter](https://github.com/Astonex). It aims to bring the full multimedia support Tox offers to your device, although it's still currently in heavy development. Antox is currently available for alpha testing on Google Play and F-Droid.

###Current development

- Android 6 (Marshmallow) permission support
- Bugfixing A/V

###Getting Antox

To get Antox on Google Play, join the [Google+ Community](https://plus.google.com/communities/103125800027884896310), and follow the instructions given.

To install on F-Droid, add `https://pkg.tox.chat/fdroid/repo` and search for "Antox".

###What Is Currently Working
- One to one messaging
- File transfers
- Avatars
- Partial A/V support

###Translating Antox
- You can localize the application via pull request or using [Transifex](https://www.transifex.com/antox/antox/).

###Compiling Antox From Source with Android Studio
- Download https://developer.android.com/sdk/installing/studio.html
- In Android Studio, go to Help>Check For Updates. As of writing, the latest version of AS is 1.4.1
- In Android Studio again, go to Tools>Android>SDK Manager. Make sure you're using the latest SDK tools and SDK Build tools.
- Clone the Antox repo
- To import the project, go to File>Import Project. Select the build.gradle file in the root of the Antox folder
- Download the latest tox4j binaries by running the download-dependencies script (`./download-dependencies.sh` on Linux/Mac or `download-dependencies.bat` on Windows)
- Install the Scala plugin in IntelliJ, restart, and wait for IntelliJ to set itself up
- Connect your phone in developer mode and click Run in Android Studio. It will install Antox on to your phone and run it automatically.

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


