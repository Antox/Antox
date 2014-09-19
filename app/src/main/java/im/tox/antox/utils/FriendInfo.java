package im.tox.antox.utils;

import java.sql.Timestamp;
import java.util.Comparator;

public class FriendInfo extends Friend{

    public int unreadCount;
    public String lastMessage;
    public Timestamp lastMessageTimestamp;

	public FriendInfo() {
		super();
	}

	public FriendInfo(boolean isOnline, String friendName, String userStatus, String userNote, String key, String lastMessage, Timestamp lastMessageTimestamp, int unreadCount, String a) {
		super(isOnline, friendName, userStatus, userNote, key, a);
        this.lastMessage = lastMessage;
        this.lastMessageTimestamp = lastMessageTimestamp;
        this.unreadCount = unreadCount;
	}
}
