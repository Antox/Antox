package im.tox.antox;

public class FriendsList {
	public int icon;
	public String friendName;
	public String friendStatus;

	public FriendsList() {
		super();
	}

	public FriendsList(int icon, String friendName, String friendStatus) {
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
