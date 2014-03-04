package im.tox.antox;

public class FriendRequests {
	public String requestKey;
	public String requestMessage;

	public FriendRequests() {
		super();
	}

	public FriendRequests(String requestKey, String requestMessage) {
		super();
		this.requestKey = requestKey;
		this.requestMessage = requestMessage;
	}

    public String key() {
        return requestKey;
    }
    public String message() {
        return requestMessage;
    }
}
