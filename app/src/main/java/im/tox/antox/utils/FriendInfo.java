package im.tox.antox.utils;

import java.sql.Timestamp;

public class FriendInfo extends Friend {

    public int unreadCount;
    public String lastMessage;
    public Timestamp lastMessageTimestamp;

	public FriendInfo() {
		super();
	}

	public FriendInfo(int icon, String friendName, String userStatus, String userNote, String key, String lastMessage, Timestamp lastMessageTimestamp, int unreadCount, String a) {
		super(icon, friendName, userStatus, userNote, key, a);
        this.lastMessage = lastMessage;
        this.lastMessageTimestamp = lastMessageTimestamp;
        this.unreadCount = unreadCount;
	}
}
