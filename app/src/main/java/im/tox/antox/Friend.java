package im.tox.antox;

public class Friend {
	public int icon;
	public String friendName;
	public String friendStatus;
    public String personalNote;

	public Friend() {
		super();
	}

	public Friend(int icon, String friendName, String userStatus, String userNote) {
		super();
		this.icon = icon;
		this.friendName = friendName;
		this.friendStatus = userStatus;
        this.personalNote = userNote;
	}

	@Override
	public String toString() {
		return this.friendName;
	}
}
