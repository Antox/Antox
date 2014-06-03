package im.tox.antox.adapters;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.TimeZone;

import im.tox.antox.R;
import im.tox.antox.utils.ChatMessages;
import im.tox.antox.utils.PrettyTimestamp;

public class ChatMessagesAdapter extends ArrayAdapter<ChatMessages> {
    Context context;
    int layoutResourceId;
    public ArrayList<ChatMessages> data = null;

    public ChatMessagesAdapter(Context context, int layoutResourceId,
                               ArrayList<ChatMessages> data) {
        super(context, layoutResourceId, data);
        this.context = context;
        this.layoutResourceId = layoutResourceId;
        this.data = data;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ChatMessages messages = this.getItem(position);
        View row = convertView;
        ChatMessagesHolder holder = null;

        if (row == null) {
            LayoutInflater inflater = LayoutInflater.from(context);
            row = inflater.inflate(layoutResourceId, parent, false);
            holder = new ChatMessagesHolder();
            holder.message = (TextView) row.findViewById(R.id.message_text);
            holder.alignment = (LinearLayout) row.findViewById(R.id.message_alignment_box);
            holder.layout = (LinearLayout) row.findViewById(R.id.message_text_layout);
            holder.row = (LinearLayout) row.findViewById(R.id.message_row_layout);
            holder.background = (LinearLayout) row.findViewById(R.id.message_text_background);
            holder.time = (TextView) row.findViewById(R.id.message_text_date);
            holder.sent = (ImageView) row.findViewById(R.id.chat_row_sent);
            holder.received = (ImageView) row.findViewById(R.id.chat_row_received);
            row.setTag(holder);
        } else {
            holder = (ChatMessagesHolder) row.getTag();
        }

        ChatMessages chatMessages = data.get(position);
        holder.message.setText(chatMessages.message);
        holder.time.setText(PrettyTimestamp.prettyTimestamp(chatMessages.time));

        if (messages.IsMine()) {
            holder.alignment.setGravity(Gravity.RIGHT);
            holder.time.setGravity(Gravity.RIGHT);
            holder.layout.setGravity(Gravity.RIGHT);
            holder.row.setGravity(Gravity.RIGHT);
            if (messages.sent) {
                holder.sent.setVisibility(View.VISIBLE);
                if (messages.received) {
                    holder.sent.setVisibility(View.GONE);
                    holder.received.setVisibility(View.VISIBLE);
                } else {
                    holder.received.setVisibility(View.GONE);
                }
            } else {
                holder.sent.setVisibility(View.GONE);
                holder.received.setVisibility(View.GONE);
            }
        } else {
            holder.alignment.setGravity(Gravity.LEFT);
            holder.time.setGravity(Gravity.LEFT);
            holder.layout.setGravity(Gravity.LEFT);
            holder.row.setGravity(Gravity.LEFT);
            holder.sent.setVisibility(View.GONE);
            holder.received.setVisibility(View.GONE);
        }
        return row;
    }

    static class ChatMessagesHolder {
        LinearLayout row;
        LinearLayout layout;
        LinearLayout background;
        LinearLayout alignment;
        TextView message;
        TextView time;
        ImageView sent;
        ImageView received;
    }

}
