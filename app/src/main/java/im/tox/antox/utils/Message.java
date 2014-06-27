package im.tox.antox.utils;

import java.sql.Timestamp;

/**
 * Created by ollie on 10/03/14.
 */
public class Message {
    public int message_id;
    public int id;
    public String key;
    public String message;
    public boolean has_been_received;
    public boolean has_been_read;
    public boolean successfully_sent;
    public Timestamp timestamp;
    public int size;
    public int type;

    public Message() {
        super();
    }

    public Message(int id, int message_id, String k, String m, boolean received, boolean read, boolean sent, Timestamp time, int size, int type) {
        super();
        this.id = id;
        this.message_id = message_id;
        this.key = k;
        this.message = m;
        this.has_been_received = received;
        this.has_been_read = read;
        this.successfully_sent = sent;
        this.size = size;
        this.type = type;
        this.timestamp = time;
    }

}
