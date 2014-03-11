![Antox](http://vexx.us/Images/AntoxFull.png "Antox Tox Android Client")
=====

Antox is an Android client for Tox. It aims to bring the full multimedia support Tox offers to your device, although it's still currently in heavy development. Antox is built on current design guidelines, and is both fast and fluid. Support for stickers, geo-location, and other various services offered by other competeting applications are currently being decided upon. Once ready, Antox will be available on Google Play and on F-Droid.

###Want to get Antox on Google Play?
1. Join the Google+ Alpha testing page <a href="https://plus.google.com/communities/112302171077600707137">here</a>.
2. Join the Google Play Alpha group <a href="https://play.google.com/apps/testing/im.tox.antox">here</a>
3. Download the app <a href="https://play.google.com/store/apps/details?id=im.tox.antox">here</a>

###Known to work on
* Samsung S3 running android 4.4 and cyanogenmod
* Galaxy Nexus running android 4.4
* Galaxy Note 3 running 4.3 (TouchWiz)
* Galaxy Note 10.1 running 4.3 (TouchWiz)
* Samsung Galaxy Tab 10.1 Wi-Fi running 4.0.4
* Moto G running 4.4.2
* Nexus 5 4.4.2 Stock
* HTC One running 4.4.2

###Getting Started With Antox
- Download https://developer.android.com/sdk/installing/studio.html
- In Android Studio, go to Help>Check For Updates. As of writing, the latest AS is 0.5.1
- In Android Studio again, go to Tools>Android>SDK Manager. Make sure you're using the latest SDK tools and SDK Build tools (22.6 and 19.0.3 respectively as of writing)
- To import the project, go to File>Import Project. Select the build.gradle file in the root of the antox folder
- Connect your phone in developer mode and click Run in Android Studio. It will install antox on to your phone and run it automatically.

###Submitting Pull Requests
- We ask that all pull requests are well documented so that we may better understand your commits. Requests that lack documentation may be rejected.
 
###Submitting Bug Reports
When submitting a bug report, please include the following:-
- Phone Model
- Android Verison
- Custom ROM?
- Error report (please give as much information as possible, not just a couple lines from logcat)

###What Is Currently Working
- Sending friend requests
- Receiving friend requests
- Profile, Settings and About Activities
- Friends list updates when a friend comes online / goes offline, or changes their other details at all.

###TODO
- Check updating the users settings gets sent across the network
- Check accepting friend requests then proceeds to update the users details in the friends list
- Friends default status should be offline when starting the app instead of last known status
- Start working on the chat
- Check friendList for received/send requests so to avoid adding the same friend twice
- Start adding fail-safes: Notify user when they're not connected to the internet, automatically try different DHT nodes if it can't connect to the first one, restart ToxService if it catches an exception, etc.
- Implement a blocking list so that user never sees friend requests from a blocked ID (very low priority)
- Add a 'paranoid' mode to the app so nothing at all is saved (very low priority)

###Screenshots of progress
<img src="http://vexx.us/Examples/Antox/device-2014-03-02-231541.png" width="230px" height="400px"/><img src="http://vexx.us/Examples/Antox/device-2014-03-02-231621.png" width="230px" height="400px"/><img src="http://markwinter.me/profile.png" width="230px" height="400px"/>
<img src="http://vexx.us/Examples/Antox/device-2014-03-09-203434.png" width="690px" height=431px"/>

*The Android robot is reproduced or modified from work created and shared by Google and used according to terms described in the Creative Commons 3.0 Attribution License.*
