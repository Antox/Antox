![Antox](http://vexx.us/Images/AntoxFull.png "Antox Tox Android Client")
=====

Antox is an Android client for Tox. It aims to bring the full multimedia support Tox offers to your device, although it's still currently in heavy development. Antox is built on current design guidelines, and is both fast and fluid. Support for stickers, geo-location, and other various services offered by other competeting applications are currently being decided upon. 

####Known to work on
* Samsung S3 running android 4.4 and cyanogenmod
* Galaxy Nexus running android 4.4
* Galaxy Note 3 running 4.3 (TouchWiz)
* Galaxy Note 10.1 running 4.3 (TouchWiz)

####Submitting Pull Requests
- We ask that all pull requests are well documented so that we may better understand your commits. Requests that lack documentation may be rejected.
 
####Submitting Bug Reports
When submitting a bug report, please include the following:-
- Phone Model
- Android Verison
- Custom ROM?
- Error report (please give as much information as possible, not just a couple lines from logcat)


###Completed

- Loading and saving of user settings
- Display a welcome screen when using the app for the first time
- Add Friend activity
- Design MainActivity window to hold friends list
- Extended Adapter class for the friends list (show status, online/offline)
- Design ChatActivity window for conversations 
- Extended Adapter class for displaying chat messages (only basic - needs a lot of design improvement)
- Searching through friends list
- Improve the ChatMessagesAdapter ([design goal](http://assets.hardwarezone.com/img/2013/11/messages.jpg)) 
- Store a list of up to date DHT servers (SettingsActivity)
- Add a QR scanner for adding friends and generating a QR of the users own public key
- Fix app crashing when you launch barcode scanner then back out of it
- When adding a friend, check the key is at least of proper length and only A-z 0-9


###In Progress

- Implement all the callbacks 
- Documenting the current code base

###TODO

- Start adding fail-safes: Notify user when they're not connected to the internet, automatically try different DHT nodes if it can't connect to the first one, restart ToxService if it catches an exception, etc.
- Check checksum to correctly validate the userkey id in the validateFriendKey method of addFriendActivity
- Implement a blocking list so that user never sees friend requests from a blocked ID (very low priority)

###Screenshots of progress
<img src="http://vexx.us/Examples/Antox/device-2014-03-02-231541.png" width="230px" height="400px"/><img src="http://vexx.us/Examples/Antox/device-2014-03-02-231621.png" width="230px" height="400px"/><img src="http://vexx.us/Examples/Antox/device-2014-03-02-231650.png" width="230px" height="400px"/>


####Milestones

- Connect to the tox network
- Working chat
