package im.tox.antox.utils;

public class FriendInfo extends Friend {

    public int unreadCount;
    public String lastMessage;

	public FriendInfo() {
		super();
	}

	public FriendInfo(int icon, String friendName, String userStatus, String userNote, String key, String group, String lastMessage, int unreadCount) {
		super(icon, friendName, userStatus, userNote, key, group);
        this.lastMessage = lastMessage;
        this.unreadCount = unreadCount;
	}
}
