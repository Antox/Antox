package im.tox.antox;

public class Friend {
	public int icon;
	public String friendName;
	public String friendStatus;
    public String personalNote="";

	public Friend() {
		super();
	}

	public Friend(int icon, String friendName, String friendStatus) {
		super();
		this.icon = icon;
		this.friendName = friendName;
		this.friendStatus = friendStatus;
	}

	@Override
	public String toString() {
		return this.friendName;
	}
}
