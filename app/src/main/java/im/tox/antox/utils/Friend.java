package im.tox.antox.utils;

public class Friend {
	public int icon;
	public String friendName;
	public String friendStatus;
    public String personalNote;
    public String friendKey;
    public String friendGroup;

	public Friend() {
		super();
	}

	public Friend(int icon, String friendName, String userStatus, String userNote, String key, String group) {
		super();
		this.icon = icon;
		this.friendName = friendName;
		this.friendStatus = userStatus;
        this.personalNote = userNote;
        this.friendKey = key;
        this.friendGroup = group;
	}

	@Override
	public String toString() {
		return this.friendName;
	}
}
