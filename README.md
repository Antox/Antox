![Antox](http://vexx.us/Images/AntoxFull.png "Antox Tox Android Client")
=====

Antox is an Android 2.2+ client for Tox. It aims to bring the full multimedia support Tox offers to your device, although it's still currently in heavy development. 
Once ready, Antox will be available on Google Play and on F-Droid.

[![tip for next commit](http://tip4commit.com/projects/654.svg)](http://tip4commit.com/projects/654)

###Current development
Currently porting Antox to [tox4j](https://github.com/sonOfRa/tox4j)

###Antox On Google Play
For those who used to test Antox on Google Play, we apologize that we will no longer be pushing alpha updates to the Play Store. Once Antox is release ready it will be published on the App store. Thanks to everyone who helped to test!

###Directly Installing Antox
PLEASE NOTE this app is still alpha and will contain bugs and missing features (compared to other Tox clients)

1. Download the latest APK <a href="https://jenkins.libtoxcore.so/job/tox4j-antox/lastSuccessfulBuild/artifact/antox.apk">here</a>
2. Sideload the APK to your phone, or just download the file from step 1 directly to your phone
3. Ensure that you allow applications from unknown sources to be installed (Tick the option "Unkown Source", found in Settings>Security)
4. Install the APK by either using a file manager or ADB.

###Known To Work On
For a list of which devices currently run Antox, visit our <a href="https://wiki.tox.im/Antox#Known_to_work_on">Tox Wiki page.</a> Antox should support Android 2.2+. Please open an issue if you find yourself unable to so it can be fixed.

###Translating Antox
You can localize the application via github pull request or by requesting a new language or join an existing translation team on Transifex.
Transifex page - https://www.transifex.com/projects/p/antox/

###Compiling Antox From Source with IntelliJ IDE
~~- Download https://developer.android.com/sdk/installing/studio.html
- In Android Studio, go to Help>Check For Updates. As of writing, the latest version of AS is 0.8.1
- In Android Studio again, go to Tools>Android>SDK Manager. Make sure you're using the latest SDK tools and SDK Build tools (22.6.3 and 19.0.3 respectively as of writing)~~

Ask pippijn, sonOfRa or subliun for better instructions on IRC at #tox on Freenode.

###Compiling Scala Antox From Source via CLI
~~- Download android sdk http://developer.android.com/sdk/index.html and unpack
- Set the environmental variable ANDROID_HOME to point to it
- Add $ANDROID_HOME/tools and $ANDROID_HOME/platform-tools to your PATH environmental variable
- run the command `android` and use it to install SDK Platform for API 10, and the latest SDK tools and SDK build tools, and Android Support Library
- Clone the Antox repo
- Download the latest jToxcore binaries from https://jenkins.libtoxcore.so/job/jtoxcore_android_arm/
- Copy libjtoxcore.so to app/src/main/jniLibs/armeabi (you will need to create some of these folders)
- Copy jToxcore.jar to app/libs/jToxcore.jar
- These dependencies are updated from time to time, so you might need to check back if the binary link changed
- Connect your phone with USB in developer mode and run `./gradlew installDebug` from the root Antox directory. It will install Antox on to your phone, and you can now run it.
- Run `adb logcat` to display the logs of your USB connected phone, to read error messages and crash logs etc.~~

###What Is Currently Working
- Basic messaging - this does not include group chats
~~- File transfers~~

###Screenshots Of Progress
<img src="http://a.pomf.se/lltmgv.png" width="230px" height="400px"/><img src="http://a.pomf.se/dpopow.png" width="230px" height="400px"/><img src="http://a.pomf.se/npaodg.png" width="230px" height="400px"/>


*The Android robot is reproduced or modified from work created and shared by Google and used according to terms described in the Creative Commons 3.0 Attribution License.*

