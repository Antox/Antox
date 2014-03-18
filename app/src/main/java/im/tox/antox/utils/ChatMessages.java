package im.tox.antox.utils;

public class ChatMessages {
    public int message_id;
    public String message;
    public String time;
    public boolean ownMessage;
    public String friendName;
    public boolean received;
    public boolean sent;

    public ChatMessages() {
        super();
    }

    public ChatMessages(int message_id,String message, String time, boolean ownMessage, boolean received, boolean sent) {
        super();
        this.message_id=message_id;
        this.time = time;
        this.message = message;
        this.ownMessage = ownMessage;
        this.received = received;
        this.sent = sent;
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
