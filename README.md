![Antox](http://vexx.us/Images/AntoxFull.png "Antox Tox Android Client")
=====

Antox is an Android client for Tox. It aims to bring the full multimedia support Tox offers to your device, although it's still currently in heavy development. Antox is built on current design guidelines, and is both fast and fluid. Support for stickers, geo-location, and other various services offered by other competeting applications are currently being decided upon. Once ready, Antox will be available on Google Play and on F-Droid.

[![tip for next commit](http://tip4commit.com/projects/654.svg)](http://tip4commit.com/projects/654)


###Want to install Antox on your Android phone?
1. Download the latest APK <a href="https://c1cf.https.cdn.softlayer.net/80C1CF/192.254.75.110:8080/job/Android-Antox/lastSuccessfulBuild/artifact/antox.apk">here</a>
2. Sideload the APK to your phone, or just download the file from step 1 directly to your phone
3. Ensure that you allow applications from unknown sources to be installed (Tick the option "Unkown Source", found in Settings>Security)
4. Install the APK by either using a file manager or ADB.

###Antox on F-Droid
To get Antox on F-Droid, add http://markwinter.me/fdroid/repo to your repo list. To do this, go to 'Manage Repos' in F-Droid.

###Want to get Antox on Google Play?
For those who used to test Antox on GPlay, we apologize that we will no longer be pushing alpha updates to the Play Store. Once Antox nears a public release, you will be able to find it in GPlay. Thanks to everyone who helped out!

###Known to work on
| Brand    | Device               | System                       |
|:--------:|:--------------------:|:----------------------------:|
| HTC      | Desire               | Android 4.2.2                |
| HTC      | One                  | Android 4.4.2                |
| LG       | Motion 4G            | Android 4.0.4                |
| LG       | Nexus 5              | Android 4.4.2                |
| Motorola | Moto G               | Android 4.4.2                |
| Samsung  | Galaxy Nexus         | Android 4.4                  |
| Samsung  | Galaxy Note 1        | Android 4.3 ParanoidAndroid  |
| Samsung  | Galaxy Note 3        | TouchWiz (Android 4.3)       |
| Samsung  | Galaxy Note 10.1     | TouchWiz (Android 4.3)       |
| Samsung  | Galaxy S3            | Android 4.4 CyanogenMod      |
| Samsung  | Galaxy Tab 10.1 WiFi | Android 4.0.4                |


###Getting Started With Antox
- Download https://developer.android.com/sdk/installing/studio.html
- In Android Studio, go to Help>Check For Updates. As of writing, the latest AS is 0.5.3
- In Android Studio again, go to Tools>Android>SDK Manager. Make sure you're using the latest SDK tools and SDK Build tools (22.6.2 and 19.0.3 respectively as of writing)
- To import the project, go to File>Import Project. Select the build.gradle file in the root of the antox folder
- Connect your phone in developer mode and click Run in Android Studio. It will install antox on to your phone and run it automatically.
- Download the current reference binaries from http://jenkins.tox.im/job/jToxcore_Android/45/artifact/artifacts/
- Copy the armeabi/, armeabi-v7a/, mips/ and x86/ folders to app/src/main/jniLibs/
- Copy jToxcore.jar to app/libs/jToxcore.jar
- These dependencies are updated from time to time, so you might need to check back if the binary link changed

###Submitting Pull Requests
- We ask that all pull requests are well documented so that we may better understand your commits. Requests that lack documentation may be rejected.
 
###Submitting Bug Reports
When submitting a bug report, please include the following:-
- Phone Model
- Android Verison
- Custom ROM?
- Error report (please give as much information as possible, not just a couple lines from logcat)

###What Is Currently Working
- Basic messaging - this does not include group chats

###TODO
Check the issues tab and filter by the tag 'To-Do' to see what needs to be done.No longer using Producteev as the invite system was a hassle.

###Screenshots of progress
<img src="http://vexx.us/Examples/Antox/Screenshot_2014-03-12-21-48-00.png" width="230px" height="400px"/><img src="http://vexx.us/Examples/Antox/Screenshot_2014-03-12-21-39-10.png" width="230px" height="400px"/><img src="http://vexx.us/Examples/Antox/device-2014-03-12-215856.png" width="230px" height="400px"/>


*The Android robot is reproduced or modified from work created and shared by Google and used according to terms described in the Creative Commons 3.0 Attribution License.*
