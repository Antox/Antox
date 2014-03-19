package im.tox.antox.utils;

public class FriendRequest {
	public String requestKey;
	public String requestMessage;

	public FriendRequest() {
		super();
	}

	public FriendRequest(String requestKey, String requestMessage) {
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
