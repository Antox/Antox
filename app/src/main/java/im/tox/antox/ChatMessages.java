package im.tox.antox;

public class ChatMessages {
    public String message;
    public String time;
    public boolean ownMessage;
    public String friendName;

    public ChatMessages() {
        super();
    }

    public ChatMessages(String message, String time, boolean ownMessage) {
        super();
        this.time = time;
        this.message = message;
        this.ownMessage = ownMessage;
    }

    public ChatMessages(String message, String time, String friendName) {
        super();
        this.time = time;
        this.message = message;
        this.friendName = friendName;
    }

    public boolean IsMine() {
        return ownMessage;
    }
}
