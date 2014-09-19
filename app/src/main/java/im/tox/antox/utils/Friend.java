package im.tox.antox.utils;

import im.tox.jtoxcore.ToxUserStatus;

public class Friend {
    public boolean isOnline;
	public String friendName;
	public String friendStatus;
    public String personalNote;
    public String friendKey;
    public String alias;

	public Friend() {
		super();
	}

	public Friend(boolean isOnline, String friendName, String friendStatus, String userNote, String key, String a) {
		super();
        this.isOnline = isOnline;
		this.friendName = friendName;
		this.friendStatus = friendStatus;
        this.personalNote = userNote;
        this.friendKey = key;
        this.alias = a;
	}

    public ToxUserStatus getFriendStatusAsToxUserStatus()
    {
        return UserStatus.getToxUserStatusFromString(friendStatus);
    }



	@Override
	public String toString() {
		return this.friendName;
	}
}
