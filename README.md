![Antox](http://vexx.us/Images/AntoxFull.png "Antox Tox Android Client")
=====

Antox is an Android 2.2+ client for Tox. It aims to bring the full multimedia support Tox offers to your device, although it's still currently in heavy development. 
Once ready, Antox will be available on Google Play and on F-Droid.

[![tip for next commit](http://tip4commit.com/projects/654.svg)](http://tip4commit.com/projects/654)

###Current development
Currently working on improving the file transfers, inline image displaying and fixing any bugs related to them. Other forms of media will
also be looked at for inlining. Development will also happen on the contacts list to allow searching and proper ordering

###Antox On F-Droid
To get Antox on F-Droid, add https://markwinter.me/fdroid/repo to your repo list. To do this, go to 'Repositories' in F-Droid and click the '+' symbol in the action bar. PLEASE NOTE this app is still alpha and
will contain bugs and missing features (compared to other Tox clients)

###Antox On Google Play
For those who used to test Antox on Google Play, we apologize that we will no longer be pushing alpha updates to the Play Store. Once Antox is release ready it will be published on the App store. Thanks to everyone who helped to test!

###Directly Installing Antox
PLEASE NOTE this app is still alpha and will contain bugs and missing features (compared to other Tox clients)

1. Download the latest APK <a href="https://jenkins.libtoxcore.so/job/Android-Antox/lastSuccessfulBuild/artifact/antox.apk">here</a>
2. Sideload the APK to your phone, or just download the file from step 1 directly to your phone
3. Ensure that you allow applications from unknown sources to be installed (Tick the option "Unkown Source", found in Settings>Security)
4. Install the APK by either using a file manager or ADB.

###Known To Work On
For a list of which devices currently run Antox, visit our <a href="https://wiki.tox.im/Antox#Known_to_work_on">Tox Wiki page.</a> Antox should support Android 2.2+. Please open an issue if you find yourself unable to so it can be fixed.

###Compiling Antox From Source
- Download https://developer.android.com/sdk/installing/studio.html
- In Android Studio, go to Help>Check For Updates. As of writing, the latest version of AS is 0.8.1
- In Android Studio again, go to Tools>Android>SDK Manager. Make sure you're using the latest SDK tools and SDK Build tools (22.6.3 and 19.0.3 respectively as of writing)
- To import the project, go to File>Import Project. Select the build.gradle file in the root of the Antox folder
- Download the latest jToxcore binaries from https://jenkins.libtoxcore.so/job/jToxcore_Android/lastSuccessfulBuild/artifact/*zip*/archive.zip
- Copy the armeabi/, armeabi-v7a/, mips/ and x86/ folders to app/src/main/jniLibs/
- Copy jToxcore.jar to app/libs/jToxcore.jar
- These dependencies are updated from time to time, so you might need to check back if the binary link changed
- Connect your phone in developer mode and click Run in Android Studio. It will install Antox on to your phone and run it automatically.

###What Is Currently Working
- Basic messaging - this does not include group chats

###Screenshots Of Progress
<img src="http://a.pomf.se/bboyqc.png" width="230px" height="400px"/><img src="https://pbs.twimg.com/media/Bq0stgVCQAA2Q6n.png:large" width="230px" height="400px"/><img src="http://a.pomf.se/qozvok.png" width="230px" height="400px"/>


*The Android robot is reproduced or modified from work created and shared by Google and used according to terms described in the Creative Commons 3.0 Attribution License.*

