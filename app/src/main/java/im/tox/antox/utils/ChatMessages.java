package im.tox.antox.utils;

public class ChatMessages {
    // These should be private
    public int id;
    public int message_id;
    public String message;
    public String time;
    public boolean ownMessage;
    public String friendName;
    public boolean received;
    public boolean sent;
    public boolean isFile;
    public int size;
    public int type;

    public ChatMessages() {
        super();
    }

    public ChatMessages(int id, int message_id,String message, String time,boolean received, boolean sent, int size, int type) {
        super();
        this.id = id;
        this.message_id=message_id;
        this.time = time;
        this.message = message;
        this.received = received;
        this.sent = sent;
        this.size = size;
        this.type = type;
    }

    public ChatMessages(String message, String time, String friendName) {
        super();
        this.time = time;
        this.message = message;
        this.friendName = friendName;
    }

    public boolean IsMine() {
        if(type == 1)
            return true;
        else
            return false;
    }

    public int getType() {
        return this.type;
    }
}
