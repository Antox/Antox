package im.tox.antox;

import java.sql.Timestamp;

/**
 * Created by ollie on 10/03/14.
 */
public class Message {
    public int message_id;
    public String key;
    public String message;
    public boolean is_outgoing;
    public boolean has_been_received;
    public boolean has_been_read;
    public Timestamp timestamp;

    public Message() {
        super();
    }

    public Message(int message_id, String k, String m, boolean outgoing, boolean received, boolean read, Timestamp time) {
        super();
        this.message_id = message_id;
        this.key = k;
        this.message = m;
        this.is_outgoing = outgoing;
        this.has_been_received = received;
        this.has_been_read = read;
        this.timestamp = time;
    }

}
